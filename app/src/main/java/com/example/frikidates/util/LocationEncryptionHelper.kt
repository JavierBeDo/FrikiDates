package com.example.frikidates.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.location.Location

object LocationEncryptionHelper {
    private const val KEY_ALIAS = "LocationKey"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        try {
            // Si ya existe, intentamos recuperarla
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val key = keyStore.getKey(KEY_ALIAS, null)
                if (key is SecretKey) {
                    return key
                } else {
                    // La clave no es válida, la eliminamos
                    keyStore.deleteEntry(KEY_ALIAS)
                }
            }
        } catch (e: Exception) {
            // Algo salió mal al acceder a la clave, la eliminamos por precaución
            e.printStackTrace()
            keyStore.deleteEntry(KEY_ALIAS)
        }

        // Generamos una nueva clave si no existe o si hubo error
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
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
        val plainText = "$lat,$lon"
        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val combined = iv + encryptedBytes
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decryptLocation(encryptedData: String): Pair<Double, Double> {
        try {
            if (!isKeyValid()) {
                // La clave ya no existe → no se puede desencriptar
                throw IllegalStateException("La clave de encriptación no existe")
            }

            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            if (combined.size <= 16) throw IllegalArgumentException("Datos cifrados muy cortos.")

            val iv = combined.copyOfRange(0, 16)
            val encryptedBytes = combined.copyOfRange(16, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedString = String(decryptedBytes)

            val parts = decryptedString.split(",")
            if (parts.size != 2) throw IllegalArgumentException("Formato inválido: $decryptedString")

            val lat = parts[0].toDoubleOrNull() ?: throw NumberFormatException("Latitud inválida.")
            val lon = parts[1].toDoubleOrNull() ?: throw NumberFormatException("Longitud inválida.")

            return Pair(lat, lon)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(0.0, 0.0) // Devolver coordenadas inválidas para indicar fallo
        }
    }


    fun hasLocationChangedSignificantly(
        oldEncryptedLocation: String,
        newLat: Double,
        newLon: Double
    ): Boolean {
        val (oldLat, oldLon) = decryptLocation(oldEncryptedLocation)
        // Evita comparar con 0.0 si la desencriptación falló
        if (oldLat == 0.0 && oldLon == 0.0) return true

        val results = FloatArray(1)
        Location.distanceBetween(oldLat, oldLon, newLat, newLon, results)
        return results[0] > 100
    }

    fun isKeyValid(): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.containsAlias(KEY_ALIAS)
    }
}