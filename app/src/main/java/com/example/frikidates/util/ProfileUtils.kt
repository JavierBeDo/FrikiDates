package com.example.frikidates.util

import java.util.Calendar

object ProfileUtils {
    fun calcularEdad(fechaNacimiento: String): Int {
        return try {
            val partes = fechaNacimiento.split("/") // Espera "dd/MM/yyyy"
            if (partes.size != 3) return 0

            val anio = partes[2].toInt()
            val mes = partes[1].toInt()
            val dia = partes[0].toInt()

            val hoy = Calendar.getInstance()
            val nacimiento = Calendar.getInstance()
            nacimiento.set(anio, mes - 1, dia)

            var edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)

            if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
                edad--
            }

            edad
        } catch (e: Exception) {
            0
        }
    }

    fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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