package pt.isec.amov.a2024136885.cityfix.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await as aguardarOperacao
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import pt.isec.amov.a2024136885.cityfix.data.model.MediaAttachment
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.util.ImageCompressor
import java.io.File
import java.util.Date

internal class CriarOcorrenciaWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("ocorrenciaId") ?: return@withContext Result.failure()
        val autorId = inputData.getString("autorId") ?: return@withContext Result.failure()
        val ficheiroPedido = ficheiroPedido(applicationContext, id)

        try {
            val repositorio = OcorrenciaRespository()
            val submetida = repositorio.procurarSubmissao(autorId, id)
            val idGuardado = submetida ?: run {
                val pedido = jsonPedido.decodeFromString<PedidoCriacao>(ficheiroPedido.readText())
                val ocorrencia = pedido.ocorrencia
                check(ocorrencia.autorId == autorId && ocorrencia.id == id)
                repositorio.prepararCriacao(ocorrencia)

                val foto = pedido.fotoPath?.let(::ficheiroLocal)
                val video = pedido.videoPath?.let(::ficheiroLocal)
                require(foto != null || video != null)
                val pasta = if (ocorrencia.publica) {
                    "$PASTA_ANEXOS_PUBLICOS/$id"
                } else "ocorrencias/$autorId/$id/anexos"
                val anexos = buildList {
                    foto?.let { add(enviarFoto(it, pasta, id)) }
                    video?.let { add(enviarVideo(it, pasta)) }
                }
                repositorio.criar(ocorrencia.copy(ficheiros = anexos))
            }
            ficheiroPedido.delete()
            Result.success(workDataOf("agrupada" to (idGuardado != id)))
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Log.w("CriarOcorrencia", "Falha ao enviar a ocorrência", erro)
            val podeRepetir = when (erro) {
                is StorageException -> erro.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED
                is FirebaseFirestoreException -> erro.code in listOf(
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                    FirebaseFirestoreException.Code.ABORTED
                )
                else -> false
            }
            if (podeRepetir && runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun ficheiroLocal(caminho: String): File {
        val ficheiro = File(caminho).canonicalFile
        require(ficheiro.parentFile == applicationContext.filesDir.canonicalFile)
        require(ficheiro.isFile && ficheiro.length() > 0)
        return ficheiro
    }

    private suspend fun enviarFoto(foto: File, pasta: String, id: String): MediaAttachment {
        val ficheiros = ImageCompressor.compressAndGenerateThumbnail(
            applicationContext,
            Uri.fromFile(foto),
            id
        ).getOrThrow()
        val raiz = FirebaseStorage.getInstance().reference
        val caminho = "$pasta/foto.jpg"
        val caminhoMiniatura = "$pasta/miniatura.jpg"
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        try {
            check(ficheiros.mainFile.length() > 0 && ficheiros.thumbnailFile.length() > 0)
            raiz.child(caminho).putFile(Uri.fromFile(ficheiros.mainFile), metadata).aguardarEnvio()
            raiz.child(caminhoMiniatura).putFile(Uri.fromFile(ficheiros.thumbnailFile), metadata).aguardarEnvio()
            return MediaAttachment("foto", caminho, "image/jpeg", caminhoMiniatura)
        } finally {
            ficheiros.mainFile.delete()
            ficheiros.thumbnailFile.delete()
        }
    }

    private suspend fun enviarVideo(video: File, pasta: String): MediaAttachment {
        val caminho = "$pasta/${video.name}"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(video.extension.lowercase())
            ?.takeIf { it.startsWith("video/") } ?: "video/mp4"
        val metadata = StorageMetadata.Builder().setContentType(mime).build()
        FirebaseStorage.getInstance().reference.child(caminho)
            .putFile(Uri.fromFile(video), metadata).aguardarEnvio()
        return MediaAttachment("video", caminho, mime)
    }

    private suspend fun UploadTask.aguardarEnvio() {
        try {
            await()
        } catch (cancelamento: CancellationException) {
            cancel()
            throw cancelamento
        }
    }

    @Serializable
    private data class PedidoCriacao(
        val ocorrencia: Ocorrencia,
        val fotoPath: String?,
        val videoPath: String?
    )

    companion object {
        private val jsonPedido = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(Timestamp::class, TimestampPedidoSerializer)
            }
        }

        fun nome(id: String) = "criar-ocorrencia:$id"

        suspend fun agendar(
            context: Context,
            ocorrencia: Ocorrencia,
            fotoPath: String?,
            videoPath: String?
        ) {
            // O ficheiro privado permite recuperar o pedido sem pôr a descrição no limite de 10 KB do WorkManager.
            withContext(Dispatchers.IO) {
                val ficheiro = ficheiroPedido(context, ocorrencia.id)
                ficheiro.parentFile?.mkdirs()
                ficheiro.writeText(jsonPedido.encodeToString(PedidoCriacao(ocorrencia, fotoPath, videoPath)))
            }
            val trabalho = OneTimeWorkRequestBuilder<CriarOcorrenciaWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf("ocorrenciaId" to ocorrencia.id, "autorId" to ocorrencia.autorId))
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(nome(ocorrencia.id), ExistingWorkPolicy.KEEP, trabalho)
                .aguardarOperacao()
        }

        private fun ficheiroPedido(context: Context, id: String) =
            File(context.filesDir, "ocorrencias-pendentes/$id.json")
    }
}


// Os pedidos pendentes de versões anteriores guardavam as datas em milissegundos.
private object TimestampPedidoSerializer : KSerializer<Timestamp> {
    override val descriptor = PrimitiveSerialDescriptor("TimestampPedido", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeLong(value.toDate().time)
    }

    override fun deserialize(decoder: Decoder) = Timestamp(Date(decoder.decodeLong()))
}
