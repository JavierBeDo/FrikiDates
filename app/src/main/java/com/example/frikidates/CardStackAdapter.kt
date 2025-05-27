package com.example.frikidates

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import me.relex.circleindicator.CircleIndicator3

class CardStackAdapter(
    private val context: Context,
    private var profiles: List<Profile>,
    private val currentUserInterests: List<String>,
    private val currentUserLatitude: Double,
    private val currentUserLongitude: Double
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    // ViewHolder se mantiene igual...
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagePager: ViewPager2 = view.findViewById(R.id.image_pager)
        val indicator: CircleIndicator3 = view.findViewById(R.id.image_indicator)
        val nameText: TextView = view.findViewById(R.id.name_text)
        val ageText: TextView = view.findViewById(R.id.age_text)
        val genderText: TextView = view.findViewById(R.id.gender_text)
        val locationText: TextView = view.findViewById(R.id.location_text)
        val compatibilityText: TextView = view.findViewById(R.id.compatibility_text)
        val buttonPreviousImage: ImageButton = view.findViewById(R.id.button_previous_image)
        val buttonNextImage: ImageButton = view.findViewById(R.id.button_next_image)
        var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]

        val imageAdapter = ImagePagerAdapter(context, profile.images)
        holder.imagePager.adapter = imageAdapter
        // ... (código del ViewPager e indicador igual) ...
        holder.indicator.setViewPager(holder.imagePager)
        holder.indicator.visibility = if (profile.images.size > 1) View.VISIBLE else View.GONE


        holder.nameText.text = profile.name

        val edad = calcularEdad(profile.birthdate)
        holder.ageText.text = if (edad > 0) "Edad: $edad" else "Edad: N/D"
        holder.genderText.text = when (profile.gender.lowercase()) {
            "hombre", "masculino" -> "\u2642" // ♂
            "mujer", "femenino" -> "\u2640"   // ♀
            "otro", "no binario", "no especificado" -> "\u26A7" // ⚧
            else -> "?" // desconocido
        }
        val commonInterests = profile.interests.intersect(currentUserInterests.toSet())
        val compatibilityPercentage = if (currentUserInterests.isNotEmpty()) {
            (commonInterests.size * 100) / currentUserInterests.size
        } else 0

        when {
            compatibilityPercentage < 40 -> {
                holder.compatibilityText.text = "Poco compatible"
                holder.compatibilityText.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }

            compatibilityPercentage in 40..69 -> {
                holder.compatibilityText.text = "Compatible"
                holder.compatibilityText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            }

            else -> {
                holder.compatibilityText.text = "Muy compatible"
                holder.compatibilityText.setTextColor(context.getColor(android.R.color.holo_green_dark))
            }
        }

        // Supongamos que tienes la ubicación del usuario actual en estas variables:
        val latUsuarioActual = currentUserLatitude
        val lonUsuarioActual = currentUserLongitude

        // Parsear ubicación del perfil
        val ubicacion = profile.encryptedLocation.split(",")
        if (ubicacion.size == 2) {
            val lat = ubicacion[0].toDoubleOrNull()
            val lon = ubicacion[1].toDoubleOrNull()
            if (lat != null && lon != null) {
                val distancia = calcularDistancia(latUsuarioActual, lonUsuarioActual, lat, lon)
                holder.locationText.text = "A ${"%.1f".format(distancia)} km de distancia"
            } else {
                holder.locationText.text = "Distancia: N/D"
            }
        } else {
            holder.locationText.text = "Distancia: N/D"
        }


        // ... (resto del código de onBindViewHolder para botones y callback se mantiene igual) ...
        fun updateImageNavigationButtonsVisibility() {
            val currentItem = holder.imagePager.currentItem
            val totalItems = holder.imagePager.adapter?.itemCount ?: 0
            holder.buttonPreviousImage.visibility =
                if (totalItems > 1 && currentItem > 0) View.VISIBLE else View.GONE
            holder.buttonNextImage.visibility =
                if (totalItems > 1 && currentItem < totalItems - 1) View.VISIBLE else View.GONE
        }

        updateImageNavigationButtonsVisibility()

        holder.pageChangeCallback?.let { holder.imagePager.unregisterOnPageChangeCallback(it) }
        holder.pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pagePosition: Int) {
                super.onPageSelected(pagePosition)
                updateImageNavigationButtonsVisibility()
            }
        }
        holder.imagePager.registerOnPageChangeCallback(holder.pageChangeCallback!!)

        holder.buttonPreviousImage.setOnClickListener {
            if (holder.imagePager.currentItem > 0) {
                holder.imagePager.setCurrentItem(holder.imagePager.currentItem - 1, true)
            }
        }

        holder.buttonNextImage.setOnClickListener {
            holder.imagePager.adapter?.let { adapter ->
                if (holder.imagePager.currentItem < adapter.itemCount - 1) {
                    holder.imagePager.setCurrentItem(holder.imagePager.currentItem + 1, true)
                }
            }
        }
    }

    // ... (onViewRecycled y getItemCount se mantienen igual) ...
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.pageChangeCallback?.let {
            holder.imagePager.unregisterOnPageChangeCallback(it)
        }
    }

    override fun getItemCount(): Int = profiles.size

    //TODO quitar esto de aqui
    private fun calcularEdad(fechaNacimiento: String): Int {
        return try {
            val partes = fechaNacimiento.split("/") // Espera "dd/MM/yyyy"
            if (partes.size != 3) return 0

            val anio = partes[2].toInt()
            val mes = partes[1].toInt()
            val dia = partes[0].toInt()

            val hoy = java.util.Calendar.getInstance()
            val nacimiento = java.util.Calendar.getInstance()
            nacimiento.set(anio, mes - 1, dia)

            var edad = hoy.get(java.util.Calendar.YEAR) - nacimiento.get(java.util.Calendar.YEAR)

            if (hoy.get(java.util.Calendar.DAY_OF_YEAR) < nacimiento.get(java.util.Calendar.DAY_OF_YEAR)) {
                edad--
            }

            edad
        } catch (e: Exception) {
            0 // Si hay error, retorna 0
        }
    }

    private fun calcularDistancia(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radioTierra = 6371.0 // Radio de la Tierra en km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return radioTierra * c
    }

}