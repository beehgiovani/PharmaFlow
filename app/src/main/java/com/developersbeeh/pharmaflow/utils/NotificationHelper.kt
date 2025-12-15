package com.developersbeeh.pharmaflow.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.developersbeeh.pharmaflow.MainActivity
import com.developersbeeh.pharmaflow.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_ORDERS = "orders_channel"
        const val CHANNEL_NAME_ORDERS = "Atualizações de Pedidos"
        const val CHANNEL_ID_PROMOS = "promos_channel"
        const val CHANNEL_NAME_PROMOS = "Ofertas e Promoções"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val orderChannel = NotificationChannel(
                CHANNEL_ID_ORDERS,
                CHANNEL_NAME_ORDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sobre o status dos seus pedidos"
                enableVibration(true)
            }

            val promoChannel = NotificationChannel(
                CHANNEL_ID_PROMOS,
                CHANNEL_NAME_PROMOS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Novidades e ofertas especiais"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(orderChannel)
            manager.createNotificationChannel(promoChannel)
        }
    }

    fun showNotification(title: String, message: String, isPromo: Boolean = false) {
        val channelId = if (isPromo) CHANNEL_ID_PROMOS else CHANNEL_ID_ORDERS

        // Intent para abrir o App ao clicar na notificação
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Ícone da notificação
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Fecha ao clicar

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Usa o tempo atual como ID único para não sobrescrever notificações anteriores
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}