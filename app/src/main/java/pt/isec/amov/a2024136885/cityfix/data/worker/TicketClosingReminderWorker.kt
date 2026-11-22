package pt.isec.amov.a2024136885.cityfix.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper

class TicketClosingReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val firestore = FirebaseFirestore.getInstance()

        return try {
            val doc = firestore.collection("ocorrencias").document(ticketId).get().await()
            val ticket = doc.toObject(Ocorrencia::class.java)

            // Só dispara se o ticket ainda estiver 'resolvido' (não foi reaberto nem encerrado)
            if (ticket != null && ticket.estado == OcorrenciaRepository.ESTADO_RESOLVIDO) {
                val notificationHelper = NotificationHelper(appContext)
                notificationHelper.show24HoursClosingAlert(ticketId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_TICKET_ID = "reminder_ticket_id"
    }
}