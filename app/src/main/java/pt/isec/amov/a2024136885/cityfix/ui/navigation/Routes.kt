// Declara as rotas para a entrada
// e as ocorrências públicas.

package pt.isec.amov.a2024136885.cityfix.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route{

    @Serializable
    data object Entrada : Route
    @Serializable
    data object OcorrenciasPublicas : Route
    @Serializable
    data object ListarOcorrenciasPublicas : Route{

    }

    @Serializable
    data class DetalheOcorrenciaPublica(
        val id: String
    ): Route

    @Serializable
    data object Login : Route

    @Serializable
    data class MfaChallenge(val uid: String) : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object ForgotPassword : Route

    @Serializable
    data object ProfileEdit : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object ValidateUsers : Route

    @Serializable
    data object ManageCategories : Route
  
    @Serializable
    data object PainelOperacional : Route

    @Serializable
    data class DetalheOperacional(val id: String) : Route

    @Serializable
    data object CriarOcorrencia : Route

    @Serializable
    data object MinhasOcorrencias : Route

    @Serializable
    data class DetalheMinhaOcorrencia(val id: String) : Route
}
