package com.example.frikidates

import android.content.Intent
import android.os.Bundle

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class ChatsActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_search: ImageView
    private lateinit var Ly_1: CardView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        nav_search = findViewById(R.id.nav_search)
        nav_profile = findViewById(R.id.nav_profile)
        Ly_1 = findViewById(R.id.cd_chats)

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
