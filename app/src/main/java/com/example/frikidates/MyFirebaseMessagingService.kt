package com.example.frikidates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.frikidates.firebase.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Manejar notificación recibida
        remoteMessage.notification?.let {
            val title = it.title ?: "Frikidates"
            val body = it.body ?: "Nueva notificación"
            sendNotification(title, body, remoteMessage.data)
        }

        // Manejar datos personalizados (data payload)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Frikidates"
            val body = remoteMessage.data["body"] ?: "Mensaje personalizado"
            sendNotification(title, body, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Guardar el token en Firestore
        val userId = FirebaseRepository.getCurrentUserId()
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("profiles")
                .document("profile_$userId")
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token guardado para profile_$userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Error guardando token: ${e.message}", e)
                }
        } else {
            Log.w("FCM", "Usuario no autenticado, no se guardó el token")
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val channelId = "frikidates_channel"
        val notificationId = System.currentTimeMillis().toInt()

        // Intent para abrir ChatsActivity
        val intent = Intent(this, ChatsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("matchedUserId", data["matchedUserId"])
            putExtra("matchId", data["matchId"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Frikidates Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de Frikidates"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Construir notificación
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Mostrar notificación
        with(NotificationManagerCompat.from(this)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e("FCM", "Permiso de notificaciones no concedido: ${e.message}")
            }
        }
    }
}