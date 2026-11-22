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
        const val CHANNEL_AUTH_ID = "cityfix_auth_channel"
        const val CHANNEL_TICKETS_ID = "cityfix_tickets_channel"
        const val CHANNEL_ALERTS_ID = "cityfix_alerts_channel"

        const val CHANNEL_AUTH_NAME = "Autenticação e Segurança"
        const val CHANNEL_TICKETS_NAME = "Atualizações de Ocorrências"
        const val CHANNEL_ALERTS_NAME = "Prazos e Alertas"
        const val NOTIFICATION_ID = 1001
        private const val PROXIMIDADE_CHANNEL_ID = "cityfix_proximidade"
        private const val PROXIMIDADE_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val authChannel = NotificationChannel(
                CHANNEL_AUTH_ID,
                CHANNEL_AUTH_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Códigos de validação MFA"
            }

            val ticketsChannel = NotificationChannel(
                CHANNEL_TICKETS_ID,
                CHANNEL_TICKETS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mudança de estados e respostas às ocorrências"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                CHANNEL_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de prazos de encerramento de ocorrências"
            }

            notificationManager.createNotificationChannels(listOf(authChannel, ticketsChannel, alertsChannel))
        }
    }

    private fun canPostNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * Notificação do código MFA da Sprint 1
     */
    fun showMfaCodeNotification(code: String) {
        if (!canPostNotification()) return

        val builder = NotificationCompat.Builder(context, CHANNEL_AUTH_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CityFix - Código de Validação")
            .setContentText("O seu código de acesso é: $code")
            .setStyle(NotificationCompat.BigTextStyle().bigText("O seu código de acesso de duplo fator (MFA) é: $code. Válido por 5 minutos."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getContentIntent())

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build())
        } catch (e: SecurityException) {
            // Permissão rejeitada ou revogada silenciosamente
        }
    }

    /**
     * Notificação de mudança de estado da ocorrência
     */
    fun showTicketStatusNotification(ticketId: String, novoEstado: String) {
        if (!canPostNotification()) return

        val estadoFormatado = when (novoEstado) {
            "pedido_informacoes" -> "Pedido de Informações Adicionais"
            "em_resolucao" -> "Em Resolução pela Equipa Técnica"
            "resolvido" -> "Marcada como Resolvida"
            "encerrado" -> "Encerrada Definitivamente"
            else -> "Atualizada para $novoEstado"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TICKETS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CityFix - Atualização de Ocorrência")
            .setContentText("A sua ocorrência está agora: $estadoFormatado")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A ocorrência #${ticketId.take(8)} mudou de estado para: $estadoFormatado."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getContentIntent())

        val notificationId = ticketId.hashCode()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Ignorar se a permissão não existir
        }
    }

    /**
     * Alerta de 24 horas restantes antes do fecho permanente
     */
    fun show24HoursClosingAlert(ticketId: String) {
        if (!canPostNotification()) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aviso de Encerramento (24h restantes)")
            .setContentText("Resta 1 dia para contestar a resolução da ocorrência #${ticketId.take(8)}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A sua ocorrência #${ticketId.take(8)} foi marcada como resolvida há 4 dias. Se o problema persistir, reabra-a nas próximas 24 horas antes do fecho definitivo."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getContentIntent())

        val notificationId = ("alert_$ticketId").hashCode()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Ignorar se a permissão não existir
        }
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