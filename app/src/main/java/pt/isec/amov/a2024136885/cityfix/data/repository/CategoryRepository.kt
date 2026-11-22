package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Category

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val categoriesCollection = firestore.collection("categorias")

    /**
     * Escuta em tempo real todas as categorias (para o painel do Operador).
     */
    fun getAllCategoriesFlow(): Flow<Result<List<Category>>> = callbackFlow {
        val listener = categoriesCollection
            .orderBy("nome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(list))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Escuta apenas as categorias ativas (para o Munícipe ao criar ocorrência).
     */
    fun getActiveCategoriesFlow(): Flow<Result<List<Category>>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("ativa", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(list))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Cria uma nova tipologia.
     */
    suspend fun createCategory(nome: String): Result<Unit> {
        return try {
            val docRef = categoriesCollection.document()
            val category = Category(
                id = docRef.id,
                nome = nome.trim(),
                ativa = true,
                criadaEm = null // @ServerTimestamp preenche no servidor
            )
            docRef.set(category).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Edita o nome de uma tipologia existente.
     */
    suspend fun updateCategoryName(id: String, novoNome: String): Result<Unit> {
        return try {
            categoriesCollection.document(id)
                .update(
                    mapOf(
                        "nome" to novoNome.trim(),
                        "atualizadaEm" to Timestamp.now()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Alterna o estado ativo/inativo da tipologia.
     */
    suspend fun toggleCategoryStatus(id: String, novoEstado: Boolean): Result<Unit> {
        return try {
            categoriesCollection.document(id)
                .update(
                    mapOf(
                        "ativa" to novoEstado,
                        "atualizadaEm" to Timestamp.now()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}