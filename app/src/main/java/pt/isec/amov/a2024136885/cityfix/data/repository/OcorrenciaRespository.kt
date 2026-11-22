package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.domain.location.podeAgruparOcoorencia
import java.security.MessageDigest

internal class OcorrenciaRespository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val ocorrencias = firestore.collection("ocorrencias")

    suspend fun procurarSubmissao(autorId: String, id: String): String? {
        verificarSessao(autorId)
        val submissao = referenciaSubmissao(autorId, id).get(Source.SERVER).await()
        submissao.getString("ocorrenciaId")?.let { return it }
        val guardada = ocorrencias.document(id).get(Source.SERVER).await()
        return id.takeIf { guardada.exists() && guardada.getString("autorId") == autorId }
    }

    suspend fun prepararCriacao(ocorrencia: Ocorrencia) {
        verificarSessao(ocorrencia.autorId)
        val perfil = firestore.collection("utilizadores").document(ocorrencia.autorId)
            .get(Source.SERVER).await()
        check(perfil.getString("perfil") == "municipe" && perfil.getBoolean("validado") == true)
        val categoria = firestore.collection("categorias").document(ocorrencia.categoriaId)
            .get(Source.SERVER).await()
        check(categoria.getBoolean("ativa") == true)
    }

    suspend fun criar(ocorrencia: Ocorrencia): String {
        verificarSessao(ocorrencia.autorId)
        require(ocorrencia.id.isNotBlank() && ocorrencia.ficheiros.isNotEmpty())

        val referencia = ocorrencias.document(ocorrencia.id)
        val submissao = referenciaSubmissao(ocorrencia.autorId, ocorrencia.id)
        val controloGrupo = firestore.collection("versoesAgrupamento").document(chaveGrupo(ocorrencia))
        val candidatas = ocorrencias
            .whereEqualTo("categoriaId", ocorrencia.categoriaId)
            .whereEqualTo("situacao", ocorrencia.situacao)
            .whereEqualTo("publica", ocorrencia.publica)
            .let { if (ocorrencia.publica) it else it.whereEqualTo("autorId", ocorrencia.autorId) }

        // Uma mudança na versão obriga a repetir a pesquisa, mesmo quando os dois pedidos não encontram candidato.
        repeat(5) {
            val versao = controloGrupo.get(Source.SERVER).await().getLong("versao") ?: 0L
            val candidata = candidatas.get(Source.SERVER).await().documents
                .filter { it.agrupavelCom(ocorrencia) }
                .minWithOrNull(compareBy<DocumentSnapshot>(
                    { it.ocorrencia().criadoEm },
                    { it.id }
                ))

            val idFinal = firestore.runTransaction { transacao ->
                transacao.get(submissao).getString("ocorrenciaId")?.let { return@runTransaction it }
                val original = transacao.get(referencia)
                if (original.exists()) {
                    check(original.getString("autorId") == ocorrencia.autorId)
                    return@runTransaction referencia.id
                }
                val atual = transacao.get(controloGrupo).getLong("versao") ?: 0L
                if (atual != versao) return@runTransaction null

                val destino = candidata?.let { transacao.get(it.reference) }
                if (destino != null && !destino.agrupavelCom(ocorrencia)) return@runTransaction null

                val idGuardado = destino?.id ?: referencia.id
                val contribuicao = transacao.get(firestore.referenciaContribuicao(idGuardado))
                val categoria = if (ocorrencia.publica) {
                    transacao.get(firestore.collection("categorias").document(ocorrencia.categoriaId))
                        .getString("nome").orEmpty()
                } else ""

                val atualizada = if (destino == null) {
                    transacao.set(referencia, ocorrencia)
                    ocorrencia
                } else {
                    val anterior = destino.ocorrencia()
                    val juntar = anterior.autorId != ocorrencia.autorId
                    val agora = System.currentTimeMillis()
                    val alteracoes = mutableMapOf<String, Any>(
                        "ficheiros" to FieldValue.arrayUnion(*ocorrencia.ficheiros.toTypedArray()),
                        "atualizadoEm" to agora
                    )
                    if (juntar) alteracoes["seguidoresIds"] = FieldValue.arrayUnion(ocorrencia.autorId)
                    transacao.update(destino.reference, alteracoes)
                    anterior.copy(
                        ficheiros = (anterior.ficheiros + ocorrencia.ficheiros).distinctBy { it.storagePath },
                        seguidoresIds = if (juntar) (anterior.seguidoresIds + ocorrencia.autorId).distinct()
                        else anterior.seguidoresIds,
                        atualizadoEm = agora
                    )
                }
                atualizarPainelPublico(firestore, transacao, atualizada, categoria, contribuicao)
                transacao.set(
                    submissao,
                    mapOf(
                        "ocorrenciaId" to idGuardado,
                        "descricao" to ocorrencia.descricao,
                        "latitude" to ocorrencia.latitude,
                        "longitude" to ocorrencia.longitude,
                        "criadoEm" to ocorrencia.criadoEm
                    )
                )
                transacao.set(controloGrupo, mapOf("versao" to atual + 1))
                idGuardado
            }.await()
            if (idFinal != null) return idFinal
        }

        throw FirebaseFirestoreException(
            "O grupo foi atualizado enquanto a ocorrência era submetida",
            FirebaseFirestoreException.Code.ABORTED
        )
    }

    fun observarMinhas(autorId: String) = combine(
        ocorrencias.whereEqualTo("autorId", autorId).observar(),
        ocorrencias.whereArrayContains("seguidoresIds", autorId).observar()
    ) { proprias, seguidas ->
        verificarSessao(autorId)
        (proprias + seguidas).distinctBy { it.id }.sortedByDescending { it.atualizadoEm }
    }

    suspend fun carregarMinha(autorId: String, id: String): Ocorrencia {
        verificarSessao(autorId)
        val ocorrencia = ocorrencias.document(id).get().await().ocorrencia()
        check(ocorrencia.autorId == autorId || autorId in ocorrencia.seguidoresIds)
        return ocorrencia
    }

    suspend fun nomeCategoria(id: String): String? =
        id.takeIf(String::isNotBlank)?.let {
            firestore.collection("categorias").document(it).get().await().getString("nome")
        }

    suspend fun urlAnexo(caminho: String): String =
        FirebaseStorage.getInstance().reference.child(caminho).downloadUrl.await().toString()

    private fun verificarSessao(autorId: String) {
        check(autorId.isNotBlank() && auth.currentUser?.uid == autorId)
    }

    private fun referenciaSubmissao(autorId: String, id: String) =
        firestore.collection("utilizadores").document(autorId).collection("submissoesOcorrencias").document(id)
}

private fun DocumentSnapshot.agrupavelCom(nova: Ocorrencia): Boolean {
    if (!exists()) return false
    val existente = ocorrencia()
    if (existente.publica != nova.publica || (!nova.publica && existente.autorId != nova.autorId)) return false
    return podeAgruparOcoorencia(
        categoriaIdNova = nova.categoriaId,
        situcaoNova = nova.situacao,
        latNova = nova.latitude ?: return false,
        longNova = nova.longitude ?: return false,
        categoriaExis = existente.categoriaId,
        situacaoExis = existente.situacao,
        latExis = existente.latitude ?: return false,
        longExist = existente.longitude ?: return false,
        existAtiva = existente.estado in estadosAtivosOcorrencia
    )
}

private fun chaveGrupo(ocorrencia: Ocorrencia): String {
    val escopo = if (ocorrencia.publica) "publica" else "privada/${ocorrencia.autorId}"
    val chave = "$escopo/${ocorrencia.categoriaId}/${ocorrencia.situacao}"
    return MessageDigest.getInstance("SHA-256").digest(chave.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

private fun Query.observar() = callbackFlow {
    val listener = addSnapshotListener { snapshot, erro ->
        if (erro != null) close(erro)
        else if (snapshot != null) trySend(snapshot.documents.map { it.ocorrencia() })
    }
    awaitClose(listener::remove)
}
