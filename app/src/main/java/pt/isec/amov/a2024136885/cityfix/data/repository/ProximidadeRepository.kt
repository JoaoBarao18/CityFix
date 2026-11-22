package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class ProximidadeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun registarEntrada(operadorId: String, ocorrenciaId: String, instante: Long) {
        check(auth.currentUser?.uid == operadorId)
        require(instante > 0)
        val perfil = firestore.collection("utilizadores").document(operadorId)
        val ref = firestore.collection("ocorrencias").document(ocorrenciaId)

        firestore.runTransaction { transacao ->
            val utilizador = transacao.get(perfil)
            check(utilizador.getString("perfil") == "operador" && utilizador.getBoolean("validado") == true)
            val ocorrencia = transacao.get(ref)
            if (ocorrencia.getString("estado") == "em_resolucao" &&
                instante > (ocorrencia.getLong("proximidadeEm") ?: 0L)
            ) {
                transacao.update(ref, "proximidadeEm", instante)
            }
        }.await()
    }
}
