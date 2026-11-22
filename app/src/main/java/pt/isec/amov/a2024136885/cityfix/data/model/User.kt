package pt.isec.amov.a2024136885.cityfix.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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

fun DocumentSnapshot.toUserSafe(): User? {
    if (!exists()) return null
    return try {
        toObject(User::class.java)?.copy(uid = id)
    } catch (e: Exception) {
        val parseTimestamp: (Any?) -> Timestamp? = { raw ->
            when (raw) {
                is Timestamp -> raw
                is Long -> Timestamp(Date(raw))
                is Number -> Timestamp(Date(raw.toLong()))
                else -> null
            }
        }
        User(
            uid = id,
            nome = getString("nome").orEmpty(),
            email = getString("email").orEmpty(),
            telefone = getString("telefone").orEmpty(),
            nif = getString("nif").orEmpty(),
            perfil = getString("perfil") ?: "municipe",
            validado = getBoolean("validado") ?: false,
            fotoUrl = getString("fotoUrl"),
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
            detecaoImpactos = getBoolean("detecaoImpactos") ?: false,
            criadoEm = parseTimestamp(get("criadoEm")),
            validadoEm = parseTimestamp(get("validadoEm")),
            validadoPor = getString("validadoPor"),
            mfaCode = getString("mfaCode"),
            mfaExpiresAt = parseTimestamp(get("mfaExpiresAt"))
        )
    }
}