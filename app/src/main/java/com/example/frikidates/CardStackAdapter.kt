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

class   CardStackAdapter(
    private var profiles: List<Profile>,
    private val c: Context
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

        val imageAdapter = ImagePagerAdapter(c, profile.images)
        holder.imagePager.adapter = imageAdapter
        // ... (código del ViewPager e indicador igual) ...
        holder.indicator.setViewPager(holder.imagePager)
        holder.indicator.visibility = if (profile.images.size > 1) View.VISIBLE else View.GONE


        holder.nameText.text = profile.name

        // << CAMBIO: Usar profile.age directamente
        holder.ageText.text = if (profile.age > 0)
            c.getString(R.string.age_with_value, profile.age)
        else
            c.getString(R.string.age_not_available)

        holder.genderText.text = profile.gender
        holder.locationText.text = profile.city
        holder.compatibilityText.text = profile.compatibility

        // ... (resto del código de onBindViewHolder para botones y callback se mantiene igual) ...
        fun updateImageNavigationButtonsVisibility() {
            val currentItem = holder.imagePager.currentItem
            val totalItems = holder.imagePager.adapter?.itemCount ?: 0
            holder.buttonPreviousImage.visibility = if (totalItems > 1 && currentItem > 0) View.VISIBLE else View.GONE
            holder.buttonNextImage.visibility = if (totalItems > 1 && currentItem < totalItems - 1) View.VISIBLE else View.GONE
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

}