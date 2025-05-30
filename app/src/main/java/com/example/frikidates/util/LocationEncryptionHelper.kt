package com.example.frikidates.util

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import android.location.Location
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

object LocationEncryptionHelper {
    private const val TAG = "LocationEncryptionHelper"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val SECRET = "FrikidatesSecret2025"
    private val key: SecretKeySpec by lazy { generateKey() }

    private fun generateKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(SECRET.toByteArray(Charsets.UTF_8))
        Log.d(TAG, "Clave generada para encriptación")
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptLocation(lat: Double, lon: Double): String {
        Log.d(TAG, "Encriptando ubicación: lat=$lat, lon=$lon")
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            Log.e(TAG, "Coordenadas inválidas: lat=$lat, lon=$lon")
            throw IllegalArgumentException("Coordenadas fuera de rango")
        }
        val plainText = "$lat,$lon"
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)
            Log.d(TAG, "Ubicación encriptada: $encoded")
            return encoded
        } catch (e: Exception) {
            Log.e(TAG, "Error encriptando ubicación: ${e.message}", e)
            throw e
        }
    }

    fun decryptLocation(encryptedData: String?, profileId: String? = null): Pair<Double, Double>? {
        val logPrefix = if (profileId != null) "[$profileId] " else ""
        Log.d(TAG, "${logPrefix}Intentando desencriptar: $encryptedData")
        if (encryptedData.isNullOrEmpty()) {
            Log.w(TAG, "${logPrefix}Datos cifrados vacíos o nulos")
            return null
        }

        try {
            val combined = try {
                Base64.decode(encryptedData, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${logPrefix}Error decodificando Base64: ${e.message}")
                return null
            }

            if (combined.size <= 16) {
                Log.e(TAG, "${logPrefix}Datos cifrados demasiado cortos: ${combined.size} bytes")
                return null
            }

            val iv = combined.copyOfRange(0, 16)
            val encryptedBytes = combined.copyOfRange(16, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val decryptedBytes = try {
                cipher.doFinal(encryptedBytes)
            } catch (e: javax.crypto.BadPaddingException) {
                Log.e(TAG, "${logPrefix}BadPaddingException al desencriptar: ${e.message}")
                return null
            } catch (e: javax.crypto.IllegalBlockSizeException) {
                Log.e(TAG, "${logPrefix}IllegalBlockSizeException al desencriptar: ${e.message}")
                return null
            }

            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            Log.d(TAG, "${logPrefix}Desencriptado: $decryptedString")

            val parts = decryptedString.split(",")
            if (parts.size != 2) {
                Log.e(TAG, "${logPrefix}Formato inválido: $decryptedString")
                return null
            }

            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()
            if (lat == null || lon == null) {
                Log.e(TAG, "${logPrefix}Coordenadas inválidas: lat=$lat, lon=$lon")
                return null
            }

            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                Log.e(TAG, "${logPrefix}Coordenadas fuera de rango: lat=$lat, lon=$lon")
                return null
            }

            Log.d(TAG, "${logPrefix}Ubicación desencriptada: lat=$lat, lon=$lon")
            return Pair(lat, lon)
        } catch (e: Exception) {
            Log.e(TAG, "${logPrefix}Excepción general desencriptando: ${e.message}", e)
            return null
        }
    }

    fun hasLocationChangedSignificantly(
        oldEncryptedLocation: String,
        newLat: Double,
        newLon: Double,
        thresholdMeters: Float = 100f
    ): Boolean {
        Log.d(TAG, "Verificando cambio de ubicación: old=$oldEncryptedLocation, newLat=$newLat, newLon=$newLon")
        if (newLat !in -90.0..90.0 || newLon !in -180.0..180.0) {
            Log.w(TAG, "Coordenadas nuevas inválidas: lat=$newLat, lon=$newLon")
            return true
        }

        val decrypted = decryptLocation(oldEncryptedLocation) ?: run {
            Log.w(TAG, "No se pudo desencriptar ubicación antigua")
            return true
        }
        val (oldLat, oldLon) = decrypted

        val results = FloatArray(1)
        Location.distanceBetween(oldLat, oldLon, newLat, newLon, results)
        val changed = results[0] > thresholdMeters
        Log.d(TAG, "Distancia: ${results[0]}m, cambió: $changed")
        return changed
    }
}