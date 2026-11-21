package pt.isec.amov.a2024136885.cityfix.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val nif: String = "",
    val perfil: String = "municipe", // "municipe" ou "operador"
    val validado: Boolean = false,
    val fotoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val detecaoImpactos: Boolean = false,
    val criadoEm: Long = 0L,
    val validadoEm: Long? = null,
    val validadoPor: String? = null,

    val mfaCode: String? = null,
    val mfaExpiresAt: Long? = null
)