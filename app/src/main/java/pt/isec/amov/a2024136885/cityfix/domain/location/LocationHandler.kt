package pt.isec.amov.a2024136885.cityfix.domain.location

import android.location.Location
import android.util.Log
import pt.isec.amov.a2024136885.cityfix.data.model.Categoria

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

private fun distanciaMetros(
    longA: Double,
    longB: Double,
    latA: Double,
    latB: Double
): Float{
    val res = FloatArray(1)
    Location.distanceBetween(
        latA, longA,
        latB, longB,
        res
    )
    return res[0]
}

//vai comparaqr um nova ocorencia como q ja existe
internal fun podeAgruparOcoorencia(
    categoriaIdNova: String,
    situcaoNova: String,
    latNova: Double,
    longNova: Double,
    categoriaExis: String,
    situacaoExis: String,
    latExis: Double,
    longExist: Double,
    existAtiva: Boolean
): Boolean{
    return existAtiva && categoriaIdNova == categoriaExis &&
            situcaoNova == situacaoExis &&
            distanciaMetros(
                latA = latNova,
                longA = longNova,
                latB = latExis,
                longB = longExist
            )<= 10f
}
