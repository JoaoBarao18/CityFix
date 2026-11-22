package pt.isec.amov.a2024136885.cityfix.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

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
    @Contextual @ServerTimestamp val criadoEm: Timestamp? = null,
    @Contextual val validadoEm: Timestamp? = null,
    val validadoPor: String? = null,

    val mfaCode: String? = null,
    @Contextual val mfaExpiresAt: Timestamp? = null
)