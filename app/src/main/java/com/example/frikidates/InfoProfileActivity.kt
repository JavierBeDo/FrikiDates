package com.example.frikidates

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.frikidates.util.LocationEncryptionHelper
import com.example.frikidates.util.ProfileUtils
import me.relex.circleindicator.CircleIndicator3

class InfoProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_info)

        // Obtener datos del Intent
        val profile = IntentCompat.getParcelableExtra(intent, "PROFILE", Profile::class.java)
        val currentLat = intent.getDoubleExtra("CURRENT_LAT", 0.0)
        val currentLon = intent.getDoubleExtra("CURRENT_LON", 0.0)
        val currentUserInterests = intent.getStringArrayListExtra("CURRENT_INTERESTS") ?: emptyList<String>()

        if (profile == null) {
            Log.e("InfoProfileActivity", "No profile data received")
            finish()
            return
        }

        Log.d("InfoProfileActivity", "Profile interests: ${profile.interests}")
        Log.d("InfoProfileActivity", "Current user interests: $currentUserInterests")

        // Inicializar vistas
        val imagePager = findViewById<ViewPager2>(R.id.image_pager)
        val indicator = findViewById<CircleIndicator3>(R.id.image_indicator)
        val nameText = findViewById<TextView>(R.id.name_text)
        val ageText = findViewById<TextView>(R.id.age_text)
        val genderText = findViewById<TextView>(R.id.gender_text)
        val locationText = findViewById<TextView>(R.id.location_text)
        val interestsLabel = findViewById<TextView>(R.id.interests_label)
        val interestsContainer = findViewById<LinearLayout>(R.id.interests_container)
        val descriptionCard = findViewById<View>(R.id.description_card)
        val descText = findViewById<TextView>(R.id.desc_text)

        // Configurar galería de imágenes
        if (profile.images.isNotEmpty()) {
            val imageAdapter = ImagePagerAdapter(this, profile.images)
            imagePager.adapter = imageAdapter
            indicator.setViewPager(imagePager)
            indicator.visibility = if (profile.images.size > 1) View.VISIBLE else View.GONE
            imagePager.visibility = View.VISIBLE
        } else {
            imagePager.visibility = View.GONE
            indicator.visibility = View.GONE
        }

        // Establecer información del perfil
        nameText.text = profile.name
        try {
            val age = ProfileUtils.calculateAge(profile.birthdate)
            ageText.text = if (age > 0) "Edad: $age" else "Edad: N/D"
        } catch (e: Exception) {
            Log.e("InfoProfileActivity", "Error calculating age: ${e.message}")
            ageText.text = "Edad: N/D"
        }

        genderText.text = when (profile.gender.lowercase()) {
            "hombre", "masculino" -> "Género: Masculino \u2642"
            "mujer", "femenino" -> "Género: Femenino \u2640"
            "otro", "no binario", "no especificado" -> "Género: No binario \u26A7"
            else -> "Género: ?"
        }

        try {
            val decryptedLocation = LocationEncryptionHelper.decryptLocation(profile.encryptedLocation)
            if (decryptedLocation != null && currentLat != 0.0 && currentLon != 0.0) {
                val (lat, lon) = decryptedLocation
                val distancia = ProfileUtils.calculateDistance(currentLat, currentLon, lat, lon)
                locationText.text = "A ${"%.1f".format(distancia)} km de distancia"
            } else {
                locationText.text = "Ubicación: N/D"
            }
        } catch (e: Exception) {
            Log.e("InfoProfileActivity", "Error decrypting location: ${e.message}")
            locationText.text = "Ubicación: N/D"
        }

        // Configurar intereses comunes
        val commonInterests = profile.interests.map { it.lowercase() }
            .intersect(currentUserInterests.map { it.lowercase() }.toSet())
        Log.d("InfoProfileActivity", "Common interests: $commonInterests")

        if (commonInterests.isNotEmpty()) {
            interestsLabel.text = "Intereses en común"
            interestsContainer.visibility = View.VISIBLE
            // Dividir intereses en grupos de 3
            val chunkedInterests = commonInterests.chunked(3)
            chunkedInterests.forEach { chunk ->
                val rowLayout = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                chunk.forEach { interest ->
                    rowLayout.addView(createInterestTag(interest))
                }
                interestsContainer.addView(rowLayout)
            }
        } else {
            interestsLabel.text = "Ningún interés en común"
            interestsContainer.visibility = View.GONE
        }

        descriptionCard.visibility = View.GONE
    }

    private fun createInterestTag(text: String): TextView {
        val formattedText = text.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        return TextView(this).apply {
            this.text = formattedText
            textSize = 11f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(6.dp, 6.dp, 6.dp, 6.dp)
            setBackgroundResource(R.drawable.circle_background)
            layoutParams = LinearLayout.LayoutParams(92.dp, 44.dp).apply {
                marginEnd = 2.dp
            }
            gravity = Gravity.CENTER
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}