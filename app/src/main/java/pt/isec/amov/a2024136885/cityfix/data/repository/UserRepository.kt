package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
     * Obtém a lista de munícipes pendentes de validação (exclusivo para Operadores).
     */
    suspend fun getPendingMunicipes(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("utilizadores")
                .whereEqualTo("perfil", "municipe")
                .whereEqualTo("validado", false)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Valida um munícipe através do NIF, registando o operador responsável e timestamp.
     */
    suspend fun validateMunicipe(municipeUid: String, operadorUid: String): Result<Unit> {
        return try {
            firestore.collection("utilizadores")
                .document(municipeUid)
                .update(
                    mapOf(
                        "validado" to true,
                        "validadoEm" to System.currentTimeMillis(),
                        "validadoPor" to operadorUid
                    )
                ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}