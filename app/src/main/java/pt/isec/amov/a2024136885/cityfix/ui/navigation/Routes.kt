package pt.isec.amov.a2024136885.cityfix.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route{
    @Serializable
    data object Entrada : Route

    @Serializable
    data object OcorrenciasPublicas : Route

}