package com.example.frikidates

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddphotosActivity : AppCompatActivity() {

    private lateinit var buttonUpload: Button
    private lateinit var imageView: ImageView
    private lateinit var iv_camera: ImageView

    private val REQUEST_CODE_GALLERY = 1001
    private val REQUEST_CODE_CAMERA = 1002
    private val REQUEST_CODE_STORAGE_PERMISSION = 1003

    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID().toString()}")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        buttonUpload = findViewById(R.id.buttonUpload)
        imageView = findViewById(R.id.imageView1)
        iv_camera = findViewById(R.id.iv_camera)

        // Acción para abrir la galería
        imageView.setOnClickListener {
            requestStoragePermission()
            openGallery()
        }
        // Acción para abrir la cámara
        iv_camera.setOnClickListener {
            requestStoragePermission()
            openCamera()
        }

        // Acción para volver a la vista de login
        buttonUpload.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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
                        // Cargar y mostrar la imagen seleccionada en la galería
                        imageView.setImageURI(selectedImage)
                        uploadImageToFirebaseStorage(selectedImage)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    data?.extras?.get("data")?.let { imageBitmap ->
                        // Cargar y mostrar la imagen capturada con la cámara
                        imageView.setImageBitmap(imageBitmap as Bitmap)
                        uploadImageToFirebaseStorage(imageBitmap as Bitmap)
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
        } else {
            // Permisos otorgados, puedes proceder con la subida de la imagen
            // No es necesario llamar a este método aquí, ya que se llama antes de abrir la galería o la cámara
        }
    }

    private fun uploadImageToFirebaseStorage(image: Any) {
        when (image) {
            is Bitmap -> {
                // Subir imagen capturada con la cámara
            }
            is android.net.Uri -> {
                // Subir imagen seleccionada de la galería
                storageRef.putFile(image)
                    .addOnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Aquí puedes guardar la URL de descarga de la imagen en tu base de datos de Firebase
                            // o realizar otras acciones con la imagen subida
                            val imageUrl = downloadUri.toString()
                            // Guarda la URL de la imagen en tu base de datos
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Maneja los errores de la subida de la imagen
                    }
            }
        }
    }
}
