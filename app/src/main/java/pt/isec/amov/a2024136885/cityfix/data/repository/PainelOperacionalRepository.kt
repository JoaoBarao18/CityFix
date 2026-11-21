package pt.isec.amov.a2024136885.cityfix.data.repository

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.MediaAttachment
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.model.User
import java.io.File

internal data class EventoOperacional(val estado: String, val instante: Long)

internal data class DetalheOperacional(
    val ocorrencia: Ocorrencia,
    val autor: User?,
    val historico: List<EventoOperacional>
)

internal class PainelOperacionalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun listar(operadorId: String): List<Ocorrencia> {
        verificarOperador(operadorId)
        return firestore.collection("ocorrencias").get().await().documents
            .map { it.ocorrencia() }
            .sortedByDescending { it.criadoEm }
    }

    suspend fun carregarDetalhe(operadorId: String, id: String): DetalheOperacional {
        verificarOperador(operadorId)
        val ref = firestore.collection("ocorrencias").document(id)
        val ocorrencia = ref.get().await().ocorrencia()
        val autor = ocorrencia.autorId.takeIf { it.isNotBlank() }?.let { autorId ->
            firestore.collection("utilizadores").document(autorId).get().await()
                .takeIf { it.exists() }
                ?.let {
                    User(
                        uid = it.id,
                        nome = it.getString("nome").orEmpty(),
                        email = it.getString("email").orEmpty(),
                        telefone = it.getString("telefone").orEmpty()
                    )
                }
        }
        val eventos = ref.collection("historico").get().await().documents.mapNotNull { evento ->
            val estado = evento.getString("estado") ?: return@mapNotNull null
            val instante = evento.milisegundos("dataHora") ?: return@mapNotNull null
            EventoOperacional(estado, instante)
        }
        val historico = buildList {
            if (ocorrencia.criadoEm > 0 && eventos.none { it.estado == "aberto" }) {
                add(EventoOperacional("aberto", ocorrencia.criadoEm))
            }
            addAll(eventos)
        }.sortedBy { it.instante }
        return DetalheOperacional(ocorrencia, autor, historico)
    }

    suspend fun iniciarResolucao(operadorId: String, id: String): Ocorrencia =
        mudarEstado(operadorId, id, "em_resolucao")

    suspend fun resolver(
        operadorId: String,
        id: String,
        fotoPath: String?,
        videoPath: String?
    ): Ocorrencia {
        verificarOperador(operadorId)
        val atual = firestore.collection("ocorrencias").document(id).get().await().ocorrencia()
        require(atual.autorId.isNotBlank())
        val ficheiros = buildList {
            fotoPath?.let { add(ficheiroComprovativo(atual, it, "foto")) }
            videoPath?.let { add(ficheiroComprovativo(atual, it, "video")) }
        }
        require(ficheiros.isNotEmpty())
        val comprovativos = ficheiros.map { it.second }
        if (atual.estado == "resolvido" && atual.temComprovativos(comprovativos)) return atual
        check(atual.estado == "em_resolucao")
        for ((ficheiro, anexo) in ficheiros) {
            val metadata = StorageMetadata.Builder().setContentType(anexo.contentType).build()
            storage.reference.child(anexo.storagePath).putFile(Uri.fromFile(ficheiro), metadata).await()
        }
        return mudarEstado(operadorId, id, "resolvido", comprovativos)
    }

    suspend fun urlAnexo(operadorId: String, storagePath: String): String {
        verificarOperador(operadorId)
        return storage.reference.child(storagePath).downloadUrl.await().toString()
    }

    private suspend fun mudarEstado(
        operadorId: String,
        id: String,
        destino: String,
        comprovativos: List<MediaAttachment> = emptyList()
    ): Ocorrencia {
        check(auth.currentUser?.uid == operadorId)
        val perfilRef = firestore.collection("utilizadores").document(operadorId)
        val ref = firestore.collection("ocorrencias").document(id)
        val evento = ref.collection("historico").document()
        val agora = System.currentTimeMillis()
        return firestore.runTransaction { transacao ->
            check(transacao.get(perfilRef).operadorValidado())
            val atual = transacao.get(ref).ocorrencia()
            if (atual.estado == destino &&
                (destino == "em_resolucao" || atual.temComprovativos(comprovativos))
            ) {
                return@runTransaction atual
            }
            val inicioPermitido = destino == "em_resolucao" &&
                atual.estado in listOf("aberto", "pedido_informacoes")
            val resolucaoPermitida = destino == "resolvido" &&
                atual.estado == "em_resolucao" && comprovativos.isNotEmpty()
            check(inicioPermitido || resolucaoPermitida)
            val alteracoes = mutableMapOf<String, Any>("estado" to destino, "atualizadoEm" to agora)
            val novosComprovativos = (atual.comprovativosResolucao + comprovativos)
                .distinctBy { it.storagePath }
            if (resolucaoPermitida) {
                alteracoes["resolvidoEm"] = agora
                alteracoes["comprovativosResolucao"] = novosComprovativos
            }
            transacao.update(ref, alteracoes)
            transacao.set(
                evento,
                mapOf(
                    "estado" to destino,
                    "estadoAnterior" to atual.estado,
                    "dataHora" to FieldValue.serverTimestamp(),
                    "operadorId" to operadorId
                )
            )
            atual.copy(
                estado = destino,
                atualizadoEm = agora,
                resolvidoEm = if (resolucaoPermitida) agora else atual.resolvidoEm,
                comprovativosResolucao = novosComprovativos
            )
        }.await()
    }

    private suspend fun verificarOperador(id: String) {
        check(id.isNotBlank() && auth.currentUser?.uid == id)
        check(firestore.collection("utilizadores").document(id).get().await().operadorValidado())
    }

    private fun ficheiroComprovativo(ocorrencia: Ocorrencia, caminho: String, tipo: String)
        : Pair<File, MediaAttachment> {
        val ficheiro = File(caminho)
        require(ficheiro.isFile && ficheiro.length() > 0)
        val mime = if (tipo == "foto") "image/jpeg" else
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ficheiro.extension.lowercase())
                ?.takeIf { it.startsWith("video/") } ?: "video/mp4"
        val destino = "ocorrencias/${ocorrencia.autorId}/${ocorrencia.id}/comprovativos/${ficheiro.name}"
        return ficheiro to MediaAttachment(tipo = tipo, storagePath = destino, contentType = mime)
    }
}

private fun DocumentSnapshot.operadorValidado(): Boolean =
    getString("perfil") == "operador" && getBoolean("validado") == true

private fun Ocorrencia.temComprovativos(comprovativos: List<MediaAttachment>): Boolean =
    comprovativos.isNotEmpty() && comprovativos.all { novo ->
        comprovativosResolucao.any { it.storagePath == novo.storagePath }
    }

private fun DocumentSnapshot.milisegundos(campo: String): Long? = when (val valor = get(campo)) {
    is Number -> valor.toLong()
    is Timestamp -> valor.toDate().time
    else -> null
}

private fun DocumentSnapshot.anexos(campo: String): List<MediaAttachment> =
    (get(campo) as? List<*>).orEmpty().mapNotNull { valor ->
        val anexo = valor as? Map<*, *> ?: return@mapNotNull null
        val caminho = (anexo["storagePath"] as? String)?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val tipo = anexo["tipo"] as? String ?: "foto"
        MediaAttachment(
            tipo = tipo,
            storagePath = caminho,
            contentType = anexo["contentType"] as? String ?: if (tipo == "video") "video/mp4" else "image/jpeg",
            thumbnailPath = anexo["thumbnailPath"] as? String
        )
    }

private fun DocumentSnapshot.ocorrencia(): Ocorrencia {
    check(exists())
    return Ocorrencia(
        id = id,
        autorId = getString("autorId").orEmpty(),
        seguidoresIds = (get("seguidoresIds") as? List<*>).orEmpty().filterIsInstance<String>(),
        categoriaId = getString("categoriaId").orEmpty(),
        situacao = getString("situacao") ?: "avaria",
        descricao = getString("descricao").orEmpty(),
        estado = getString("estado") ?: "aberto",
        publica = getBoolean("publica") ?: true,
        latitude = (get("latitude") as? Number)?.toDouble(),
        longitude = (get("longitude") as? Number)?.toDouble(),
        morada = getString("morada"),
        ficheiros = anexos("ficheiros"),
        comprovativosResolucao = anexos("comprovativosResolucao"),
        criadoEm = milisegundos("criadoEm") ?: 0,
        atualizadoEm = milisegundos("atualizadoEm") ?: 0,
        resolvidoEm = milisegundos("resolvidoEm")
    )
}
