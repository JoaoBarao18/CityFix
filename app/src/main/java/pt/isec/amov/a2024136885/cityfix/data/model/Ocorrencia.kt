package pt.isec.amov.a2024136885.cityfix.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MediaAttachment(
    val tipo: String = "foto", // "foto" ou "video"
    val storagePath: String = "",
    val contentType: String = "image/jpeg", // Tipo de conteúdo do arquivo
    val thumbnailPath: String? = null
)

@Serializable
data class EstadoHistorico(
    val estado: String = "",
    val autorUid: String = "",
    @Contextual val data: Timestamp? = null,
    val nota: String = ""
)

@Serializable
data class Ocorrencia(
    val id: String = "",
    val autorId: String = "",
    val seguidoresIds: List<String> = emptyList(),
    val categoriaId: String = "",
    val situacao: String = "avaria",
    val descricao: String = "",
    val estado: String = "aberto",
    val publica: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val morada: String? = null,
    val ficheiros: List<MediaAttachment> = emptyList(),
    val comprovativosResolucao: List<MediaAttachment> = emptyList(),
    val historicoEstados: List<EstadoHistorico> = emptyList(),

    @Contextual @ServerTimestamp
    val criadoEm: Timestamp? = null,
    @Contextual
    val atualizadoEm: Timestamp? = null,
    @Contextual
    val resolvidoEm: Timestamp? = null,
    @Contextual
    val encerradoEm: Timestamp? = null
)
