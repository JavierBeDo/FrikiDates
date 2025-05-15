package com.example.frikidates

import CardStackAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView

class MainMenuActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_chat: ImageView
    private lateinit var linearLayout_info: LinearLayout
    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        nav_profile = findViewById(R.id.nav_profile)
        nav_chat = findViewById(R.id.nav_chat)
        // Inicializa linearLayout_info aquí
        linearLayout_info = findViewById(R.id.linearLayout_info)
        cardStackView = findViewById(R.id.card_stack_view)

        manager = CardStackLayoutManager(this)
        cardStackView.layoutManager = manager
        cardStackView.adapter = CardStackAdapter(getDummyData())

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
    private fun getDummyData(): List<Profile> {
        return listOf(
            Profile("María", 26, "Madrid", "Alta compatibilidad", R.drawable.user1),
            Profile("Laura", 24, "Barcelona", "Muy compatible", R.drawable.user2),
            Profile("Lucía", 28, "Sevilla", "Compatible", R.drawable.user3)
        )
    }
}


