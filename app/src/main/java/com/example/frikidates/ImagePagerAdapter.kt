package com.example.frikidates

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePagerAdapter(
    private val context: Context, // Necesitarás el contexto para Glide
    private val imageUrls: List<String> // Cambiado de List<Int> a List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Asumiendo que tu R.layout.item_image tiene un ImageView con id "image"
        val imageView: ImageView = view.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        // Asegúrate que R.layout.item_image es un layout simple con solo un ImageView
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (imageUrls.isNotEmpty()) {
            val imageUrl = imageUrls[position]
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.landscapeplaceholder) // Opcional: crea este drawable
                .error(R.drawable.error_image)         // Opcional: crea este drawable
                .into(holder.imageView)
        } else {
            // Si la lista está vacía, podrías mostrar un placeholder por defecto
            Glide.with(context)
                .load(R.drawable.landscapeplaceholder) // Carga un placeholder general
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int {
        // Si la lista de URLs está vacía, aún podríamos querer mostrar una "página" con el placeholder.
        return if (imageUrls.isEmpty()) 1 else imageUrls.size
    }
}
