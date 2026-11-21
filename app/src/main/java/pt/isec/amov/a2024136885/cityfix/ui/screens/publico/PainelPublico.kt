package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.a2024136885.cityfix.R

@Composable
fun CartaoEstatistica(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun Ocorrencia(
    situation: String,
    categoria: String,
    status: String,
    pathImg: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (pathImg.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.public_report_no_photo),
                        modifier = Modifier.padding(4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = pathImg,
                        onError = { erro ->
                            Log.e("CityFixMiniatura", "Falha ao carregar a imagem", erro.result.throwable)
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = situation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

data class OcorrenciaPublicaUi(
    val id: String,
    val categoria: String,
    val situation: String,
    val status: String,
    val pathImg: String? = null,
    val pathVideo: String? = null,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val enderecoOcorrencia: String? = null
)

@Composable
fun PainelPublicoContent(
    ocorrencias: List<OcorrenciaPublicaUi>,
    onVerTodas: () -> Unit = {},
    onAbrirOcorrencia: (String) -> Unit = {},
    onCriarOcorrencia: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "CityFix",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (onCriarOcorrencia != null) {
            item {
                Button(onClick = onCriarOcorrencia, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_report_title))
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.public_dashboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.visitor_radius_note),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CartaoEstatistica(
                    titulo = stringResource(R.string.public_dashboard_average_time),
                    valor = stringResource(R.string.public_dashboard_average_value),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                CartaoEstatistica(
                    titulo = stringResource(R.string.public_dashboard_reports),
                    valor = "1 248",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                CartaoEstatistica(
                    titulo = stringResource(R.string.public_dashboard_active),
                    valor = "18%",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.public_dashboard_in_your_city),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onVerTodas) {
                    Text(stringResource(R.string.public_dashboard_view_all))
                }
            }
        }
        if (ocorrencias.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.public_reports_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(ocorrencias) { ocorrencia ->
            Ocorrencia(
                situation = ocorrencia.situation,
                categoria = ocorrencia.categoria,
                status = ocorrencia.status,
                modifier = Modifier.clip(MaterialTheme.shapes.large).clickable{
                    onAbrirOcorrencia(ocorrencia.id)
                },
                pathImg = ocorrencia.pathImg,
            )
        }
    }
}


//
//@Preview(showBackground = true)
//@Composable
//private fun CartaoEstatisitcaPreview(){
//    CityFIxTheme(dynamicColor = false) {
//        val packageName = LocalContext.current.packageName
//        //CartaoEstatistica(titulo = "Tempo médio", valor = "2,4 dias")
//        //PainelPublicoContent()
//        //Ocorrencia("Avaria", "Iluminçaao","Aberto", pathImg = "android.resource://$packageName/${R.drawable.img_welcome}")
//
//        PainelPublicoContent(
//            ocorrencias = listOf(
//                OcorrenciaPublicaUi(
//                    id = "ocorrencia_001",
//                    categoria = "Iluminação pública",
//                    situation = "Avaria",
//                    status = "Aberto",
//                    pathImg = "android.resource://$packageName/${R.drawable.img_welcome}",
//                ),
//                OcorrenciaPublicaUi(
//                    id = "ocorrencia_002",
//                    categoria = "Vias públicas",
//                    situation = "Dano",
//                    status = "Em resolução",
//                )
//            )
//        )
//    }
//}