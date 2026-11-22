package pt.isec.amov.a2024136885.cityfix.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Message
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.worker.MediaUploadWorker
import pt.isec.amov.a2024136885.cityfix.data.worker.TicketClosingReminderWorker
import java.util.UUID
import java.util.concurrent.TimeUnit

class OcorrenciaRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val ocorrenciasCollection = firestore.collection("ocorrencias")

    companion object {
        const val PRAZO_REABERTURA_MS = 5L * 24 * 60 * 60 * 1000 // 5 dias em milissegundos
        const val ESTADO_ABERTO = "aberto"
        const val ESTADO_PEDIDO_INFO = "pedido_informacoes"
        const val ESTADO_EM_RESOLUCAO = "em_resolucao"
        const val ESTADO_RESOLVIDO = "resolvido"
        const val ESTADO_ENCERRADO = "encerrado"
    }

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
                estado = ESTADO_ABERTO,
                criadoEm = null // @ServerTimestamp preenche no Firestore
            )

            firestore.collection("ocorrencias").document(ticketId).set(ocorrencia).await()

            // Registo inicial no histórico de estados
            adicionarEntradaHistorico(
                ticketId = ticketId,
                estadoNovo = ESTADO_ABERTO,
                autorUid = uid,
                nota = "Criação da ocorrência"
            )

            // Enfileirar Worker para Upload de Media
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

            // Vídeo
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

    /**
     * Transição de estado acionada pelo Operador Municipal.
     */
    suspend fun atualizarEstadoOperador(
        ticketId: String,
        novoEstado: String,
        nota: String? = null,
        context: Context? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado"))

        return try {
            val docRef = ocorrenciasCollection.document(ticketId)
            val snapshot = docRef.get().await()
            val ocorrencia = snapshot.toObject(Ocorrencia::class.java)
                ?: return Result.failure(Exception("Ocorrência não encontrada"))

            if (ocorrencia.estado == ESTADO_ENCERRADO) {
                return Result.failure(Exception("Ocorrências encerradas não podem ser alteradas."))
            }

            val updates = mutableMapOf<String, Any>(
                "estado" to novoEstado,
                "atualizadoEm" to Timestamp.now()
            )

            if (novoEstado == ESTADO_RESOLVIDO) {
                updates["resolvidoEm"] = Timestamp.now()
            } else if (novoEstado == ESTADO_ENCERRADO) {
                updates["encerradoEm"] = Timestamp.now()
            }

            docRef.update(updates).await()
            adicionarEntradaHistorico(ticketId, novoEstado, uid, nota)

            // Agendar lembrete de fecho se transitou para RESOLVIDO
            if (novoEstado == ESTADO_RESOLVIDO) {
                val ctx = context ?: this.context
                if (ctx != null) {
                    try {
                        val workManager = WorkManager.getInstance(ctx)
                        val data = Data.Builder()
                            .putString(TicketClosingReminderWorker.KEY_TICKET_ID, ticketId)
                            .build()

                        val workRequest = OneTimeWorkRequestBuilder<TicketClosingReminderWorker>()
                            .setInitialDelay(4, TimeUnit.DAYS)
                            .setInputData(data)
                            .addTag("reminder_closing_$ticketId")
                            .build()

                        workManager.enqueue(workRequest)
                    } catch (e: Exception) {
                        // Ignorar falha no agendamento do Worker
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reabertura da ocorrência pelo Munícipe (autor ou seguidor).
     * Exige obrigatoriamente estar dentro do prazo de 5 dias após 'resolvido' e conter justificação.
     */
    suspend fun reabrirOcorrencia(
        ticketId: String,
        justificacao: String,
        context: Context? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado"))

        if (justificacao.trim().length < 10) {
            return Result.failure(Exception("A justificação de reabertura deve ter pelo menos 10 caracteres."))
        }

        return try {
            val docRef = ocorrenciasCollection.document(ticketId)
            val snapshot = docRef.get().await()
            val ocorrencia = snapshot.toObject(Ocorrencia::class.java)
                ?: return Result.failure(Exception("Ocorrência não encontrada"))

            // 1. Validar estado atual
            if (ocorrencia.estado != ESTADO_RESOLVIDO) {
                return Result.failure(Exception("Apenas ocorrências no estado 'Resolvido' podem ser reabertas."))
            }

            // 2. Validar o prazo limite de 5 dias
            val resolvidoEm = ocorrencia.resolvidoEm
                ?: return Result.failure(Exception("Data de resolução inválida."))

            val tempoDecorrido = System.currentTimeMillis() - resolvidoEm.toDate().time
            if (tempoDecorrido > PRAZO_REABERTURA_MS) {
                // Se excedeu o prazo, bloqueia e marca como encerrado
                docRef.update(
                    mapOf(
                        "estado" to ESTADO_ENCERRADO,
                        "encerradoEm" to Timestamp.now(),
                        "atualizadoEm" to Timestamp.now()
                    )
                ).await()
                return Result.failure(Exception("O prazo de 5 dias para contestar esta resolução expirou. A ocorrência foi encerrada."))
            }

            // 3. Executar a reabertura
            docRef.update(
                mapOf(
                    "estado" to ESTADO_ABERTO,
                    "resolvidoEm" to FieldValue.delete(),
                    "atualizadoEm" to Timestamp.now()
                )
            ).await()

            adicionarEntradaHistorico(
                ticketId = ticketId,
                estadoNovo = ESTADO_ABERTO,
                autorUid = uid,
                nota = "Reaberta pelo munícipe: $justificacao"
            )

            // Cancelar lembrete de fecho pendente
            val ctx = context ?: this.context
            if (ctx != null) {
                try {
                    WorkManager.getInstance(ctx).cancelAllWorkByTag("reminder_closing_$ticketId")
                } catch (e: Exception) {
                    // Ignorar falha no cancelamento do Worker
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica e encerra a ocorrência se os 5 dias tiverem passado sem contestação.
     */
    suspend fun verificarEEncerrarSeExpirado(ticketId: String): Result<Boolean> {
        return try {
            val docRef = ocorrenciasCollection.document(ticketId)
            val snapshot = docRef.get().await()
            val ocorrencia = snapshot.toObject(Ocorrencia::class.java) ?: return Result.success(false)

            if (ocorrencia.estado == ESTADO_RESOLVIDO && ocorrencia.resolvidoEm != null) {
                val tempoDecorrido = System.currentTimeMillis() - ocorrencia.resolvidoEm.toDate().time
                if (tempoDecorrido > PRAZO_REABERTURA_MS) {
                    docRef.update(
                        mapOf(
                            "estado" to ESTADO_ENCERRADO,
                            "encerradoEm" to Timestamp.now(),
                            "atualizadoEm" to Timestamp.now()
                        )
                    ).await()
                    adicionarEntradaHistorico(ticketId, ESTADO_ENCERRADO, "sistema", "Encerramento automático (prazo de 5 dias decorrido)")
                    return Result.success(true)
                }
            }
            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOcorrenciasByAutorFlow(autorUid: String): Flow<Result<List<Ocorrencia>>> = callbackFlow {
        val listener = ocorrenciasCollection
            .whereEqualTo("autorId", autorUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(list))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Escuta em tempo real uma ocorrência específica.
     */
    fun getOcorrenciaFlow(ticketId: String): Flow<Result<Ocorrencia?>> = callbackFlow {
        val listener = ocorrenciasCollection.document(ticketId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val ocorrencia = snapshot?.toObject(Ocorrencia::class.java)?.copy(id = snapshot.id)
                trySend(Result.success(ocorrencia))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Subcoleção de Chat - Enviar nova mensagem.
     */
    suspend fun enviarMensagem(
        ticketId: String,
        remetenteNome: String,
        perfil: String,
        texto: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado"))
        
        if (texto.isBlank()) {
            return Result.failure(Exception("A mensagem não pode estar vazia."))
        }

        return try {
            val docRef = ocorrenciasCollection.document(ticketId)
            val snapshot = docRef.get().await()
            val ocorrencia = snapshot.toObject(Ocorrencia::class.java)
                ?: return Result.failure(Exception("Ocorrência não encontrada"))

            if (ocorrencia.estado == ESTADO_ENCERRADO) {
                return Result.failure(Exception("Não é possível enviar mensagens numa ocorrência encerrada."))
            }

            val msgRef = docRef.collection("mensagens").document()
            val mensagem = Message(
                id = msgRef.id,
                ticketId = ticketId,
                autorId = uid,
                autorNome = remetenteNome,
                perfil = perfil,
                texto = texto.trim(),
                enviadoEm = null // @ServerTimestamp resolve o preenchimento no servidor
            )

            msgRef.set(mensagem).await()
            
            // Atualizar data de modificação do ticket
            docRef.update("atualizadoEm", Timestamp.now()).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Subcoleção de Chat - Escuta em tempo real.
     */
    fun getMensagensFlow(ticketId: String): Flow<Result<List<Message>>> = callbackFlow {
        val listener = ocorrenciasCollection.document(ticketId)
            .collection("mensagens")
            .orderBy("enviadoEm", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val mensagens = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(Result.success(mensagens))
            }

        awaitClose { listener.remove() }
    }

    private suspend fun adicionarEntradaHistorico(
        ticketId: String,
        estadoNovo: String,
        autorUid: String,
        nota: String?
    ) {
        val entrada = mapOf(
            "estado" to estadoNovo,
            "autorUid" to autorUid,
            "data" to Timestamp.now(),
            "nota" to (nota ?: "")
        )

        ocorrenciasCollection.document(ticketId)
            .update("historicoEstados", FieldValue.arrayUnion(entrada))
            .await()
    }
}