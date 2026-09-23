package com.example.hiit.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.hiit.MainActivity
import com.example.hiit.R

/** Notificaciones transitorias de aviso (distintas de la persistente del HIIT). */
object Notifier {

    private const val CHANNEL_ID = "hiit_alerts"
    const val NOTIFICATION_ID_PUBLIC = 1001

    /**
     * Muestra un aviso transitorio que se retira solo pasados [timeoutMs]
     * (por defecto unos segundos), para no tapar la app ni quedarse en la
     * bandeja. El aviso del fin de sesión dura algo más.
     */
    fun show(context: Context, message: String, timeoutMs: Long = 4_000) {
        createChannel(context)

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setTimeoutAfter(timeoutMs)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PUBLIC, builder.build())
        } catch (_: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS: no se muestra, pero el TTS sigue sonando
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
