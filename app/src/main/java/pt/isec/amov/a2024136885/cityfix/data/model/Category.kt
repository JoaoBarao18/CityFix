package pt.isec.amov.a2024136885.cityfix.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Serializable
data class Category(
    val id: String = "",
    val nome: String = "",
    val ativa: Boolean = true,
    @Contextual
    @ServerTimestamp
    val criadaEm: Timestamp? = null,
    @Contextual
    val atualizadaEm: Timestamp? = null
)