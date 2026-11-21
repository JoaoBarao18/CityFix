package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Category

internal class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun listarAtivas(): List<Category> =
        firestore.collection("categorias")
            .whereEqualTo("ativa", true)
            .get()
            .await()
            .documents.mapNotNull { documento ->
                documento.toObject(Category::class.java)?.copy(id = documento.id)
            }
}
