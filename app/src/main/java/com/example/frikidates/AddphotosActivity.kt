package com.example.frikidates

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.example.frikidates.firebase.FirebaseRepository


class AddphotosActivity(private val c: Context) : AppCompatActivity() {

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

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        userId = user?.userId ?: return
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

        val imageViews =
            listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
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
        FirebaseRepository.uploadImage(userId, image, { imageUrl ->
            FirebaseRepository.saveImageUrl(userId, imageUrl, {
                Log.d(c.getString(R.string.tag_firestore), "Image URL saved: $imageUrl")
            }, { e ->
                Log.e(c.getString(R.string.tag_firestore), "Error saving image: ${e.message}")
            })
        }, { e ->
            Log.e(c.getString(R.string.tag_upload), "Error: ${e.message}")
        })
    }

    private fun loadUserImages() {
        FirebaseRepository.loadUserImages(userId, { urls ->
            val imageViews =
                listOf(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6)
            for ((index, url) in urls.withIndex()) {
                if (index < imageViews.size) {
                    Glide.with(this).load(url).into(imageViews[index])
                }
            }
        }, { e ->
            Toast.makeText(this, getString(R.string.error_loading_images), Toast.LENGTH_SHORT).show()
        })

    }
}
