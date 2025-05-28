package com.example.frikidates

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.frikidates.firebase.FirebaseRepository
import com.example.frikidates.utils.InterestManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class PerfilActivity : AppCompatActivity() {

    private lateinit var interestManager: InterestManager
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var imageView3: ImageView
    private lateinit var imageView4: ImageView
    private lateinit var imageView5: ImageView
    private lateinit var imageView6: ImageView
    private lateinit var iv_camera: ImageView
    private lateinit var descEdit: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var genderSpinner2: Spinner
    private lateinit var notificationCheckBox: CheckBox
    private var user: User? = null

    private val REQUEST_CODE_GALLERY = 1001
    private val REQUEST_CODE_CAMERA = 1002
    private val REQUEST_CODE_STORAGE_PERMISSION = 1003

    private var selectedImageView: ImageView? = null

    private lateinit var userPreferences: UserPreferences

    private val userId: String
        get() = user?.userId ?: ""

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val logoutTextView = findViewById<TextView>(R.id.tv_logout)
        logoutTextView.setOnClickListener {
            showLogoutConfirmation()
        }

        val layout = findViewById<LinearLayout>(R.id.ll_interest_vertical)
        interestManager = InterestManager(this, layout)

        userPreferences = UserPreferences(this)

        user = userPreferences.getUser()

        imageView1 = findViewById(R.id.img1)
        imageView2 = findViewById(R.id.img2)
        imageView3 = findViewById(R.id.img3)
        imageView4 = findViewById(R.id.img4)
        imageView5 = findViewById(R.id.img5)
        imageView6 = findViewById(R.id.img6)
        iv_camera = findViewById(R.id.iv_camera)
        descEdit = findViewById(R.id.descEdit)
        genderSpinner = findViewById(R.id.genderSpinner)
        genderSpinner2 = findViewById(R.id.genderSpinner2)
        notificationCheckBox = findViewById(R.id.notificationCheckBox)

        // Setup navigation with interest saving
        BottomNavManager.setupNavigation(this) {
            // Save description
            updateDescriptionInDatabase(userId, descEdit.text.toString())
            // Save interests before navigating
            FirebaseRepository.saveUserInterests(
                interests = interestManager.selectedTags.toList(),
                onSuccess = {
                    Toast.makeText(this, getString(R.string.interests_saved), Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show()
                }
            )
        }

        loadUserImages()
        loadUserInfo()
        setupGenderSpinner(genderSpinner, "preferenciaGenero")
        setupGenderSpinner(genderSpinner2, "genero")
        loadNotificationSettings()
        interestManager = InterestManager(this, findViewById(R.id.ll_interest_vertical))
        interestManager.loadAndDisplayInterests()

        notificationCheckBox.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSettingsInDatabase(isChecked)
        }

        val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)

        for (imageView in imageViews) {
            imageView.setOnClickListener(object : View.OnClickListener {
                private var lastClickTime = 0L
                private val DOUBLE_CLICK_TIME_DELTA = 300L
                private var clickCount = 0

                override fun onClick(v: View?) {
                    val imageViewClicked = v as? ImageView ?: return
                    val clickTime = System.currentTimeMillis()

                    clickCount++

                    if (clickCount == 1) {
                        lastClickTime = clickTime
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(DOUBLE_CLICK_TIME_DELTA)
                            if (clickCount == 1) {
                                if (imageViewClicked.tag != null || imageViews.count { it.tag != null } < 6) {
                                    selectedImageView = imageViewClicked
                                    requestStoragePermission()
                                    openGallery()
                                } else {
                                    Toast.makeText(this@PerfilActivity, "Máximo 6 imágenes. Reemplaza una existente.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            clickCount = 0
                        }
                    } else if (clickCount == 2 && clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                        val drawable = imageViewClicked.drawable
                        if (drawable != null) {
                            showDeleteConfirmation(imageViewClicked)
                        }
                        clickCount = 0
                    }
                }
            })

            // Añadir OnLongClickListener para abrir imagen a pantalla completa
            imageView.setOnLongClickListener {
                if (imageView.tag != null) {
                    val imageUrl = imageView.tag.toString()
                    val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                        putExtra(FullScreenImageActivity.EXTRA_IMAGE_URL, imageUrl)
                    }
                    startActivity(intent)
                    true
                } else {
                    false
                }
            }
        }

        iv_camera.setOnClickListener {
            val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
            // Verificar el número de imágenes en Firebase
            FirebaseRepository.countUserImages(userId,
                onSuccess = { imageCount ->
                    if (imageCount >= 6) {
                        Toast.makeText(this, "Elimina una foto para poder abrir la cámara", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedImageView = imageViews.firstOrNull { it.tag == null }
                        if (selectedImageView != null) {
                            requestStoragePermission()
                            openCamera()
                        } else {
                            Toast.makeText(this, "Máximo 6 imágenes. Reemplaza una existente.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFailure = { e ->
                    Log.e("Firebase", "Error al contar imágenes: ${e.message}")
                    Toast.makeText(this, "Error al verificar imágenes", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CODE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    data?.data?.let { selectedImage ->
                        uploadImageToFirebaseStorage(selectedImage)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    data?.extras?.get("data")?.let { imageBitmap ->
                        val bitmap = imageBitmap as Bitmap
                        uploadImageToFirebaseStorage(bitmap)
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
        }
    }

    private fun uploadImageToFirebaseStorage(image: Any) {
        selectedImageView?.let { imageView ->
            Glide.with(this).load(R.drawable.loading_animation).into(imageView)

            if (imageView.tag != null) {
                val imageUrl = imageView.tag.toString()
                val decodedUrl = URLDecoder.decode(imageUrl, StandardCharsets.UTF_8.toString())
                val fileName = decodedUrl.substringAfterLast("/").substringBefore("?")
                if (fileName.isNotEmpty()) {
                    val storageRef = FirebaseStorage.getInstance().reference.child("$userId/$fileName")
                    storageRef.delete()
                        .addOnSuccessListener {
                            Log.d("DeleteImage", "Imagen anterior eliminada con éxito")
                            proceedWithUpload(image, imageView)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("DeleteImage", "Error al eliminar imagen anterior: ${exception.message}")
                            Toast.makeText(this, "Error al reemplazar la imagen", Toast.LENGTH_SHORT).show()
                            imageView.setImageResource(R.drawable.landscapeplaceholder)
                        }
                } else {
                    Log.e("DeleteImage", "fileName is empty, cannot delete")
                    proceedWithUpload(image, imageView)
                }
            } else {
                proceedWithUpload(image, imageView)
            }
        }
    }

    private fun proceedWithUpload(image: Any, imageView: ImageView) {
        when (image) {
            is Bitmap -> {
                FirebaseRepository.uploadBitmapToFirebaseStorage(
                    userId,
                    image,
                    onSuccess = { imageUrl ->
                        Log.d("Firebase", "Imagen subida con éxito: $imageUrl")
                        imageView.tag = imageUrl
                        loadUserImages()
                    },
                    onFailure = { exception ->
                        Log.e("Upload", "Error al subir la imagen: ${exception.message}")
                        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                        imageView.setImageResource(R.drawable.landscapeplaceholder)
                    }
                )
            }
            is Uri -> {
                FirebaseRepository.uploadUriToFirebaseStorage(
                    userId,
                    image,
                    onSuccess = { imageUrl ->
                        Log.d("Firebase", "Imagen subida con éxito: $imageUrl")
                        imageView.tag = imageUrl
                        loadUserImages()
                    },
                    onFailure = { exception ->
                        Log.e("Upload", "Error al subir la imagen: ${exception.message}")
                        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                        imageView.setImageResource(R.drawable.landscapeplaceholder)
                    }
                )
            }
        }
    }

    private fun loadUserImages() {
        val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
        imageViews.forEach {
            it.setImageResource(R.drawable.landscapeplaceholder)
            it.tag = null
        }

        FirebaseRepository.loadUserImages(
            userId,
            onSuccess = { imageUris ->
                for ((index, uri) in imageUris.withIndex()) {
                    if (index < imageViews.size) {
                        loadImageIntoImageView(uri, imageViews[index])
                    }
                }
                for (index in imageUris.size until imageViews.size) {
                    imageViews[index].setImageResource(R.drawable.landscapeplaceholder)
                    imageViews[index].tag = null
                }
                Log.d("Firebase", "Cargadas ${imageUris.size} imágenes")
            },
            onFailure = { e ->
                Log.e("Firebase", "Error al cargar imágenes: ${e.message}")
                Toast.makeText(this, "Error al cargar imágenes", Toast.LENGTH_SHORT).show()
                imageViews.forEach { it.setImageResource(R.drawable.landscapeplaceholder) }
            }
        )
    }

    fun loadImageIntoImageView(uri: Uri, imageView: ImageView) {
        Glide.with(imageView.context)
            .load(uri)
            .placeholder(R.drawable.loading_animation)
            .error(R.drawable.landscapeplaceholder)
            .into(imageView)

        imageView.tag = uri.toString()
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, getString(R.string.error_user_not_authenticated), Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseRepository.loadUserInfo(uid,
            onSuccess = { data ->
                val name = data["name"] as? String
                val birthdate = data["birthdate"] as? String
                val gender = data["genero"] as? String
                val description = data["bio"] as? String
                val surname = data["surname"] as? String
                val edad = birthdate?.let { calcularEdad(it) } ?: "Desconocida"

                val userInfoText = getString(R.string.user_info_display, name, surname, edad, gender)
                findViewById<TextView>(R.id.userInfo).text = userInfoText
                descEdit.setText(description)
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.error_getting_user_info, e.message)
                Log.e("Firestore", errorMessage, e)
            }
        )
    }

    private fun updateDescriptionInDatabase(userId: String, description: String) {
        FirebaseRepository.updateDescription(userId, description,
            onSuccess = {
                Log.d("Firestore", getString(R.string.description_updated))
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.error_updating_description, e.message)
                Log.e("Firestore", errorMessage, e)
            }
        )
    }

    private fun setupGenderSpinner(spinner: Spinner, fieldName: String) {
        FirebaseRepository.loadGenders(
            onSuccess = { genders ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                FirebaseRepository.getDefaultGender(
                    userId,
                    fieldName,
                    onSuccess = { savedGender ->
                        savedGender?.let {
                            val position = genders.indexOf(it)
                            if (position >= 0) {
                                spinner.setSelection(position)
                            }
                        }
                    },
                    onFailure = { e ->
                        val errorMessage = getString(R.string.error_getting_field, fieldName, e.message)
                        Log.e("PerfilActivity", errorMessage, e)
                    }
                )

                var isSpinnerInitial = true
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        if (isSpinnerInitial) {
                            isSpinnerInitial = false
                            return
                        }
                        val selectedGender = genders[position]
                        FirebaseRepository.updateGender(
                            userId,
                            fieldName,
                            selectedGender,
                            onSuccess = {
                                val updateMessage = getString(R.string.field_updated, fieldName, selectedGender)
                                Log.d("PerfilActivity", updateMessage)
                            },
                            onFailure = { e ->
                                val errorMessage = getString(R.string.error_updating_field, fieldName, e.message)
                                Log.e("PerfilActivity", errorMessage, e)
                            }
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                    }
                }
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.error_loading_genders, e.message)
                Log.e("PerfilActivity", errorMessage, e)
            }
        )
    }

    private fun loadNotificationSettings() {
        FirebaseRepository.loadNotificationSettings(
            userId,
            onSuccess = { notificationsEnabled ->
                notificationCheckBox.isChecked = notificationsEnabled
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.error_loading_notifications, e.message)
                Log.e("PerfilActivity", errorMessage, e)
            }
        )
    }

    @SuppressLint("StringFormatMatches")
    private fun updateNotificationSettingsInDatabase(enabled: Boolean) {
        FirebaseRepository.updateNotificationSettings(
            userId,
            enabled,
            onSuccess = {
                val updateMessage = getString(R.string.notifications_updated, enabled)
                Log.d("PerfilActivity", updateMessage)
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.error_updating_notifications, e.message)
                Log.e("Firestore", errorMessage, e)
            }
        )
    }

    fun calcularEdad(fechaNacimiento: String): Int {
        val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
        val fechaNacimiento = LocalDate.parse(fechaNacimiento, formatter)
        val hoy = LocalDate.now()
        return Period.between(fechaNacimiento, hoy).years
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performLogout() {
        userPreferences.clear()
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteImage(imageView: ImageView) {
        imageView.tag?.let { tag ->
            val imageUrl = tag.toString()
            Log.d("DeleteImage", "imageUrl: $imageUrl")

            val decodedUrl = URLDecoder.decode(imageUrl, StandardCharsets.UTF_8.toString())
            Log.d("DeleteImage", "decodedUrl: $decodedUrl")

            val fileName = decodedUrl.substringAfterLast("/").substringBefore("?")
            Log.d("DeleteImage", "fileName: $fileName")

            if (fileName.isEmpty()) {
                Log.e("DeleteImage", "fileName is empty, cannot proceed")
                return
            }

            Glide.with(this).load(R.drawable.loading_animation).into(imageView)

            val storageRef = FirebaseStorage.getInstance().reference.child("$userId/$fileName")
            storageRef.delete()
                .addOnSuccessListener {
                    Log.d("DeleteImage", "Imagen eliminada con éxito de Firebase Storage")
                    Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
                    loadUserImages()
                }
                .addOnFailureListener { exception ->
                    Log.e("DeleteImage", "Error al eliminar la imagen: ${exception.message}", exception)
                    Toast.makeText(this, "Error al eliminar la imagen", Toast.LENGTH_SHORT).show()
                    imageView.setImageResource(R.drawable.landscapeplaceholder)
                }
        } ?: run {
            Log.e("DeleteImage", "ImageView tag is null")
            imageView.setImageResource(R.drawable.landscapeplaceholder)
        }
    }

    private fun showDeleteConfirmation(imageView: ImageView) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar imagen")
            .setMessage("¿Quieres eliminar esta imagen?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteImage(imageView)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
