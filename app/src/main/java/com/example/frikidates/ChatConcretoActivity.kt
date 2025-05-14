package com.example.frikidates

import MensajeEnviar
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class ChatConcretoActivity : AppCompatActivity() {

    private lateinit var fotoPerfil: CircleImageView
    private lateinit var nombre: TextView
    private lateinit var rvMensajes: RecyclerView
    private lateinit var txtMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var adapter: AdapterMensajes
    private lateinit var btnEnviarFoto: ImageButton

    private val PHOTO_SEND = 1
    private val PHOTO_PERFIL = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_concretos)

        fotoPerfil = findViewById(R.id.profile_image_2)
        nombre = findViewById(R.id.username)
        rvMensajes = findViewById(R.id.rvMensajes)
        txtMensaje = findViewById(R.id.message_input)
        btnEnviar = findViewById(R.id.send_button)
        btnEnviarFoto = findViewById(R.id.btnEnviarFoto)

        adapter = AdapterMensajes(this)
        rvMensajes.layoutManager = LinearLayoutManager(this)
        rvMensajes.adapter = adapter

        val matchId = intent.getStringExtra("matchId") ?: "match_1"
        val db = FirebaseFirestore.getInstance()
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        btnEnviar.setOnClickListener {
            val mensajeTexto = txtMensaje.text.toString().trim()
            if (mensajeTexto.isNotEmpty()) {

                val mensaje = MensajeEnviar(
                    senderId = senderId,
                    text = mensajeTexto,
                    timestamp = FieldValue.serverTimestamp(),
                    type = "text"
                )

                db.collection("matches")
                    .document(matchId)
                    .collection("messages")
                    .document("mensaje_${System.currentTimeMillis()}")
                    .set(mensaje)
                    .addOnSuccessListener {
                        txtMensaje.text.clear()
                    }
                    .addOnFailureListener {
                        Log.e("ChatConcreto", "Error al enviar mensaje: ${it.message}")
                    }
            }
        }

        btnEnviarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), PHOTO_SEND)
        }

        fotoPerfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), PHOTO_PERFIL)
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
            }
        })

        db.collection("matches")
            .document(matchId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ChatConcreto", "Error escuchando mensajes: ${error.message}")
                    return@addSnapshotListener
                }

                for (doc in snapshots!!.documentChanges) {
                    val mensaje = doc.document.toObject(MensajeRecibir::class.java)
                    if (doc.type == DocumentChange.Type.ADDED) {
                        adapter.addMensaje(mensaje)
                    }
                }
            }
    }

    private fun setScrollbar() {
        rvMensajes.scrollToPosition(adapter.itemCount - 1)
    }

    /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_SEND && resultCode == RESULT_OK) {
            val uri = data?.data
            enviarFoto(uri)
        } else if (requestCode == PHOTO_PERFIL && resultCode == RESULT_OK) {
            val uri = data?.data
            // Aquí puedes manejar la lógica para actualizar la foto de perfil
        }
    }

    private fun enviarFoto(uri: Uri?) {
        val matchId = intent.getStringExtra("matchId") ?: return
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        uri?.let { imageUri ->
            val storageReference = FirebaseStorage.getInstance().reference.child("imagenes/${System.currentTimeMillis()}.jpg")

            storageReference.putFile(imageUri)
                .addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        val timestamp = System.currentTimeMillis()
                        val mensajeId = "mensaje_$timestamp"
                        val db = FirebaseFirestore.getInstance()

                        val mensaje = hashMapOf(
                            "senderId" to senderId,
                            "text" to downloadUri.toString(),
                            "timestamp" to FieldValue.serverTimestamp(),
                            "type" to "image"
                        )

                        db.collection("matches")
                            .document(matchId)
                            .collection("messages")
                            .document(mensajeId)
                            .set(mensaje)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatConcretoActivity", "Error al subir la imagen: ${exception.message}")
                }
        }
    }
    */
}
