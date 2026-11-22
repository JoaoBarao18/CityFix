package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

internal data class AnexoPublicado(
    val tipo: String,
    val caminho: String,
    val miniatura: String?
)

internal data class OcorrenciaPublicada(
    val id: String,
    val categoria: String,
    val situacao: String,
    val estado: String,
    val atualizadoEm: Long,
    val latitudeAproximada: Double?,
    val longitudeAproximada: Double?,
    val anexos: List<AnexoPublicado>
) {
    val miniatura get() = anexos.firstOrNull { it.tipo == "foto" }?.let { it.miniatura ?: it.caminho }
}

internal data class EstatisticasPublicas(
    val total: Long,
    val ativas: Long,
    val reparadas: Long,
    val duracaoTotalMs: Long
) {
    val mediaDias get() = if (reparadas > 0) duracaoTotalMs.toDouble() / reparadas / 86_400_000 else null
    val fracaoAtiva get() = if (total > 0) ativas.toDouble() / total else 0.0
}

internal data class DadosPainelPublico(
    val ocorrencias: List<OcorrenciaPublicada>,
    val estatisticas: EstatisticasPublicas
)

internal class PainelPublicoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    fun observar() = combine(observarOcorrencias(), observarEstatisticas(), ::DadosPainelPublico)

    suspend fun urlAnexo(caminho: String): String {
        require(eAnexoPublico(caminho))
        return storage.reference.child(caminho).downloadUrl.await().toString()
    }

    private fun observarOcorrencias() = callbackFlow {
        val listener = firestore.collection("ocorrenciasPublicas").addSnapshotListener { snapshot, erro ->
            if (erro != null) close(erro)
            else if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.ocorrenciaPublicada() }
                    .sortedByDescending { it.atualizadoEm })
            }
        }
        awaitClose(listener::remove)
    }

    private fun observarEstatisticas() = callbackFlow {
        val listener = firestore.document("estatisticasPublicas/painel").addSnapshotListener { snapshot, erro ->
            if (erro != null) close(erro)
            else if (snapshot != null) {
                trySend(EstatisticasPublicas(
                    total = snapshot.getLong("total") ?: 0,
                    ativas = snapshot.getLong("ativas") ?: 0,
                    reparadas = snapshot.getLong("reparadas") ?: 0,
                    duracaoTotalMs = snapshot.getLong("duracaoTotalMs") ?: 0
                ))
            }
        }
        awaitClose(listener::remove)
    }
}

private fun DocumentSnapshot.ocorrenciaPublicada(): OcorrenciaPublicada? {
    val estado = getString("estado") ?: return null
    if (estado !in estadosAtivosOcorrencia) return null

    val anexos = (get("anexos") as? List<*>).orEmpty().mapNotNull { valor ->
        val anexo = valor as? Map<*, *> ?: return@mapNotNull null
        val tipo = anexo["tipo"] as? String ?: return@mapNotNull null
        if (tipo !in listOf("foto", "video")) return@mapNotNull null
        val caminho = (anexo["storagePath"] as? String)?.takeIf { eAnexoPublico(it) }
            ?: return@mapNotNull null
        val miniatura = (anexo["thumbnailPath"] as? String)?.takeIf { eAnexoPublico(it) }
        AnexoPublicado(tipo, caminho, miniatura)
    }
    return OcorrenciaPublicada(
        id = id,
        categoria = getString("categoria").orEmpty(),
        situacao = getString("situacao").orEmpty(),
        estado = estado,
        atualizadoEm = when (val atualizado = get("atualizadoEm")) {
            is Timestamp -> atualizado.toDate().time
            is Number -> atualizado.toLong()
            else -> 0L
        },
        latitudeAproximada = getDouble("latitudeAproximada"),
        longitudeAproximada = getDouble("longitudeAproximada"),
        anexos = anexos
    )
}
