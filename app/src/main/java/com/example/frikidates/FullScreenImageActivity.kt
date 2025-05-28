package com.example.frikidates

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullScreenImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageView = findViewById<ImageView>(R.id.full_screen_image)
        val btnClose = findViewById<Button>(R.id.btn_close)

        // Obtener la URL de la imagen del Intent
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)

        // Cargar la imagen con Glide
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.landscapeplaceholder)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.landscapeplaceholder)
        }

        // Cerrar la actividad al pulsar el botón
        btnClose.setOnClickListener {
            finish()
        }
    }
}