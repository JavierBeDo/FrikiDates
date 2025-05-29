package com.example.frikidates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Manejar notificación recibida
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Frikidates", it.body ?: "Nueva notificación")
        }

        // Manejar datos personalizados (si usas data payload)
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "Frikidates"
            val body = remoteMessage.data["body"] ?: "Mensaje personalizado"
            sendNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Enviar el token a tu servidor si necesitas enviar notificaciones específicas
        // Ejemplo: FirebaseRepository.saveFCMToken(token)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "frikidates_channel"
        val notificationId = 1001

        // Intent para abrir PerfilActivity al tocar la notificación
        val intent = Intent(this, PerfilActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear canal de notificación (requerido para Android 8.0+)
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
            .setSmallIcon(R.drawable.icon) // Crea este ícono en res/drawable
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Mostrar notificación
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MyFirebaseMessagingService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, builder.build())
            } else {
                // Solicitar permiso en la actividad (ver paso 3)
            }
        }
    }
}