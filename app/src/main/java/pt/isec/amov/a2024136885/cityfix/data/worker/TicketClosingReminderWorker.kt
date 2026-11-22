package pt.isec.amov.a2024136885.cityfix.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.model.toUserSafe
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.temParticipante
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper
import java.util.concurrent.TimeUnit

class TicketClosingReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val uid = inputData.getString(KEY_UID) ?: return Result.failure()
        val resolucao = inputData.getLong(KEY_RESOLUCAO, 0)
        if (resolucao <= 0) return Result.failure()
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()

        val fimPrazo = resolucao + OcorrenciaRepository.PRAZO_REABERTURA_MS
        val agora = System.currentTimeMillis()
        return try {
            if (inputData.getBoolean(KEY_ENCERRAR, false)) {
                if (agora < fimPrazo) return Result.retry()
                val encerrada = OcorrenciaRepository().verificarEEncerrarSeExpirado(ticketId, uid, resolucao)
                if (encerrada) NotificationHelper(applicationContext).showTicketStatusNotification(
                    ticketId,
                    OcorrenciaRepository.ESTADO_ENCERRADO
                )
            } else if (agora < fimPrazo) {
                if (agora < fimPrazo - ANTECEDENCIA_MS) return Result.retry()
                val firestore = FirebaseFirestore.getInstance()
                val perfil = firestore.collection("utilizadores").document(uid).get(Source.SERVER).await().toUserSafe()
                if (perfil?.perfil != "municipe" || !perfil.validado) return Result.success()
                val documento = firestore.collection("ocorrencias").document(ticketId).get(Source.SERVER).await()
                val ticket = documento.takeIf { it.exists() }?.ocorrencia()
                if (ticket?.estado == OcorrenciaRepository.ESTADO_RESOLVIDO && ticket.temParticipante(uid) &&
                    ticket.resolvidoEm?.toDate()?.time == resolucao && FirebaseAuth.getInstance().currentUser?.uid == uid
                ) {
                    val avisos = applicationContext.getSharedPreferences("avisosFecho", Context.MODE_PRIVATE)
                    val chaveAviso = "$uid:$ticketId"
                    if (avisos.getLong(chaveAviso, 0) < resolucao) {
                        NotificationHelper(applicationContext).show24HoursClosingAlert(ticketId)
                        avisos.edit().putLong(chaveAviso, resolucao).apply()
                    }
                }
            }
            Result.success()
        } catch (erro: CancellationException) {
            throw erro
        } catch (erro: FirebaseFirestoreException) {
            when (erro.code) {
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.UNAVAILABLE -> Result.retry()
                else -> Result.failure()
            }
        }
    }

    companion object {
        const val KEY_TICKET_ID = "reminder_ticket_id"
        private const val KEY_UID = "uid"
        private const val KEY_RESOLUCAO = "resolvidoEmMs"
        private const val KEY_ENCERRAR = "encerrar"
        private const val ANTECEDENCIA_MS = 24L * 60 * 60 * 1000

        internal fun sincronizarPrazos(context: Context, uid: String, ocorrencias: List<Ocorrencia>) {
            val workManager = WorkManager.getInstance(context)
            val agora = System.currentTimeMillis()
            for (ocorrencia in ocorrencias) {
                val resolucao = ocorrencia.resolvidoEm?.toDate()?.time ?: continue
                if (ocorrencia.estado != OcorrenciaRepository.ESTADO_RESOLVIDO ||
                    !ocorrencia.temParticipante(uid)
                ) continue

                val fimPrazo = resolucao + OcorrenciaRepository.PRAZO_REABERTURA_MS
                if (agora < fimPrazo) {
                    agendar(workManager, uid, ocorrencia.id, resolucao, fimPrazo - ANTECEDENCIA_MS - agora, false)
                }
                agendar(workManager, uid, ocorrencia.id, resolucao, fimPrazo - agora, true)
            }
        }

        private fun agendar(
            workManager: WorkManager,
            uid: String,
            id: String,
            resolucao: Long,
            espera: Long,
            encerrar: Boolean
        ) {
            val pedido = OneTimeWorkRequestBuilder<TicketClosingReminderWorker>()
                .setInitialDelay(espera.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(
                    workDataOf(
                        KEY_TICKET_ID to id,
                        KEY_UID to uid,
                        KEY_RESOLUCAO to resolucao,
                        KEY_ENCERRAR to encerrar
                    )
                )
                .build()
            workManager.enqueueUniqueWork(
                "prazo-ocorrencia:$uid:$id:$resolucao:$encerrar",
                ExistingWorkPolicy.KEEP,
                pedido
            )
        }
    }
}
