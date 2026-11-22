package pt.isec.amov.a2024136885.cityfix.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.isec.amov.a2024136885.cityfix.MainActivity
import pt.isec.amov.a2024136885.cityfix.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "cityfix_auth_channel"
        const val CHANNEL_NAME = "Autenticação e Segurança"
        const val NOTIFICATION_ID = 1001
        private const val PROXIMIDADE_CHANNEL_ID = "cityfix_proximidade"
        private const val PROXIMIDADE_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de segurança e códigos de autenticação (MFA)"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showMfaCodeNotification(code: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Usa o ícone que preferires
            .setContentTitle("CityFix - Código de Segurança")
            .setContentText("O seu código de verificação é: $code")
            .setStyle(NotificationCompat.BigTextStyle().bigText("O seu código de verificação é: $code. Válido por 5 minutos."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun mostrarProximidadeSeNova(uid: String, ocorrenciaId: String, instante: Long) {
        val preferencias = context.getSharedPreferences("alertasProximidade", Context.MODE_PRIVATE)
        val chave = "ultimo:$uid:$ocorrenciaId"
        if (instante > preferencias.getLong(chave, 0L) && mostrarProximidade(ocorrenciaId, instante)) {
            preferencias.edit().putLong(chave, instante).apply()
        }
    }

    internal fun mostrarProximidade(ocorrenciaId: String, instante: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                PROXIMIDADE_CHANNEL_ID,
                context.getString(R.string.proximity_channel),
                NotificationManager.IMPORTANCE_HIGH
            ))
        }
        val abrirApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acao = PendingIntent.getActivity(context, 0, abrirApp, PendingIntent.FLAG_IMMUTABLE)
        val aviso = NotificationCompat.Builder(context, PROXIMIDADE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.proximity_title))
            .setContentText(context.getString(R.string.proximity_notification))
            .setContentIntent(acao)
            .setWhen(instante)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        manager.notify(ocorrenciaId, PROXIMIDADE_NOTIFICATION_ID, aviso)
        return true
    }
}