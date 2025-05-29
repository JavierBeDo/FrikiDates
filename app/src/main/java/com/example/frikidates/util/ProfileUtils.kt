package com.example.frikidates.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ProfileUtils {
    fun calculateAge(birthdate: String?): Int {
        if (birthdate.isNullOrEmpty()) return 0
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birth = sdf.parse(birthdate) ?: return 0
            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply { time = birth }
            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) {
            0
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371.0 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return radioTierra * c
    }

    fun calculateCompatibility(userInterests: List<String>, profileInterests: List<String>): Int {
        if (userInterests.isEmpty() || profileInterests.isEmpty()) return 0
        val common = userInterests.intersect(profileInterests.toSet()).size
        val totalUnique = userInterests.size + profileInterests.size - common
        return if (totalUnique > 0) (common * 100) / totalUnique else 0
    }
}