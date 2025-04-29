package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_chat: ImageView
    private lateinit var linearLayout_info: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        nav_profile = findViewById(R.id.nav_profile)
        nav_chat = findViewById(R.id.nav_chat)
        // Inicializa linearLayout_info aquí
        linearLayout_info = findViewById(R.id.linearLayout_info)


        nav_chat.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
        }

        nav_profile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

        linearLayout_info.setOnClickListener {
                    startActivity(Intent(this, InfoProfileActivity::class.java))
        }
        }
    }
