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
    private var profiles: List<Profile>,
    private val context: Context
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

        holder.indicator.setViewPager(holder.imagePager)
        holder.indicator.visibility = if (profile.images.size > 1) View.VISIBLE else View.GONE

        holder.nameText.text = profile.name
        holder.ageText.text = if (profile.age > 0)
            context.getString(R.string.age_with_value, profile.age)
        else
            context.getString(R.string.age_not_available)
        holder.genderText.text = profile.gender
        holder.locationText.text = profile.city
        holder.compatibilityText.text = profile.compatibility

        fun updateImageNavigationButtonsVisibility() {
            val currentItem = holder.imagePager.currentItem
            val totalItems = holder.imagePager.adapter?.itemCount ?: 0
            holder.buttonPreviousImage.visibility =
                if (totalItems > 1 && currentItem > 0) View.VISIBLE else View.GONE
            holder.buttonNextImage.visibility =
                if (totalItems > 1 && currentItem < totalItems - 1) View.VISIBLE else View.GONE
        }

        updateImageNavigationButtonsVisibility()

        // Limpia el callback anterior si existe
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
            val currentItem = holder.imagePager.currentItem
            val totalItems = holder.imagePager.adapter?.itemCount ?: 0
            if (currentItem < totalItems - 1) {
                holder.imagePager.setCurrentItem(currentItem + 1, true)
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.pageChangeCallback?.let {
            holder.imagePager.unregisterOnPageChangeCallback(it)
            holder.pageChangeCallback = null
        }
    }

    override fun getItemCount(): Int = profiles.size
}
