package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import kotlin.math.round

internal val estadosAtivosOcorrencia = setOf("aberto", "pedido_informacoes", "em_resolucao")
internal const val PASTA_ANEXOS_PUBLICOS = "painelPublico"

internal fun FirebaseFirestore.referenciaContribuicao(id: String) =
    collection("contribuicoesPainel").document(id)

internal fun atualizarPainelPublico(
    firestore: FirebaseFirestore,
    transacao: Transaction,
    ocorrencia: Ocorrencia,
    nomeCategoria: String,
    anterior: DocumentSnapshot
) {
    val ativa = ocorrencia.estado in estadosAtivosOcorrencia
    val resolvidaEm = ocorrencia.resolvidoEm
    val criadoEmMs = ocorrencia.criadoEm?.toDate()?.time ?: 0L
    val resolvidaEmMs = resolvidaEm?.toDate()?.time ?: 0L
    val reparada = ocorrencia.estado in listOf("resolvido", "encerrado") &&
        criadoEmMs > 0L && resolvidaEmMs >= criadoEmMs

    val contribuicao = mapOf(
        "total" to 1L,
        "ativas" to if (ativa) 1L else 0L,
        "reparadas" to if (reparada) 1L else 0L,
        "duracaoTotalMs" to if (reparada) (resolvidaEmMs - criadoEmMs) else 0L
    )
    val diferencas = contribuicao.mapValues { (campo, valor) ->
        valor - (anterior.getLong(campo) ?: 0L)
    }
    if (!anterior.exists() || diferencas.values.any { it != 0L }) {
        transacao.set(
            firestore.document("estatisticasPublicas/painel"),
            diferencas.mapValues { FieldValue.increment(it.value) },
            SetOptions.merge()
        )
        transacao.set(anterior.reference, contribuicao)
    }

    val publica = firestore.collection("ocorrenciasPublicas").document(ocorrencia.id)
    if (ocorrencia.publica && ativa) {
        val anexos = ocorrencia.ficheiros.mapNotNull { anexo ->
            val caminho = anexo.storagePath.takeIf(::eAnexoPublico) ?: return@mapNotNull null
            mapOf(
                "tipo" to anexo.tipo,
                "storagePath" to caminho,
                "thumbnailPath" to anexo.thumbnailPath?.takeIf(::eAnexoPublico)
            )
        }
        transacao.set(publica, mapOf(
            "categoria" to nomeCategoria,
            "situacao" to ocorrencia.situacao,
            "estado" to ocorrencia.estado,
            "atualizadoEm" to (ocorrencia.atualizadoEm?.toDate()?.time ?: criadoEmMs),
            "latitudeAproximada" to ocorrencia.latitude?.takeIf { it in -90.0..90.0 }?.let { round(it * 10) / 10 },
            "longitudeAproximada" to ocorrencia.longitude?.takeIf { it in -180.0..180.0 }?.let { round(it * 10) / 10 },
            "anexos" to anexos
        ))
    } else if (ocorrencia.publica) {
        transacao.delete(publica)
    }
}

internal fun eAnexoPublico(caminho: String): Boolean =
    caminho.startsWith("$PASTA_ANEXOS_PUBLICOS/") && caminho.split('/').size == 3
