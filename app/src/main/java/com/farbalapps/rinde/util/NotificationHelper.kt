package com.farbalapps.rinde.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.farbalapps.rinde.R
import android.content.Intent
import android.app.PendingIntent
import com.farbalapps.rinde.MainActivity

object NotificationHelper {
    private const val POSTS_CHANNEL_ID = "community_posts_channel"

    private const val NEW_POSTS_CHANNEL_ID = "new_posts_alert_channel"
    private const val NEW_POSTS_NOTIF_ID = 9001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_posts_name)
            val descriptionText = context.getString(R.string.notif_channel_posts_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(POSTS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val newPostsChannel = NotificationChannel(
                NEW_POSTS_CHANNEL_ID,
                "Nuevas Ofertas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre nuevas ofertas publicadas"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(newPostsChannel)
        }
    }

    fun getPublishingNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, POSTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este icono existe o usa uno genérico
            .setContentTitle(context.getString(R.string.notif_publishing_title))
            .setContentText(context.getString(R.string.notif_publishing_desc))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    fun showSuccessNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, POSTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_post_success_title))
            .setContentText(context.getString(R.string.notif_post_success_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showErrorNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, POSTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_post_error_title))
            .setContentText(context.getString(R.string.notif_post_error_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showInAppNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, POSTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showNewPostsNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "community")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NEW_POSTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Nuevas publicaciones en Rinde!")
            .setContentText("Hay $count ofertas nuevas que no te puedes perder.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NEW_POSTS_NOTIF_ID, notification)
    }
}
