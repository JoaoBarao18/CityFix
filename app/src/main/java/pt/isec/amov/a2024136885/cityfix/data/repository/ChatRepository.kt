package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Message
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.model.User
import pt.isec.amov.a2024136885.cityfix.data.model.toUserSafe

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Escuta em tempo real as mensagens associadas a uma ocorrência específica.
     */
    fun getMessagesFlow(ticketId: String): Flow<Result<List<Message>>> = callbackFlow {
        val messagesRef = firestore.collection("ocorrencias")
            .document(ticketId)
            .collection("mensagens")
            .orderBy("enviadoEm", Query.Direction.ASCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(Result.success(list))
        }

        awaitClose { listener.remove() }
    }

    /**
     * Envia uma mensagem no chat da ocorrência.
     */
    suspend fun sendMessage(ticketId: String, texto: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Utilizador não autenticado."))
        val uid = currentUser.uid

        if (texto.isBlank()) {
            return Result.failure(Exception("A mensagem não pode estar vazia."))
        }

        return try {
            // 1. Validar se a ocorrência está encerrada
            val ticketDoc = firestore.collection("ocorrencias").document(ticketId).get().await()
            val ticket = ticketDoc.toObject(Ocorrencia::class.java)
                ?: return Result.failure(Exception("Ocorrência não encontrada."))

            if (ticket.estado == OcorrenciaRepository.ESTADO_ENCERRADO) {
                return Result.failure(Exception("Esta ocorrência está encerrada. Não é possível enviar mais mensagens."))
            }

            // 2. Obter dados do perfil do remetente
            val userDoc = firestore.collection("utilizadores").document(uid).get().await()
            val userProfile = userDoc.toUserSafe()
            val autorNome = userProfile?.nome?.ifBlank { "Utilizador" } ?: "Utilizador"
            val perfil = userProfile?.perfil ?: "municipe"

            // 3. Gravar na subcoleção de mensagens
            val msgCollection = firestore.collection("ocorrencias")
                .document(ticketId)
                .collection("mensagens")

            val docRef = msgCollection.document()
            val message = Message(
                id = docRef.id,
                ticketId = ticketId,
                autorId = uid,
                autorNome = autorNome,
                perfil = perfil,
                texto = texto.trim(),
                enviadoEm = null // @ServerTimestamp preenche no Firestore
            )

            docRef.set(message).await()

            // 4. Atualizar o timestamp de atividade da ocorrência
            firestore.collection("ocorrencias").document(ticketId)
                .update("atualizadoEm", com.google.firebase.Timestamp.now())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}