package pt.isec.amov.a2024136885.cityfix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val nome: String = "",
    val ativa: Boolean = true,
    val criadoEm: Long = 0L,
    val atualizadoEm: Long = 0L
)