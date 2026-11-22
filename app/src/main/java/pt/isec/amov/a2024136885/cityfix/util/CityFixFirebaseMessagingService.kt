package pt.isec.amov.a2024136885.cityfix.util

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CityFixFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM gerado: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensagem Push (FCM) recebida: ${remoteMessage.data}")

        val notificationHelper = NotificationHelper(applicationContext)

        val ticketId = remoteMessage.data["ticketId"] ?: "0"
        val novoEstado = remoteMessage.data["novoEstado"]

        if (novoEstado != null) {
            notificationHelper.showTicketStatusNotification(ticketId, novoEstado)
        } else {
            remoteMessage.notification?.let { notification ->
                val corpo = notification.body ?: ""
                notificationHelper.showTicketStatusNotification(ticketId, corpo)
            }
        }
    }

    companion object {
        private const val TAG = "CityFixFCM"
    }
}
