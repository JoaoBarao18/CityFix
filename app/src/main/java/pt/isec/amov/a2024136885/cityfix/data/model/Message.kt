package pt.isec.amov.a2024136885.cityfix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val autorId: String = "",
    val autorNome: String = "",
    val texto: String = "",
    val criadoEm: Long = 0L
)