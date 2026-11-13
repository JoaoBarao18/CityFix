package pt.isec.amov.a2024136885.cityfix.domain.location

import android.location.Location
import android.util.Log

internal fun estaDentroRaio (logitude: Double, latitude: Double): Boolean{
    val res = FloatArray(1)

    Location.distanceBetween(
        logitude, latitude,
        PRACA_REPUBLICA_LONGITUDE, PRACA_REPUBLICA_LATITUDE,
        res
    )
    Log.d("CityFixLocation", "Distancia: ${res[0]}m")

    return res[0] <= 10_000f
}
