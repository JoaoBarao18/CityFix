package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import coil3.compose.AsyncImage
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.ui.components.AnexoVideo
import pt.isec.amov.a2024136885.cityfix.ui.components.AvisoProximidade
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme

@Composable
fun DetalheOcorrenciaPublicaContent(
    categoria: String,
    situacao: String,
    status: String,
    descricao: String,
    latitudeAproximada: Double?,
    longitudeAproximada: Double?,
    pathImg: String? = null,
    pathVideo: String? = null,
    onVoltar: () -> Unit = {},
    modifier: Modifier = Modifier,
    mostrarAvisoProximidade: Boolean = false,
    anexosPublicados: @Composable () -> Unit = {},
    conteudoAdicional: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.public_report_back)
                )
            }
            Text(
                text = situacao,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (mostrarAvisoProximidade) AvisoProximidade()

        if (!pathImg.isNullOrBlank()) FotoOcorrencia(pathImg)
        if (!pathVideo.isNullOrBlank()) AnexoVideo(pathVideo)
        anexosPublicados()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = categoria,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (descricao.isNotBlank()) {
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (latitudeAproximada != null && longitudeAproximada != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.public_report_approximate_location),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!LocalInspectionMode.current) {
                        MapaAproximado(latitudeAproximada, longitudeAproximada)
                    }
                }
            }
        }
        conteudoAdicional()
    }
}

@Composable
internal fun FotoOcorrencia(url: String) {
    AsyncImage(
        model = url,
        contentDescription = stringResource(R.string.public_report_photo),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Fit,
        onError = { error ->
            Log.e("DetalheOcorrencia", "Falha ao carregar a imagem", error.result.throwable)
        }
    )
}

@Composable
private fun MapaAproximado(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapa = remember(context, latitude, longitude) {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(mapa, lifecycle) {
        var ativo = true
        var iniciado = false
        var retomado = false
        mapa.getMapAsync { map ->
            if (ativo) {
                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(11.0)
                    .build()
            }
        }
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> {
                    mapa.onStart()
                    iniciado = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    mapa.onResume()
                    retomado = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapa.onPause()
                    retomado = false
                }
                Lifecycle.Event.ON_STOP -> {
                    mapa.onStop()
                    iniciado = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observador)
        onDispose {
            ativo = false
            lifecycle.removeObserver(observador)
            if (retomado) mapa.onPause()
            if (iniciado) mapa.onStop()
            mapa.onDestroy()
        }
    }

    androidx.compose.runtime.key(mapa) {
        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true, locale = "pt-rPT", widthDp = 390)
@Composable
private fun DetalheOcorrenciaPublicaPreview() {
    val packageName = LocalContext.current.packageName
    CityFIxTheme(dynamicColor = false) {
        DetalheOcorrenciaPublicaContent(
            categoria = "Iluminação pública",
            situacao = "Avaria na lâmpada",
            status = "Aberto",
            descricao = "Um candeeiro da via pública deixou de funcionar.",
            latitudeAproximada = 40.2,
            longitudeAproximada = -8.4,
            pathImg = "android.resource://$packageName/${R.drawable.img_welcome}",
            pathVideo = "android.resource://$packageName/${R.raw.flower}"
        )
    }
}
