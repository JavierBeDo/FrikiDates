package com.example.frikidates

import android.content.Intent
import android.os.Bundle

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity


class ChatsActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_search: ImageView
    private lateinit var Ly_1: LinearLayout
    private lateinit var Ly_2: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        nav_search = findViewById(R.id.nav_search)
        nav_profile = findViewById(R.id.nav_profile)
        Ly_1 = findViewById(R.id.Ly_1)
        Ly_2 = findViewById(R.id.Ly_2)

        nav_search.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
        nav_profile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

        Ly_1.setOnClickListener {
            startActivity(Intent(this, ChatConcretoActivity::class.java))
        }
    }
}
