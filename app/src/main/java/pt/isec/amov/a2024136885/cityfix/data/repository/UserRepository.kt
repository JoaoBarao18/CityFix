package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.User

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Atualização de perfil com regras estritas do enunciado:
     * - Munícipe: Nome, NIF e Georreferenciação NÃO podem ser alterados.
     * - Operador: Nome e NIF NÃO podem ser alterados.
     */
    suspend fun updateProfile(
        uid: String,
        isOperador: Boolean,
        novoTelefone: String,
        novaFotoUrl: String? = null,
        novaDetecaoImpactos: Boolean? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "telefone" to novoTelefone.trim()
            )

            if (novaFotoUrl != null) {
                updates["fotoUrl"] = novaFotoUrl
            }

            if (!isOperador && novaDetecaoImpactos != null) {
                updates["detecaoImpactos"] = novaDetecaoImpactos
            }

            firestore.collection("utilizadores")
                .document(uid)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escuta em tempo real os munícipes pendentes de validação.
     */
    fun getPendingMunicipesFlow(): Flow<Result<List<User>>> = callbackFlow {
        val listener = firestore.collection("utilizadores")
            .whereEqualTo("perfil", "municipe")
            .whereEqualTo("validado", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(Result.success(users))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Ativa o registo de um munícipe registando o operador e a data.
     */
    suspend fun validateMunicipe(municipeUid: String, operadorUid: String): Result<Unit> {
        return try {
            firestore.collection("utilizadores")
                .document(municipeUid)
                .update(
                    mapOf(
                        "validado" to true,
                        "validadoEm" to Timestamp.now(),
                        "validadoPor" to operadorUid
                    )
                ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}