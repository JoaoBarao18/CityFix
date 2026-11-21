package pt.isec.amov.a2024136885.cityfix.domain.location

import android.location.Location
import android.util.Log

internal fun estaDentroRaio (latitude: Double, longitude: Double): Boolean{
    val res = FloatArray(1)

    Location.distanceBetween(
        latitude,
        longitude,
        PRACA_REPUBLICA_LATITUDE,
        PRACA_REPUBLICA_LONGITUDE,
        res
    )
    Log.d("CityFixLocation", "Distancia: ${res[0]}m")

    return res[0] <= 10_000f
}
