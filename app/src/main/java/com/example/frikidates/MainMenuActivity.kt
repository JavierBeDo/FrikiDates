package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        nav_profile = findViewById(R.id.nav_profile)

        nav_profile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }
        }
    }
