package pt.isec.amov.a2024136885.cityfix.domain.location

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import pt.isec.amov.a2024136885.cityfix.data.repository.ProximidadeRepository

internal class EnviarEntradaProximidadeWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val operadorId = inputData.getString("operadorId") ?: return Result.failure()
        val ocorrenciaId = inputData.getString("ocorrenciaId") ?: return Result.failure()
        val instante = inputData.getLong("instante", 0L).takeIf { it > 0 } ?: return Result.failure()

        return try {
            ProximidadeRepository().registarEntrada(operadorId, ocorrenciaId, instante)
            Result.success()
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Log.w("CityFixGeofence", "Falha ao comunicar a entrada na zona", erro)
            val temporario = erro is FirebaseNetworkException ||
                (erro is FirebaseFirestoreException && erro.code in listOf(
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                    FirebaseFirestoreException.Code.ABORTED
                ))
            if (temporario && runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun pedido(operadorId: String, ocorrenciaId: String, instante: Long) =
            OneTimeWorkRequestBuilder<EnviarEntradaProximidadeWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(
                    "operadorId" to operadorId,
                    "ocorrenciaId" to ocorrenciaId,
                    "instante" to instante
                ))
                .build()
    }
}
