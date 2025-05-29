package com.example.frikidates.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.location.Location

object LocationEncryptionHelper {
    private const val KEY_ALIAS = "LocationKey"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val TAG = "LocationEncryptionHelper"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val key = keyStore.getKey(KEY_ALIAS, null)
                if (key is SecretKey) {
                    Log.d(TAG, "Clave existente recuperada: $KEY_ALIAS")
                    return key
                } else {
                    Log.w(TAG, "Clave no válida, eliminando: $KEY_ALIAS")
                    keyStore.deleteEntry(KEY_ALIAS)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accediendo a la clave: ${e.message}", e)
            keyStore.deleteEntry(KEY_ALIAS)
        }

        Log.d(TAG, "Generando nueva clave: $KEY_ALIAS")
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun encryptLocation(lat: Double, lon: Double): String {
        Log.d(TAG, "Encriptando ubicación: lat=$lat, lon=$lon")
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            Log.e(TAG, "Coordenadas inválidas: lat=$lat, lon=$lon")
            throw IllegalArgumentException("Coordenadas fuera de rango")
        }
        val plainText = "$lat,$lon"
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())
            val combined = iv + encryptedBytes
            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)
            Log.d(TAG, "Ubicación encriptada: $encoded")
            return encoded
        } catch (e: Exception) {
            Log.e(TAG, "Error encriptando ubicación: ${e.message}", e)
            throw e
        }
    }

    fun decryptLocation(encryptedData: String?): Pair<Double, Double>? {
        Log.d(TAG, "Intentando desencriptar: $encryptedData")
        if (encryptedData.isNullOrEmpty()) {
            Log.w(TAG, "Datos cifrados vacíos o nulos")
            return null
        }

        try {
            if (!isKeyValid()) {
                Log.e(TAG, "Clave de encriptación no existe")
                return null
            }

            val combined = try {
                Base64.decode(encryptedData, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error decodificando Base64: ${e.message}")
                return null
            }

            if (combined.size <= 16) {
                Log.e(TAG, "Datos cifrados demasiado cortos: ${combined.size} bytes")
                return null
            }

            val iv = combined.copyOfRange(0, 16)
            val encryptedBytes = combined.copyOfRange(16, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            try {
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando Cipher: ${e.message}")
                return null
            }

            val decryptedBytes = try {
                cipher.doFinal(encryptedBytes)
            } catch (e: javax.crypto.BadPaddingException) {
                Log.e(TAG, "BadPaddingException al desencriptar: ${e.message}")
                return null
            } catch (e: javax.crypto.IllegalBlockSizeException) {
                Log.e(TAG, "IllegalBlockSizeException al desencriptar: ${e.message}")
                return null
            } catch (e: Exception) {
                Log.e(TAG, "Error en doFinal: ${e.message}", e)
                return null
            }

            val decryptedString = String(decryptedBytes)
            Log.d(TAG, "Desencriptado: $decryptedString")

            val parts = decryptedString.split(",")
            if (parts.size != 2) {
                Log.e(TAG, "Formato inválido: $decryptedString")
                return null
            }

            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()
            if (lat == null || lon == null) {
                Log.e(TAG, "Coordenadas inválidas: lat=$lat, lon=$lon")
                return null
            }

            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                Log.e(TAG, "Coordenadas fuera de rango: lat=$lat, lon=$lon")
                return null
            }

            Log.d(TAG, "Ubicación desencriptada: lat=$lat, lon=$lon")
            return Pair(lat, lon)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción general desencriptando: ${e.message}", e)
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

    fun isKeyValid(): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val valid = keyStore.containsAlias(KEY_ALIAS)
        Log.d(TAG, "Clave válida: $valid")
        return valid
    }

    fun resetKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
            Log.d(TAG, "Clave eliminada: $KEY_ALIAS")
        } else {
            Log.d(TAG, "No había clave para eliminar: $KEY_ALIAS")
        }
    }
}