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

    private val listMensaje: MutableList<MensajeRecibir> = ArrayList()

    fun addMensaje(v: MensajeRecibir) {
        listMensaje.add(v)
        notifyItemInserted(listMensaje.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderMensaje {
        val v = LayoutInflater.from(c).inflate(R.layout.card_view_mensajes, parent, false)
        return HolderMensaje(v)
    }

    override fun onBindViewHolder(holder: HolderMensaje, position: Int) {
        val mensaje = listMensaje[position]

        holder.nombre.text = mensaje.senderId

        if (mensaje.type == "image") {
            holder.fotoMensaje.visibility = View.VISIBLE
            holder.mensaje.visibility = View.GONE
            Glide.with(c).load(mensaje.text).into(holder.fotoMensaje)
        } else {
            holder.fotoMensaje.visibility = View.GONE
            holder.mensaje.visibility = View.VISIBLE
            holder.mensaje.text = mensaje.text
        }

        holder.fotoMensajePerfil.setImageResource(R.mipmap.ic_launcher) // Se puede actualizar según sea necesario

        val timestamp = mensaje.timestamp?.time ?: 0L
        val d = Date(timestamp)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.hora.text = sdf.format(d)
    }

    override fun getItemCount(): Int = listMensaje.size
}