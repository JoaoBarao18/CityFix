package pt.isec.amov.a2024136885.cityfix.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.MediaAttachment
import java.io.File

class MediaUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val mediaPath = inputData.getString(KEY_MEDIA_PATH) ?: return Result.failure()
        val thumbPath = inputData.getString(KEY_THUMB_PATH)
        val tipo = inputData.getString(KEY_TIPO) ?: "foto"
        val contentType = inputData.getString(KEY_CONTENT_TYPE) ?: if (tipo == "video") "video/mp4" else "image/jpeg"

        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            Log.e(TAG, "Ficheiro local não encontrado: $mediaPath")
            return Result.failure()
        }

        return try {
            // 1. Upload do ficheiro principal
            val mainStoragePath = "ocorrencias/$ticketId/${mediaFile.name}"
            val mainStorageRef = storage.reference.child(mainStoragePath)

            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()

            mainStorageRef.putFile(Uri.fromFile(mediaFile), metadata).await()
            val mainDownloadUrl = mainStorageRef.downloadUrl.await().toString()

            // 2. Upload da thumbnail (se existir)
            var thumbStoragePath: String? = null
            var thumbFileGuardado: File? = null
            if (!thumbPath.isNullOrBlank()) {
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) {
                    val caminhoThumb = "ocorrencias/$ticketId/${thumbFile.name}"
                    val thumbStorageRef = storage.reference.child(caminhoThumb)
                    val thumbMetadata = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build()
                    thumbStorageRef.putFile(Uri.fromFile(thumbFile), thumbMetadata).await()
                    thumbStoragePath = caminhoThumb
                    thumbFileGuardado = thumbFile
                }
            }

            // 3. Atualizar o documento da ocorrência no Firestore
            val attachment = MediaAttachment(
                tipo = tipo,
                storagePath = mainStoragePath,
                contentType = contentType,
                thumbnailPath = thumbStoragePath
            )

            firestore.collection("ocorrencias")
                .document(ticketId)
                .update(
                    "ficheiros", FieldValue.arrayUnion(attachment.toMap()),
                    "atualizadoEm", com.google.firebase.Timestamp.now()
                ).await()

            mediaFile.delete()
            thumbFileGuardado?.delete()
            Log.d(TAG, "Upload concluído com sucesso para o ticket: $ticketId")
            Result.success(workDataOf(KEY_DOWNLOAD_URL to mainDownloadUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Erro no upload de multimédia", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    // Função utilitária para converter o modelo para mapa do Firestore
    private fun MediaAttachment.toMap(): Map<String, Any?> {
        return mapOf(
            "tipo" to tipo,
            "storagePath" to storagePath,
            "contentType" to contentType,
            "thumbnailPath" to thumbnailPath
        )
    }

    companion object {
        const val TAG = "MediaUploadWorker"
        const val KEY_TICKET_ID = "ticket_id"
        const val KEY_MEDIA_PATH = "media_path"
        const val KEY_THUMB_PATH = "thumb_path"
        const val KEY_TIPO = "tipo"
        const val KEY_CONTENT_TYPE = "content_type"
        const val KEY_DOWNLOAD_URL = "download_url"
    }
}