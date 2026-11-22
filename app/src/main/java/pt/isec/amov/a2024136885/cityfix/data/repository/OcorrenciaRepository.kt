package pt.isec.amov.a2024136885.cityfix.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.worker.MediaUploadWorker
import java.util.UUID

class OcorrenciaRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun criarOcorrencia(
        context: Context,
        categoriaId: String,
        situacao: String,
        descricao: String,
        publica: Boolean,
        latitude: Double,
        longitude: Double,
        fotoPath: String?,
        thumbPath: String?,
        videoPath: String?
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado"))
        val ticketId = UUID.randomUUID().toString()

        return try {
            val ocorrencia = Ocorrencia(
                id = ticketId,
                autorId = uid,
                categoriaId = categoriaId,
                situacao = situacao,
                descricao = descricao,
                publica = publica,
                latitude = latitude,
                longitude = longitude,
                estado = "aberto",
                criadoEm = System.currentTimeMillis(),
                atualizadoEm = System.currentTimeMillis()
            )

            firestore.collection("ocorrencias").document(ticketId).set(ocorrencia).await()

            // 2. Enfileirar o Worker para o Upload de Media
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Foto
            if (fotoPath != null) {
                val data = Data.Builder()
                    .putString(MediaUploadWorker.KEY_TICKET_ID, ticketId)
                    .putString(MediaUploadWorker.KEY_MEDIA_PATH, fotoPath)
                    .putString(MediaUploadWorker.KEY_THUMB_PATH, thumbPath)
                    .putString(MediaUploadWorker.KEY_TIPO, "foto")
                    .putString(MediaUploadWorker.KEY_CONTENT_TYPE, "image/jpeg")
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

                workManager.enqueue(workRequest)
            }

            // Video
            if (videoPath != null) {
                val data = Data.Builder()
                    .putString(MediaUploadWorker.KEY_TICKET_ID, ticketId)
                    .putString(MediaUploadWorker.KEY_MEDIA_PATH, videoPath)
                    .putString(MediaUploadWorker.KEY_TIPO, "video")
                    .putString(MediaUploadWorker.KEY_CONTENT_TYPE, "video/mp4")
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

                workManager.enqueue(workRequest)
            }

            Result.success(ticketId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
