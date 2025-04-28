package com.example.frikidates

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class AdapterMensajes(private val c: Context) : RecyclerView.Adapter<HolderMensaje>() {

    private val listMensaje: MutableList<MensajeEnviar> = ArrayList()

    fun addMensaje(v: MensajeEnviar) {
        listMensaje.add(v)
        notifyItemInserted(listMensaje.size - 1) // Corrige el índice
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderMensaje {
        val v = LayoutInflater.from(c).inflate(R.layout.card_view_mensajes, parent, false)
        return HolderMensaje(v)
    }

    override fun onBindViewHolder(holder: HolderMensaje, position: Int) {
        val mensaje = listMensaje[position]

        // Acceder a las propiedades directamente
        holder.nombre.text = mensaje.nombre // Cambié a mensaje.nombre
        holder.mensaje.text = mensaje.mensaje
        if (mensaje.type_mensaje == "2") {
            holder.fotoMensaje.visibility = View.VISIBLE
            holder.mensaje.visibility = View.VISIBLE
            Glide.with(c).load(mensaje.urlFoto).into(holder.fotoMensaje)
        } else if (mensaje.type_mensaje == "1") {
            holder.fotoMensaje.visibility = View.GONE
            holder.mensaje.visibility = View.VISIBLE
        }

        if (mensaje.fotoPerfil.isNullOrEmpty()) {
            holder.fotoMensajePerfil.setImageResource(R.mipmap.ic_launcher)
        } else {
            Glide.with(c).load(mensaje.fotoPerfil).into(holder.fotoMensajePerfil)
        }

        val codigoHora = listMensaje.get(position).hora?.get("timestamp")

        val timestamp: Long = when (codigoHora) {
            is Long -> codigoHora // Si ya es Long
            is Int -> codigoHora.toLong() // Si es Int, conviértelo a Long
            is String -> codigoHora.toLongOrNull() ?: 0L // Si es String, intenta convertirlo a Long
            else -> 0L // Valor por defecto en caso de que no sea ninguno de los anteriores
        }

        val d = Date(timestamp) // Ahora timestamp es un Long
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()) // AM/PM
        holder.hora.text = sdf.format(d)



    }


    override fun getItemCount(): Int = listMensaje.size
}
