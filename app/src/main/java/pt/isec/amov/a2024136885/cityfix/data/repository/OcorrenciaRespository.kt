package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia

internal class OcorrenciaRespository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun prepararCriacao(ocorrencia: Ocorrencia): Boolean {
        check(auth.currentUser?.uid == ocorrencia.autorId)
        val doc = firestore.collection("ocorrencias").document(ocorrencia.id).get(Source.SERVER).await()
        if (doc.exists()) {
            check(doc.getString("autorId") == ocorrencia.autorId)
            return false
        }

        val perfil = firestore.collection("utilizadores").document(ocorrencia.autorId)
            .get(Source.SERVER).await()
        check(perfil.getString("perfil") == "municipe" && perfil.getBoolean("validado") == true)
        val categoria = firestore.collection("categorias").document(ocorrencia.categoriaId)
            .get(Source.SERVER).await()
        check(categoria.getBoolean("ativa") == true)
        return true
    }

    suspend fun criar(ocorrencia: Ocorrencia) {
        require(ocorrencia.id.isNotBlank())
        require(ocorrencia.autorId.isNotBlank() && auth.currentUser?.uid == ocorrencia.autorId)
        require(ocorrencia.ficheiros.isNotEmpty())

        val doc = firestore.collection("ocorrencias").document(ocorrencia.id)

        firestore.runTransaction { transacao ->
            val guardada = transacao.get(doc)
            if (guardada.exists()) {
                check(guardada.getString("autorId") == ocorrencia.autorId)
            } else {
                transacao.set(doc, ocorrencia)
            }
        }.await()
    }
}
