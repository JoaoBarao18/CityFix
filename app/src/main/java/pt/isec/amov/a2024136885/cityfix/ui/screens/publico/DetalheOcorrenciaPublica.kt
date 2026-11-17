package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme

@Composable
fun DetalheOcorrenciaPublicaContent(
    categoria: String,
    situacao: String,
    status: String,
    descricao: String,
    latitude: Double ,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = situacao, style = MaterialTheme.typography.headlineSmall)
        Text(text = categoria, style = MaterialTheme.typography.bodyMedium)
        Text(text = status, style = MaterialTheme.typography.labelLarge)
        Text(text = descricao, style = MaterialTheme.typography.bodyLarge)

        val camera = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(longitude = longitude, latitude = latitude),
                zoom = 12.0
            )
        )

        MaplibreMap(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            cameraState = camera
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun DetalheOcorrenciaPublicaPreview() {
    CityFIxTheme(dynamicColor = false) {
        DetalheOcorrenciaPublicaContent(
            categoria = "Iluminação pública",
            situacao = "Avaria",
            status = "Aberto",
            descricao = "Um candeeiro da via pública deixou de funcionar.",
            latitude = 40.2800,
            longitude = -8.4800
        )
    }
}
