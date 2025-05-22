package com.example.frikidates

import MensajeEnviar
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import com.google.firebase.database.ValueEventListener


class ChatConcretoActivity : BaseActivity() {

    private lateinit var fotoPerfil: CircleImageView
    private lateinit var nombre: TextView
    private lateinit var rvMensajes: RecyclerView
    private lateinit var txtMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var btnEnviarFoto: ImageButton
    private lateinit var adapter: AdapterMensajes

    private val PHOTO_SEND = 1
    private val PHOTO_PERFIL = 2

    private lateinit var estadoUsuario: ImageView


    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Oculta la barra, pero el menú sigue funcionando
        setContentView(R.layout.activity_chats_concretos)

        // ==== Vistas ====
        fotoPerfil   = findViewById(R.id.profile_image_2)
        nombre       = findViewById(R.id.username)
        rvMensajes   = findViewById(R.id.rvMensajes)
        txtMensaje   = findViewById(R.id.message_input)
        btnEnviar    = findViewById(R.id.send_button)
        btnEnviarFoto= findViewById(R.id.btnEnviarFoto)
        estadoUsuario = findViewById(R.id.estado_usuario)


        // ==== Preparar Recycler + Adapter ====
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        adapter = AdapterMensajes(this, senderId)
        val lm = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMensajes.layoutManager = lm
        rvMensajes.adapter = adapter

        // Dentro de onCreate, después de recibir matchId:
        val matchId = intent.getStringExtra("matchId") ?: "match_1"
        val db = FirebaseFirestore.getInstance()

// Obtener el matchedUserId desde el documento del match
        db.collection("matches")
            .document(matchId)
            .get()
            .addOnSuccessListener { matchDoc ->
                val matchedUserId = matchDoc.getString("matchedUserId") ?: return@addOnSuccessListener

                // === 1. Obtener nombre del usuario ===
                db.collection("profiles")
                    .document(matchedUserId)
                    .get()
                    .addOnSuccessListener { profileDoc ->
                        val nombreReal = profileDoc.getString("name") ?: "Usuario"
                        nombre.text = nombreReal

                        // === 2. Obtener imagen de perfil ===
                        val folderName = matchedUserId.removePrefix("profile_")
                        val estadoRef = FirebaseDatabase.getInstance("https://frikidatesdb-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("user/$folderName/status")

                        estadoRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val estado = snapshot.getValue(String::class.java)
                                Log.d("EstadoUsuario", "Snapshot recibido con estado: $estado")
                                if (estado == "active") {
                                    estadoUsuario.setImageResource(R.drawable.circle_online)
                                } else {
                                    estadoUsuario.setImageResource(R.drawable.circle_inactive)
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("EstadoUsuario", "Error consultando estado: ${error.message}")
                            }
                        })

                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            Log.e("BaseActivity", "Usuario no autenticado, no se puede actualizar estado")
                            return@addOnSuccessListener
                        }
                        Log.d("BaseActivity", "Usuario autenticado: ${user.uid}")
                        Log.d("BaseActivity", "el otro: $folderName")
                        Log.d("EstadoUsuario", "Escuchando en ruta: user/$folderName/status")



                        val storageRef = FirebaseStorage.getInstance().reference.child(folderName)

                        storageRef.listAll()
                            .addOnSuccessListener { result ->
                                val primeraImagen = result.items.firstOrNull()
                                if (primeraImagen != null) {
                                    primeraImagen.downloadUrl.addOnSuccessListener { uri ->
                                        Glide.with(this)
                                            .load(uri)
                                            .placeholder(R.drawable.default_avatar)
                                            .circleCrop()
                                            .into(fotoPerfil)
                                    }
                                } else {
                                    fotoPerfil.setImageResource(R.drawable.default_avatar)
                                }
                            }
                            .addOnFailureListener {
                                fotoPerfil.setImageResource(R.drawable.default_avatar)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatConcreto", "Error obteniendo perfil: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatConcreto", "Error obteniendo matchedUserId: ${e.message}")
            }



        // ==== 2) Envío de texto ====
        btnEnviar.setOnClickListener {
            val mensajeTexto = txtMensaje.text.toString().trim()
            if (mensajeTexto.isNotEmpty()) {
                val mensaje = MensajeEnviar(
                    senderId = senderId,
                    text      = mensajeTexto,
                    timestamp = FieldValue.serverTimestamp(),
                    type      = "text"
                )
                db.collection("matches")
                    .document(matchId)
                    .collection("messages")
                    .document("mensaje_${System.currentTimeMillis()}")
                    .set(mensaje)
                    .addOnSuccessListener { txtMensaje.text.clear() }
                    .addOnFailureListener { e ->
                        Log.e("ChatConcreto", "Error enviando mensaje: ${e.message}")
                    }
            }
        }

        // ==== 3) Envío de foto ====
        btnEnviarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/jpeg"; putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivityForResult(
                Intent.createChooser(intent, "Selecciona una foto"), PHOTO_SEND
            )
        }

        // ==== 4) Listener de nuevos mensajes ====
        db.collection("matches")
            .document(matchId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, error ->
                if (error != null) {
                    Log.e("ChatConcreto", "Error escuchando mensajes: ${error.message}")
                    return@addSnapshotListener
                }
                for (dc in snaps!!.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val msg = dc.document.toObject(MensajeRecibir::class.java)
                        adapter.addMensaje(msg)
                    }
                }
            }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(posStart: Int, itemCount: Int) {
                super.onItemRangeInserted(posStart, itemCount)
                rvMensajes.scrollToPosition(adapter.itemCount - 1)
            }
        })

        val menuButton = findViewById<ImageView>(R.id.menu_boton)
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.menu_chat_opciones, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_deshacer_match -> {
                        showAlertDialog("¿Estás seguro de que deseas deshacer el match?")
                        true
                    }
                    R.id.menu_denunciar -> {
                        showAlertDialog("¿Deseas denunciar a este usuario?")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

    }



    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage(message)
            .setPositiveButton("Sí") { dialog, _ ->
                // Aquí va la lógica para deshacer match o denunciar
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
