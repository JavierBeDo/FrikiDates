package com.example.frikidates

import android.content.Intent
import android.os.Bundle

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class PerfilActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_search: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        nav_profile = findViewById(R.id.nav_chat)
        nav_search = findViewById(R.id.nav_search2)

        nav_profile.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
        nav_search.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
        }
    }
}
