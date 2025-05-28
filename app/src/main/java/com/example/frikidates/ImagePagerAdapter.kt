package com.example.frikidates

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

class ImagePagerAdapter(
    private val context: Context,
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        if (imageUrls.isNotEmpty()) {
            val imageUrl = imageUrls[position]
            Glide.with(context)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .fitCenter()
                )
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ImagePagerAdapter", "Failed to load image: $imageUrl, error: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(holder.imageView)
        } else {
            Glide.with(context)
                .load(R.drawable.placeholder_image)
                .fitCenter()
                .into(holder.imageView)
        }
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(context).clear(holder.imageView)
    }

    override fun getItemCount(): Int = if (imageUrls.isEmpty()) 1 else imageUrls.size
}