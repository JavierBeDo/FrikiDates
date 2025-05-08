package com.example.frikidates

import android.content.Intent
import android.os.Bundle

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale


class ChatsActivity : AppCompatActivity() {

    private lateinit var navProfile: ImageView
    private lateinit var navSearch: ImageView
    private lateinit var userChat: CardView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        navSearch = findViewById(R.id.nav_search)
        navProfile = findViewById(R.id.nav_profile)
        userChat = findViewById(R.id.cd_chats)

        val db = FirebaseFirestore.getInstance()
        db.collection("matches")
            .whereEqualTo("user_1", currentUserUid)
            .get()
            .addOnSuccessListener { docsUser1 ->
                db.collection("matches")
                    .whereEqualTo("user_2", currentUserUid)
                    .get()
                    .addOnSuccessListener { docsUser2 ->

                        val allMatches = docsUser1.documents + docsUser2.documents

                        for (doc in allMatches) {
                            val matchId = doc.id
                            val otherUserId = if (doc.getString("user_1") == currentUserUid)
                                doc.getString("user_2")
                            else
                                doc.getString("user_1")

                            // Carga último mensaje
                            db.collection("matches")
                                .document(matchId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { messages ->
                                    val lastMessage = messages.firstOrNull()
                                    val lastText = when (lastMessage?.getString("type")) {
                                        "image" -> "📷 Imagen"
                                        else -> lastMessage?.getString("content") ?: "Nuevo chat"
                                    }
                                    val lastTime = lastMessage?.getTimestamp("timestamp")?.toDate()?.let {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                                    } ?: ""

                                    // Aquí llamas a tu función para renderizar la vista del chat
                                    cargarVistaChat(matchId, otherUserId, lastText, lastTime)
                                }
                        }
                    }
            }

        navSearch.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
        navProfile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

//        userChat.setOnClickListener {
//            startActivity(Intent(this, ChatConcretoActivity::class.java))
//        }
    }

    private fun cargarVistaChat(matchId: String, otherUserId: String?, lastText: String, lastTime: String) {
        if (otherUserId == null) return // seguridad básica

        val layout = findViewById<LinearLayout>(R.id.messageList)
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { userDoc ->
                val nombreUsuario = userDoc.getString("name") ?: "Usuario"
                val cardView = layoutInflater.inflate(R.layout.chat_card, layout, false) as CardView

                val nombre = cardView.findViewById<TextView>(R.id.nombreMensaje)
                val mensaje = cardView.findViewById<TextView>(R.id.mensajeMensaje)
                val hora = cardView.findViewById<TextView>(R.id.horaMensaje)

                nombre.text = nombreUsuario
                mensaje.text = lastText
                hora.text = lastTime

                cardView.setOnClickListener {
                    val intent = Intent(this, ChatConcretoActivity::class.java)
                    intent.putExtra("matchId", matchId)
                    intent.putExtra("otherUserId", otherUserId)
                    startActivity(intent)
                }

                layout.addView(cardView)
            }
    }

}
