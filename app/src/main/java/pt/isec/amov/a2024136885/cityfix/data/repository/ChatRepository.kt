package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Message
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

        val ticketRef = firestore.collection("ocorrencias").document(ticketId)
        val userRef = firestore.collection("utilizadores").document(uid)
        val messageRef = ticketRef.collection("mensagens").document()

        return try {
            firestore.runTransaction { transaction ->
                val ticket = transaction.get(ticketRef).ocorrencia()
                val user = transaction.get(userRef).toUserSafe()
                check(user?.validado == true)
                check(user.perfil == "operador" ||
                    (user.perfil == "municipe" && (ticket.autorId == uid || uid in ticket.seguidoresIds)))
                check(ticket.estado != OcorrenciaRepository.ESTADO_ENCERRADO && !ticket.prazoDecorrido())

                transaction.set(
                    messageRef,
                    Message(
                        id = messageRef.id,
                        ticketId = ticketId,
                        autorId = uid,
                        autorNome = user.nome,
                        perfil = user.perfil,
                        texto = texto.trim()
                    )
                )
                transaction.update(ticketRef, "atualizadoEm", FieldValue.serverTimestamp())
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
