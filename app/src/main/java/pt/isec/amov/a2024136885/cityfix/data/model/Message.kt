package pt.isec.amov.a2024136885.cityfix.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val ticketId: String = "",
    val autorId: String = "",
    val autorNome: String = "",
    val perfil: String = "municipe", // "municipe" ou "operador"
    val texto: String = "",

    @Contextual @ServerTimestamp
    val enviadoEm: Timestamp? = null
)