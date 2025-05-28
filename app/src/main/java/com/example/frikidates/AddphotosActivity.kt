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
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.frikidates.firebase.FirebaseRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        userPreferences = UserPreferences(this)
        user = userPreferences.getUser()

        userId = user?.userId ?: run {
            Toast.makeText(this, "Error: no se encontró el usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
                                    Toast.makeText(this@AddphotosActivity, "Máximo 6 imágenes. Reemplaza una existente.", Toast.LENGTH_SHORT).show()
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

        buttonUpload.setOnClickListener {
            if (imageViews.any { it.tag != null }) {
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Debes subir al menos una foto", Toast.LENGTH_SHORT).show()
            }
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
                        selectedImageView?.let {
                            Glide.with(this).load(R.drawable.loading_animation).into(it)
                            uploadImageToFirebaseStorage(selectedImage)
                        }
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    data?.extras?.get("data")?.let { imageBitmap ->
                        val bitmap = imageBitmap as Bitmap
                        selectedImageView?.let {
                            Glide.with(this).load(R.drawable.loading_animation).into(it)
                            uploadImageToFirebaseStorage(bitmap)
                        }
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

    private fun loadImageIntoImageView(uri: Uri, imageView: ImageView) {
        Glide.with(imageView.context)
            .load(uri)
            .placeholder(R.drawable.loading_animation)
            .error(R.drawable.landscapeplaceholder)
            .into(imageView)

        imageView.tag = uri.toString()
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