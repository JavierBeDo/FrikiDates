package com.example.frikidates

import android.content.Context
import android.content.SharedPreferences

class LocationManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserLocationPrefs", Context.MODE_PRIVATE)

    fun saveLocation(latitude: Double, longitude: Double) {
        val editor = sharedPreferences.edit()
        editor.putFloat("latitude", latitude.toFloat())
        editor.putFloat("longitude", longitude.toFloat())
        editor.apply()
    }

    fun getLocation(): Pair<Double, Double>? {
        val latitude = sharedPreferences.getFloat("latitude", 0f).toDouble()
        val longitude = sharedPreferences.getFloat("longitude", 0f).toDouble()
        return if (latitude != 0.0 && longitude != 0.0) {
            Pair(latitude, longitude)
        } else {
            null
        }
    }
}