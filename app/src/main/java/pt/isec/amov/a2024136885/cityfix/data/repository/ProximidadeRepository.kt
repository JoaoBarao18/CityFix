package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

internal class ProximidadeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun registarEntrada(operadorId: String, ocorrenciaId: String, instante: Long) {
        check(auth.currentUser?.uid == operadorId)
        require(instante > 0)
        val entrada = Timestamp(Date(instante))
        val perfil = firestore.collection("utilizadores").document(operadorId)
        val ref = firestore.collection("ocorrencias").document(ocorrenciaId)

        firestore.runTransaction { transacao ->
            val utilizador = transacao.get(perfil)
            check(utilizador.getString("perfil") == "operador" && utilizador.getBoolean("validado") == true)
            val ocorrencia = transacao.get(ref)
            val entradaAnterior = ocorrencia.timestampOcorrencia("proximidadeEm")
            if (ocorrencia.getString("estado") == "em_resolucao" &&
                (entradaAnterior == null || entrada > entradaAnterior)
            ) {
                transacao.update(ref, "proximidadeEm", entrada)
            }
        }.await()
    }
}
