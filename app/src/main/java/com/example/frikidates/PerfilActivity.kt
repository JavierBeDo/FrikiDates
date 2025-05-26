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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.frikidates.firebase.FirebaseRepository
import com.example.frikidates.utils.InterestManager
import com.google.firebase.auth.FirebaseAuth
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

    private var selectedImageView: ImageView? = null // Variable para rastrear qué ImageView se seleccionó

    private lateinit var userPreferences: UserPreferences


    private val userId: String
        get() = user?.userId ?: ""

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)


        val layout = findViewById<LinearLayout>(R.id.ll_interest_vertical)
        interestManager = InterestManager(this, layout)

        userPreferences = UserPreferences(this)

        //esto carga el nav
        BottomNavManager.setupNavigation(this) {
            updateDescriptionInDatabase(userId, descEdit.text.toString())
        }


        // Ahora puedes usar userPreferences para obtener el usuario
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

        // Acción para abrir la galería
        val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
        for (imageView in imageViews) {
            imageView.setOnClickListener {
                selectedImageView = imageView // Guardar la referencia del ImageView seleccionado
                requestStoragePermission()
                openGallery()
            }
        }

        // Acción para abrir la cámara
        iv_camera.setOnClickListener {
            requestStoragePermission()
            openCamera()
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
                        selectedImageView?.setImageURI(selectedImage) // Usar el ImageView seleccionado
                        uploadImageToFirebaseStorage(selectedImage)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    data?.extras?.get("data")?.let { imageBitmap ->
                        val bitmap = imageBitmap as Bitmap
                        selectedImageView?.setImageBitmap(bitmap) // Usar el ImageView seleccionado
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
        when (image) {
            is Bitmap -> {
                // Subir imagen capturada con la cámara
                uploadBitmapToFirebaseStorage(image)
            }
            is Uri -> {
                // Subir imagen seleccionada de la galería
                uploadUriToFirebaseStorage(image)
            }
        }
    }

    private fun uploadUriToFirebaseStorage(imageUri: Uri) {
        FirebaseRepository.uploadUriToFirebaseStorage(
            userId,
            imageUri,
            onSuccess = { imageUrl ->
                val successMessage = "Image URL saved successfully: $imageUrl"
                FirebaseRepository.saveImageUrlToFirestore(
                    userId,
                    imageUrl,
                    onSuccess = {
                        Log.d("Firestore", successMessage)
                        // Opcional: Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        val errorMessage = "Error saving image URL: ${e.message}"
                        Log.e("Firestore", errorMessage)
                        // Opcional: Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = { exception ->
                val uploadError = "Error uploading image: ${exception.message}"
                Log.e("Upload", uploadError)
                // Opcional: Toast.makeText(this, uploadError, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun uploadBitmapToFirebaseStorage(bitmap: Bitmap) {
        FirebaseRepository.uploadBitmapToFirebaseStorage(
            userId,
            bitmap,
            onSuccess = { imageUrl ->
                saveImageUrlToFirestore(
                    userId,
                    imageUrl,
                    onSuccess = {
                        val successMessage = getString(R.string.upload_success, imageUrl)
                        Log.d("Firestore", successMessage)
                    },
                    onFailure = { e ->
                        val errorMessage = getString(R.string.save_error, e.message)
                        Log.e("Firestore", errorMessage)
                    }
                )
            },
            onFailure = { exception ->
                val uploadError = getString(R.string.upload_error, exception.message)
                Log.e("Upload", uploadError)
            }
        )
    }

    private fun saveImageUrlToFirestore(
        userId: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val savingMessage = getString(R.string.saving_image, userId, imageUrl)
        Log.d("Firestore", savingMessage)
        FirebaseRepository.saveImageUrlToFirestore(userId, imageUrl, onSuccess, onFailure)
    }

    private fun loadUserImages() {
        FirebaseRepository.loadUserImages(
            userId,
            onSuccess = { imageUris ->
                val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
                for ((index, uri) in imageUris.withIndex()) {
                    if (index < imageViews.size) {
                        loadImageIntoImageView(uri, imageViews[index])
                    }
                }
                val loadSuccessMessage = getString(R.string.load_images_success, imageUris.size)
                Log.d("Firebase", loadSuccessMessage)
            },
            onFailure = { e ->
                val errorMessage = getString(R.string.load_images_error, e.message)
                Log.e("Firebase", errorMessage)
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }




    private fun loadImageIntoImageView(imageUri: Uri, imageView: ImageView) {
        // Usar Glide o Picasso para cargar la imagen en el ImageView
        Glide.with(this)
            .load(imageUri)
            .into(imageView)
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

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
                        // Nada
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
                Log.e("PerfilActivity", errorMessage, e)
            }
        )
    }

    fun calcularEdad(fechaNacimiento: String): Int {
            val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
            val fechaNacimiento = LocalDate.parse(fechaNacimiento, formatter)
            val hoy = LocalDate.now()
            return Period.between(fechaNacimiento, hoy).years
        }

}
