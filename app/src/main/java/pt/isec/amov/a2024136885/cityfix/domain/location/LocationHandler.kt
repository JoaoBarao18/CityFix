package pt.isec.amov.a2024136885.cityfix.domain.location

import android.location.Location
import android.util.Log

// Valida o acesso de visitantes ao raio de 10 km da Praça da República.
internal fun estaDentroRaio(longitude: Double, latitude: Double): Boolean {
    val distancia = FloatArray(1)

    Location.distanceBetween(
        latitude, longitude,
        PRACA_REPUBLICA_LATITUDE, PRACA_REPUBLICA_LONGITUDE,
        distancia
    )
    Log.d("CityFixLocation", "Distancia: ${distancia[0]}m")

    return distancia[0] <= 10_000f
}
