package com.example.frikidates

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class AddphotosActivity : AppCompatActivity() {

    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var imageView3: ImageView
    private lateinit var imageView4: ImageView
    private lateinit var imageView5: ImageView
    private lateinit var imageView6: ImageView
    private lateinit var iv_camera: ImageView
    private lateinit var buttonUpload: Button

    private var selectedImageView: ImageView? = null
    private lateinit var userPreferences: UserPreferences
    private var user: User? = null

    private val REQUEST_CODE_GALLERY = 1001
    private val REQUEST_CODE_CAMERA = 1002
    private val REQUEST_CODE_STORAGE_PERMISSION = 1003

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        userPreferences = UserPreferences(this)
        user = userPreferences.getUser()

        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)
        imageView3 = findViewById(R.id.imageView3)
        imageView4 = findViewById(R.id.imageView4)
        imageView5 = findViewById(R.id.imageView5)
        imageView6 = findViewById(R.id.imageView6)
        iv_camera = findViewById(R.id.iv_camera)
        buttonUpload = findViewById(R.id.buttonUpload)

        val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
        for (imageView in imageViews) {
            imageView.setOnClickListener {
                selectedImageView = imageView
                requestStoragePermission()
                openGallery()
            }
        }

        iv_camera.setOnClickListener {
            selectedImageView = imageView1 // Por defecto, si no se ha tocado ninguno
            requestStoragePermission()
            openCamera()
        }

        buttonUpload.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadUserImages()
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
                        selectedImageView?.setImageURI(selectedImage)
                        uploadImageToFirebaseStorage(selectedImage)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    data?.extras?.get("data")?.let { imageBitmap ->
                        val bitmap = imageBitmap as Bitmap
                        selectedImageView?.setImageBitmap(bitmap)
                        uploadImageToFirebaseStorage(bitmap)
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
    }

    private fun uploadImageToFirebaseStorage(image: Any) {
        when (image) {
            is Bitmap -> {
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val data = baos.toByteArray()
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("${user?.userId}/${UUID.randomUUID()}.jpg")

                storageRef.putBytes(data)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            saveImageUrlToFirestore(uri.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Upload", "Error: ${e.message}")
                    }
            }

            is Uri -> {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("${user?.userId}/${UUID.randomUUID()}.jpg")

                storageRef.putFile(image)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            saveImageUrlToFirestore(uri.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Upload", "Error: ${e.message}")
                    }
            }
        }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val userId = user?.userId ?: return
        val imageData = hashMapOf("imageUrl" to imageUrl)

        db.collection("users").document(userId)
            .set(imageData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Image URL saved: $imageUrl")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving image: ${e.message}")
            }
    }

    private fun loadUserImages() {
        val userId = user?.userId ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("$userId/")

        storageRef.listAll()
            .addOnSuccessListener { list ->
                val imageViews = listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
                for ((index, item) in list.items.withIndex()) {
                    if (index < imageViews.size) {
                        item.downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri)
                                .into(imageViews[index])
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar imágenes", Toast.LENGTH_SHORT).show()
            }
    }
}
