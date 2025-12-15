package com.developersbeeh.pharmaflow.features.notifications

import android.util.Log
import com.developersbeeh.pharmaflow.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    /**
     * Chamado quando um novo token é gerado (ex: primeira instalação).
     * Em um app real, enviaríamos isso para o Firestore (User.fcmToken)
     * para poder enviar notificação para uma pessoa específica.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo token gerado: $token")
        // Todo: Salvar token no perfil do usuário no Firestore
    }

    /**
     * Chamado quando uma mensagem chega e o app está em PRIMEIRO PLANO.
     * Se o app estiver em SEGUNDO PLANO, o Android gerencia automaticamente a notificação de sistema.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Verifica se é uma Notificação (título/corpo padrão)
        remoteMessage.notification?.let {
            val title = it.title ?: "PharmaFlow"
            val body = it.body ?: "Nova mensagem recebida."
            notificationHelper.showNotification(title, body)
        }

        // 2. Verifica se é uma Mensagem de Dados (Customizada)
        if (remoteMessage.data.isNotEmpty()) {
            val customTitle = remoteMessage.data["title"]
            val customBody = remoteMessage.data["body"]
            val type = remoteMessage.data["type"] // ex: "promo"

            if (!customTitle.isNullOrEmpty() && !customBody.isNullOrEmpty()) {
                notificationHelper.showNotification(
                    title = customTitle,
                    message = customBody,
                    isPromo = type == "promo"
                )
            }
        }
    }
}