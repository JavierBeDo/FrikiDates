package com.example.frikidates

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HolderMensaje(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // Definir las propiedades directamente
   // val nombre: TextView = itemView.findViewById(R.id.nombreMensaje)
    val mensaje: TextView = itemView.findViewById(R.id.mensajeMensaje)
    val hora: TextView = itemView.findViewById(R.id.horaMensaje)
    //val fotoMensajePerfil: CircleImageView = itemView.findViewById(R.id.fotoPerfilMensaje)
    val fotoMensaje: ImageView = itemView.findViewById(R.id.mensajeFoto)

    // No es necesario definir getters manualmente
}
