package com.example.frikidates

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.frikidates.util.LocationEncryptionHelper
import com.example.frikidates.util.ProfileUtils
import me.relex.circleindicator.CircleIndicator3

class CardStackAdapter(
    private val context: Context,
    private var profiles: List<Profile>,
    private val currentUserInterests: List<String>,
    private val currentUserLatitude: Double,
    private val currentUserLongitude: Double
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

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
        val cardInfoContainer: LinearLayout = view.findViewById(R.id.card_info_container)
        val btnMoreInfo: ImageButton = view.findViewById(R.id.btn_more_info)
        var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

        init {
            val clickListener = View.OnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val profile = profiles.getOrNull(position) ?: return@OnClickListener
                    val intent = Intent(context, InfoProfileActivity::class.java).apply {
                        putExtra("PROFILE", profile)
                        putStringArrayListExtra("CURRENT_INTERESTS", ArrayList(currentUserInterests))
                        putExtra("CURRENT_LAT", currentUserLatitude)
                        putExtra("CURRENT_LON", currentUserLongitude)
                    }
                    context.startActivity(intent)
                }
            }
            cardInfoContainer.setOnClickListener(clickListener)
            btnMoreInfo.setOnClickListener(clickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    private fun normalizeDateFormat(date: String): String {
        try {
            val parts = date.split("/")
            if (parts.size != 3) return date
            val day = parts[0].padStart(2, '0')
            val month = parts[1].padStart(2, '0')
            val year = parts[2]
            return "$day/$month/$year"
        } catch (e: Exception) {
            return date
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles.getOrNull(position) ?: return
        Log.d("CardStackAdapter", "Binding profile at position $position: ${profile.name}")

        val imageUrls = profile.images
        val imageAdapter = ImagePagerAdapter(context, imageUrls)
        holder.imagePager.adapter = imageAdapter
        holder.indicator.setViewPager(holder.imagePager)
        holder.indicator.visibility = if (imageUrls.size > 1) View.VISIBLE else View.GONE

        holder.nameText.text = profile.name.takeIf { it.isNotBlank() } ?: "Sin nombre"
        try {
            val edad = ProfileUtils.calculateAge(normalizeDateFormat(profile.birthdate))
            holder.ageText.text = if (edad > 0) context.getString(R.string.age_format, edad) else "Edad: N/D"
        } catch (e: Exception) {
            Log.e("CardStackAdapter", "Error calculating age for ${profile.name}: ${e.message}", e)
            holder.ageText.text = "Edad: N/D"
        }

        holder.genderText.text = when (profile.gender.lowercase()) {
            "hombre", "masculino" -> "\u2642"
            "mujer", "femenino" -> "\u2640"
            "otro", "no binario", "no especificado" -> "\u26A7"
            else -> "?"
        }

        val compatibilityPercentage =  ProfileUtils.calculateCompatibility(currentUserInterests, profiles.flatMap { it.interests })
        holder.compatibilityText.text = when {
            compatibilityPercentage < 40 -> context.getString(R.string.compatibility_low)
            compatibilityPercentage in 40..69 -> context.getString(R.string.compatibility_medium)
            else -> context.getString(R.string.compatibility_high)
        }
        holder.compatibilityText.setTextColor(
            context.getColor(
                when {
                    compatibilityPercentage < 40 -> android.R.color.holo_red_dark
                    compatibilityPercentage in 40..69 -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_green_dark
                }
            )
        )

        try {
            val decryptedLocation = LocationEncryptionHelper.decryptLocation(profile.encryptedLocation)
            if (decryptedLocation != null && currentUserLatitude != 0.0 && currentUserLongitude != 0.0) {
                val (lat, lon) = decryptedLocation
                val distancia = ProfileUtils.calculateDistance(currentUserLatitude, currentUserLongitude, lat, lon)
                holder.locationText.text = context.getString(R.string.distance_format, "%.1f".format(distancia))
            } else {
                holder.locationText.text = context.getString(R.string.distance_unknown)
            }
        } catch (e: Exception) {
            Log.e("CardStackAdapter", "Error decrypting location for ${profile.name}: ${e.message}", e)
            holder.locationText.text = context.getString(R.string.distance_unknown)
        }

        fun updateImageNavigationButtonsVisibility() {
            val currentItem = holder.imagePager.currentItem
            val totalItems = holder.imagePager.adapter?.itemCount ?: 0
            holder.buttonPreviousImage.visibility =
                if (totalItems > 1 && currentItem > 0) View.VISIBLE else View.GONE
            holder.buttonNextImage.visibility =
                if (totalItems > 1 && currentItem < totalItems - 1) View.VISIBLE else View.GONE
        }

        updateImageNavigationButtonsVisibility()

        holder.pageChangeCallback?.let {
            holder.imagePager.unregisterOnPageChangeCallback(it)
        }

        val newCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pagePosition: Int) {
                updateImageNavigationButtonsVisibility()
            }
        }
        holder.pageChangeCallback = newCallback
        holder.imagePager.registerOnPageChangeCallback(newCallback)

        holder.buttonPreviousImage.setOnClickListener {
            val currentItem = holder.imagePager.currentItem
            if (currentItem > 0) {
                holder.imagePager.setCurrentItem(currentItem - 1, true)
            }
        }
        holder.buttonNextImage.setOnClickListener {
            val adapter = holder.imagePager.adapter
            if (adapter != null && holder.imagePager.currentItem < adapter.itemCount - 1) {
                holder.imagePager.setCurrentItem(holder.imagePager.currentItem + 1, true)
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.pageChangeCallback?.let {
            holder.imagePager.unregisterOnPageChangeCallback(it)
            holder.pageChangeCallback = null
        }
        holder.imagePager.adapter = null
    }

    override fun getItemCount(): Int = profiles.size
}