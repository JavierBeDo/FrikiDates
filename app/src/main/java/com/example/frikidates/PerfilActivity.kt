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
import com.example.frikidates.utils.InterestManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

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

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userPreferences: UserPreferences

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val layout = findViewById<LinearLayout>(R.id.ll_interest_vertical)
        interestManager = InterestManager(this, layout)

        userPreferences = UserPreferences(this)

        //esto carga el nav
        BottomNavManager.setupNavigation(this) {
            updateDescriptionInDatabase(descEdit.text.toString())
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
        val storageRef = FirebaseStorage.getInstance().reference.child("${user?.userId}/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    // Guarda la URL de la imagen en Firestore
                    saveImageUrlToFirestore(imageUrl)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Upload", "Error uploading image: ${exception.message}")
            }
    }

    private fun uploadBitmapToFirebaseStorage(bitmap: Bitmap) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")

        // Convertir Bitmap a ByteArray
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    // Guarda la URL de la imagen en Firestore
                    saveImageUrlToFirestore(imageUrl)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Upload", "Error uploading image: ${exception.message}")
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        // Crear un objeto de usuario o simplemente guardar la URL en una colección
        val userId = user?.userId // Reemplaza con el ID del usuario actual
        val imageData = hashMapOf(
            "imageUrl" to imageUrl
        )

        if (userId != null) {
            db.collection("users").document(userId)
                .set(imageData, SetOptions.merge()) // Merge para no sobrescribir otros campos
                .addOnSuccessListener {
                    Log.d("Firestore", "Image URL saved successfully: $imageUrl")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error saving image URL: ${e.message}")
                }
        }
    }

    private fun loadUserImages() {
        val storageRef = FirebaseStorage.getInstance().reference.child("${user?.userId}/")

        // Verificar si la carpeta del usuario existe
        storageRef.listAll().addOnSuccessListener { listResult ->
            if (listResult.items.isNotEmpty()) {
                // Si hay imágenes, cargarlas en los ImageViews
                val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
                for ((index, item) in listResult.items.withIndex()) {
                    if (index < imageViews.size) {
                        item.downloadUrl.addOnSuccessListener { uri ->
                            loadImageIntoImageView(uri, imageViews[index])
                        }.addOnFailureListener { exception ->
                            Log.e("Firebase", "Error getting download URL: ${exception.message}")
                        }
                    }
                }
            } else {
                Log.d("Firebase", "No images found for user: ${user?.userId}")
                Toast.makeText(this, "No images found for user: ${user?.userId}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error checking user folder: ${exception.message}")
            Toast.makeText(this, "Error checking user folder: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageIntoImageView(imageUri: Uri, imageView: ImageView) {
        // Usar Glide o Picasso para cargar la imagen en el ImageView
        Glide.with(this)
            .load(imageUri)
            .into(imageView)
    }

    private fun loadUserInfo() {


        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

            val profileId = "profile_$uid"
            db.collection("profiles").document(profileId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name") // Obtén el nombre del documento
                        val birthdate = document.getString("birthdate") // Suponiendo que también tienes la edad
                        val gender = document.getString("genero") // Y el género
                        val description = document.getString("bio") // Y la descripcion
                        val surname =  document.getString("surname") // Y el surname
                        val edad = birthdate?.let { calcularEdad(it) } ?: "Desconocida"

                        // Actualiza el TextView
                        val userInfoText = "Name: $name\nSurname: $surname\nEdad: $edad\nGénero: $gender"
                        findViewById<TextView>(R.id.userInfo).text = userInfoText
                        descEdit.setText(description)

                    } else {
                        Log.d("Firestore", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
    }

    // Método para actualizar la descripción en la base de datos
    private fun updateDescriptionInDatabase(description: String) {
        user?.let { currentUser ->
            val profileId = "profile_${currentUser.userId}"
            db.collection("profiles").document(profileId)
                .update("bio", description)
                .addOnSuccessListener {
                    Log.d("Firestore", "Descripción actualizada correctamente")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al actualizar la descripción", e)
                }
        }
    }

    private fun setupGenderSpinner(
        spinner: Spinner,
        firestoreFieldName: String,
        getDefault: Boolean = true
    ) {
        db.collection("gender")
            .get()
            .addOnSuccessListener { documents ->
                val genders = mutableListOf<String>()
                for (document in documents) {
                    val genderName = document.getString("name")
                    genderName?.let { genders.add(it) }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                var isInitialized = false

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        if (isInitialized) {
                            val selectedGender = genders[position]
                            updateGenderInFirestore(firestoreFieldName, selectedGender)
                        }
                        isInitialized = true
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

                if (getDefault) {
                    setDefaultGenderSelection(spinner, genders, firestoreFieldName)
                }

            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al cargar géneros", e)
            }
    }


    private fun setDefaultGenderSelection(spinner: Spinner, genders: List<String>, fieldName: String) {
        user?.let { currentUser ->
            val profileId = "profile_${currentUser.userId}"
            db.collection("profiles").document(profileId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val savedGender = document.getString(fieldName)
                        savedGender?.let {
                            val position = genders.indexOf(it)
                            if (position >= 0) {
                                spinner.setSelection(position)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error obteniendo $fieldName", e)
                }
        }
    }


    private fun updateGenderInFirestore(fieldName: String, gender: String) {
        user?.let { currentUser ->
            val profileId = "profile_${currentUser.userId}"
            db.collection("profiles").document(profileId)
                .update(fieldName, gender)
                .addOnSuccessListener {
                    Log.d("Firestore", "$fieldName actualizado a $gender")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error actualizando $fieldName", e)
                }
        }
    }


        private fun loadNotificationSettings() {
            user?.let { currentUser ->
                val profileId = "profile_${currentUser.userId}"
                db.collection("profiles").document(profileId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val notificationsEnabled = document.getBoolean("notificaciones") ?: false
                            notificationCheckBox.isChecked = notificationsEnabled
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error al cargar la configuración de notificaciones", e)
                    }
            }
        }


        private fun updateNotificationSettingsInDatabase(enabled: Boolean) {
            user?.let { currentUser ->
                val profileId = "profile_${currentUser.userId}"
                db.collection("profiles").document(profileId)
                    .update("notificaciones", enabled)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Configuración de notificaciones actualizada a $enabled")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error al actualizar la configuración de notificaciones", e)
                    }
            }
        }

    fun calcularEdad(fechaNacimiento: String): Int {
            val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
            val fechaNacimiento = LocalDate.parse(fechaNacimiento, formatter)
            val hoy = LocalDate.now()
            return Period.between(fechaNacimiento, hoy).years
        }

}
