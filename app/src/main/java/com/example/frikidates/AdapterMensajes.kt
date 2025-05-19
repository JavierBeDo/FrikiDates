package com.example.frikidates

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdapterMensajes(private val c: Context, private val myUserId: String) : RecyclerView.Adapter<HolderMensaje>() {

    private val listMensaje: MutableList<MensajeRecibir> = ArrayList()
    private val cacheUsuarios = mutableMapOf<String, Pair<String, String>>() // senderId -> Pair(nombre, fotoUrl)

    companion object {
        private const val VIEW_TYPE_PROPIO = 1
        private const val VIEW_TYPE_OTRO = 2
    }

    fun addMensaje(v: MensajeRecibir) {
        listMensaje.add(v)
        notifyItemInserted(listMensaje.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (listMensaje[position].senderId == myUserId) VIEW_TYPE_PROPIO else VIEW_TYPE_OTRO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderMensaje {
        val layoutId = if (viewType == VIEW_TYPE_PROPIO) {
            R.layout.card_view_mensajes_propios
        } else {
            R.layout.card_view_mensajes
        }

        val v = LayoutInflater.from(c).inflate(layoutId, parent, false)
        return HolderMensaje(v)
    }

    override fun onBindViewHolder(holder: HolderMensaje, position: Int) {
        val mensaje = listMensaje[position]
        val senderId = mensaje.senderId

        // Mostrar nombre y foto solo si es de otro usuario
        if (senderId != myUserId) {
            FirebaseFirestore.getInstance().collection("profiles")
                .document("profile_"+ senderId)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("name") ?: "Desconocido"
                    val fotoUrl = doc.getString("fotoPerfil") ?: ""

                    cacheUsuarios[senderId] = Pair(nombre, fotoUrl)

                   // holder.nombre.text = nombre
                    if (fotoUrl.isNotEmpty()) {
                        // Glide.with(c).load(fotoUrl).into(holder.fotoMensajePerfil)
                    } else {
                        // holder.fotoMensajePerfil.setImageResource(R.mipmap.ic_launcher)
                    }
                }
                .addOnFailureListener {
                    Log.e("AdapterMensajes", "Error obteniendo datos de usuario: ${it.message}")
                }
        }

        // Mostrar texto o imagen
        if (mensaje.type == "image") {
            holder.fotoMensaje.visibility = View.VISIBLE
            holder.mensaje.visibility = View.GONE
            Glide.with(c).load(mensaje.text).into(holder.fotoMensaje)
        } else {
            holder.fotoMensaje.visibility = View.GONE
            holder.mensaje.visibility = View.VISIBLE
            holder.mensaje.text = mensaje.text
        }

        // Mostrar hora
        val timestamp = mensaje.timestamp?.time ?: 0L
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.hora.text = sdf.format(Date(timestamp))
    }

    override fun getItemCount(): Int = listMensaje.size
}
