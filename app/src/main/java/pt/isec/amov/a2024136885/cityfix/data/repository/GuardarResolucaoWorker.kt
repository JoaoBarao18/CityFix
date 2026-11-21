package pt.isec.amov.a2024136885.cityfix.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await as aguardarOperacao
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.domain.location.removerGeofenceOcorrencia

internal class GuardarResolucaoWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val operadorId = inputData.getString("operadorId") ?: return Result.failure()
        val id = inputData.getString("ocorrenciaId") ?: return Result.failure()

        try {
            PainelOperacionalRepository().resolver(
                operadorId,
                id,
                inputData.getString("fotoPath"),
                inputData.getString("videoPath")
            )
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Log.w("PainelOperacional", "Falha ao enviar o comprovativo", erro)
            val podeRepetir = when (erro) {
                is StorageException -> erro.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED
                is FirebaseFirestoreException -> erro.code in listOf(
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                    FirebaseFirestoreException.Code.ABORTED
                )
                else -> false
            }
            return if (podeRepetir && runAttemptCount < 3) Result.retry() else Result.failure()
        }

        return try {
            removerGeofenceOcorrencia(applicationContext, id).await()
            Result.success()
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Log.w("PainelOperacional", "Resolução guardada, mas a geofence não foi removida", erro)
            Result.success(workDataOf("erroProximidade" to true))
        }
    }

    companion object {
        fun nome(operadorId: String, ocorrenciaId: String) =
            "resolucao:$operadorId:$ocorrenciaId"

        suspend fun agendar(
            context: Context,
            operadorId: String,
            id: String,
            fotoPath: String?,
            videoPath: String?
        ) {
            val pedido = OneTimeWorkRequestBuilder<GuardarResolucaoWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(
                    "operadorId" to operadorId,
                    "ocorrenciaId" to id,
                    "fotoPath" to fotoPath,
                    "videoPath" to videoPath
                ))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(nome(operadorId, id), ExistingWorkPolicy.KEEP, pedido)
                .aguardarOperacao()
        }
    }
}
