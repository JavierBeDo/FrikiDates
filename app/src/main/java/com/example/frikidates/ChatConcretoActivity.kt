package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ServerValue
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
    private var fotoPerfilCadena = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_concretos)

        fotoPerfil = findViewById(R.id.profile_image_2) // ID actualizado
        nombre = findViewById(R.id.username) // ID actualizado
        rvMensajes = findViewById(R.id.rvMensajes) // Asegúrate de que este ID sigue siendo válido
        txtMensaje = findViewById(R.id.message_input) // ID actualizado
        btnEnviar = findViewById(R.id.send_button) // ID actualizado
        btnEnviarFoto = findViewById(R.id.btnEnviarFoto) // Asegúrate de que este ID es correcto

        adapter = AdapterMensajes(this)
        val layoutManager = LinearLayoutManager(this)
        rvMensajes.layoutManager = layoutManager
        rvMensajes.adapter = adapter

        btnEnviar.setOnClickListener {
            val mensajeTexto = txtMensaje.text.toString()
            if (mensajeTexto.isNotEmpty()) {
                val timestampLocal = System.currentTimeMillis() // Obtén el timestamp local
                val mensajeEnviar = MensajeEnviar(
                    mensajeTexto,
                    nombre.text.toString(),
                    fotoPerfilCadena,
                    "1",
                    mapOf("timestamp" to timestampLocal) // Usa un mapa con el timestamp local
                )
                // databaseReference.push().setValue(mensajeEnviar)
                adapter.addMensaje(mensajeEnviar)
                txtMensaje.text.clear()
            }
        }

        btnEnviarFoto.setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivityForResult(Intent.createChooser(i, "Selecciona una foto"), PHOTO_SEND)
        }

        fotoPerfil.setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivityForResult(Intent.createChooser(i, "Selecciona una foto"), PHOTO_PERFIL)
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
            }
        })
    }

    private fun setScrollbar() {
        rvMensajes.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Manejo de resultados para fotos enviadas y perfil
        if (requestCode == PHOTO_SEND && resultCode == RESULT_OK) {
            val u = data?.data
            // Aquí puedes manejar la lógica para enviar la foto
        } else if (requestCode == PHOTO_PERFIL && resultCode == RESULT_OK) {
            val u = data?.data
            // Aquí puedes manejar la lógica para actualizar la foto de perfil
        }
    }
}
