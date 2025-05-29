package com.example.frikidates

import AdapterChats
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frikidates.firebase.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth

class ChatsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterChats
    private val chatList = mutableListOf<HolderChats>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        recyclerView = findViewById(R.id.CardChats)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdapterChats(chatList) { chat ->
            val intent = Intent(this, ChatConcretoActivity::class.java)
            intent.putExtra("matchId", chat.matchId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        BottomNavManager.setupNavigation(this)
        fetchChats()
    }

    override fun onResume() {
        super.onResume()
        fetchChats() // Recargar chats para actualizar lastMessage
    }

    private fun fetchChats() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseRepository.fetchChatsForUser(
            currentUserId,
            onChatsLoaded = { chats ->
                chatList.clear()
                chatList.addAll(chats.sortedByDescending { it.timestamp }) // Ordenar por timestamp
                adapter.notifyDataSetChanged()
            },
            onError = { e ->
                Log.e("ChatsActivity", getString(R.string.error_fetching_matches, e.message))
            }
        )
    }
}