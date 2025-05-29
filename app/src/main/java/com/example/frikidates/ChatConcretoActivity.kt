package com.example.frikidates

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.frikidates.firebase.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import de.hdodenhof.circleimageview.CircleImageView



class ChatConcretoActivity : BaseActivity() {

    private lateinit var fotoPerfil: CircleImageView
    private lateinit var nombreTextView: TextView
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
        nombreTextView = findViewById(R.id.username)
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

        // Obtener matchId
        val matchId = intent.getStringExtra("matchId") ?: getString(R.string.default_match_id)

        btnEnviar.setOnClickListener {
            val mensajeTexto = txtMensaje.text.toString().trim()
            if (mensajeTexto.isNotEmpty()) {
                FirebaseRepository.sendTextMessage(
                    matchId,
                    senderId,
                    mensajeTexto,
                    onSuccess = { txtMensaje.text.clear() },
                    onFailure = { e ->
                        Log.e("ChatConcreto", "Error al enviar mensaje: ${e.message}")
                    }
                )
            }
        }


        FirebaseRepository.getMatchedUserId(
            context = this,
            userId = matchId,
            onSuccess = { matchedUserId ->
                if (matchedUserId.isBlank()) {
                    Log.e("ChatConcreto", getString(R.string.error_getting_matched_user_id))
                    return@getMatchedUserId
                }

                FirebaseRepository.getUserNameAndObserveStatus(
                    context = this,
                    matchedUserId = matchedUserId,
                    onNameReceived = { nombre ->
                        nombreTextView.text = nombre
                    },
                    onStatusChanged = { estado ->
                        val resId = if (estado == "active") R.drawable.circle_online else R.drawable.circle_inactive
                        estadoUsuario.setImageResource(resId)
                    },
                    onError = { e ->
                        Log.e("ChatConcreto", getString(R.string.error_getting_profile_or_status, e.message ?: ""))
                    }
                )

                FirebaseRepository.getFirstProfileImage(
                    matchedUserId = matchedUserId,
                    onSuccess = { uri ->
                        Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.default_avatar)
                            .circleCrop()
                            .into(fotoPerfil)
                    },
                    onFailure = {
                        fotoPerfil.setImageResource(R.drawable.default_avatar)
                    }
                )
            },
            onFailure = { e ->
                Log.e("ChatConcreto", getString(R.string.error_getting_matched_user_id, e.message ?: ""))
            }
        )

        FirebaseRepository.listenMessages(
            matchId,
            onNewMessage = { msg ->
                adapter.addMensaje(msg)
            },
            onMessageUpdated = { msg ->
                adapter.updateMensaje(msg)
            },
            onError = { e ->
                Log.e("ChatConcreto", getString(R.string.error_listening_messages, e.message))
            }
        )

      //  btnEnviarFoto.setOnClickListener {
        //    openImageSelector()
        //}



        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                rvMensajes.post {
                    rvMensajes.scrollToPosition(adapter.itemCount - 1)
                }
            }
        })


        val menuButton = findViewById<ImageView>(R.id.menu_boton)
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.menu_chat_opciones, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_deshacer_match -> {
                        showAlertDialog(getString(R.string.dialog_message_unmatch))
                        true
                    }
                    R.id.menu_denunciar -> {
                        showAlertDialog(getString(R.string.dialog_message_report))
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
            .setTitle(getString(R.string.dialog_title_confirmation))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(getString(R.string.dialog_negative_button)) { dialog, _ -> dialog.dismiss() }
            .show()

    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), PHOTO_SEND)

    }

}
