package com.example.frikidates

import AdapterChats
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatsActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_search: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterChats
    private val chatList = mutableListOf<HolderChats>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        nav_search = findViewById(R.id.nav_search)
        nav_profile = findViewById(R.id.nav_profile)

        recyclerView = findViewById(R.id.CardChats)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdapterChats(chatList) { chat ->
            val intent = Intent(this, ChatConcretoActivity::class.java)
            intent.putExtra("userId", chat.userId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        nav_search.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }

        nav_profile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

        fetchChats()
    }

    private fun fetchChats() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("profiles")
            .document("profile_$currentUserId")
            .collection("matches")
            .get()
            .addOnSuccessListener { documents ->
                chatList.clear()
                for (doc in documents) {
                    val matchedUserId = doc.getString("matchedUserId") ?: continue
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                    // Obtener el nombre real del perfil del usuario con matchedUserId
                    db.collection("profiles")
                        .document(matchedUserId)
                        .get()
                        .addOnSuccessListener { profileDoc ->
                            val nombreReal = profileDoc.getString("name") ?: matchedUserId

                            val chat = HolderChats(
                                userId = matchedUserId,
                                username = nombreReal,
                                lastMessage = lastMessage,
                                timestamp = timestamp
                            )

                            chatList.add(chat)
                            adapter.notifyItemInserted(chatList.size - 1)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatsActivity", "Error getting profile for $matchedUserId", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatsActivity", "Error fetching matches", e)
            }
    }

}