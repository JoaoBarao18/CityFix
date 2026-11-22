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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import pt.isec.amov.a2024136885.cityfix.data.model.User
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme

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
    val latitude: Double?,
    val longitude: Double?,
    val enderecoOcorrencia: String? = null,
    val miniaturaStorage: String? = null
)

@Composable
fun PainelPublicoContent(
    ocorrencias: List<OcorrenciaPublicaUi>,
    currentUser: User? = null,
    onVerTodas: () -> Unit = {},
    onAbrirOcorrencia: (String) -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onValidateUsersClick: () -> Unit = {},
    onManageCategoriesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onCriarOcorrencia: (() -> Unit)? = null,
    onMinhasOcorrencias: (() -> Unit)? = null,
    mediaReparacao: String = "—",
    totalOcorrencias: String = "—",
    percentagemAtivas: String = "—",
    aCarregar: Boolean = false,
    onTentarNovamente: (() -> Unit)? = null,
    mostrarNotaVisitante: Boolean = true,
    carregarMiniatura: (suspend (String) -> String?)? = null
) {
    var paginaAtual by rememberSaveable { mutableIntStateOf(0) }
    val itensPorPagina = 5
    val totalPaginas = maxOf(1, (ocorrencias.size + itensPorPagina - 1) / itensPorPagina)
    val paginaVisivel = paginaAtual.coerceAtMost(totalPaginas - 1)
    val inicio = paginaVisivel * itensPorPagina
    val fim = minOf(inicio + itensPorPagina, ocorrencias.size)
    val ocorrenciasPaginadas = if (inicio < ocorrencias.size) ocorrencias.subList(inicio, fim) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botões exclusivos de Operador
                        if (currentUser.perfil == "operador") {
                            IconButton(
                                onClick = onManageCategoriesClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = stringResource(R.string.operator_manage_categories),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(
                                onClick = onValidateUsersClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = stringResource(R.string.operator_validate_users),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Botão de Perfil
                        IconButton(
                            onClick = onPerfilClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (!currentUser.fotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser.fotoUrl,
                                    contentDescription = stringResource(R.string.operator_profile),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.operator_profile),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (onCriarOcorrencia != null) {
            item {
                Button(onClick = onCriarOcorrencia, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_report_title))
                }
            }
        }
        if (onMinhasOcorrencias != null) {
            item {
                TextButton(onClick = onMinhasOcorrencias, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.my_reports_title))
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
                    if (mostrarNotaVisitante) {
                        Text(
                            text = stringResource(R.string.visitor_radius_note),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                    valor = mediaReparacao,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                CartaoEstatistica(
                    titulo = stringResource(R.string.public_dashboard_reports),
                    valor = totalOcorrencias,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                CartaoEstatistica(
                    titulo = stringResource(R.string.public_dashboard_active),
                    valor = percentagemAtivas,
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
        if (aCarregar || onTentarNovamente != null || ocorrencias.isEmpty()) {
            item { EstadoListaPublica(aCarregar, onTentarNovamente) }
        }
        items(ocorrenciasPaginadas, key = { it.id }) { ocorrencia ->
            CartaoOcorrenciaPublica(ocorrencia, carregarMiniatura) {
                onAbrirOcorrencia(ocorrencia.id)
            }
        }
        if (ocorrencias.size > itensPorPagina) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { paginaAtual = paginaVisivel - 1 },
                        enabled = paginaVisivel > 0
                    ) {
                        Text(stringResource(R.string.public_page_previous))
                    }

                    Text(
                        text = stringResource(R.string.public_page_of, paginaVisivel + 1, totalPaginas),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { paginaAtual = paginaVisivel + 1 },
                        enabled = paginaVisivel < totalPaginas - 1
                    ) {
                        Text(stringResource(R.string.public_page_next))
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = onLogoutClick,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(
                    if (currentUser != null) R.string.operator_logout else R.string.public_return_home
                )
            )
        }
    }
}

@Composable
internal fun CartaoOcorrenciaPublica(
    ocorrencia: OcorrenciaPublicaUi,
    carregarMiniatura: (suspend (String) -> String?)?,
    onAbrir: () -> Unit
) {
    val miniatura by produceState(ocorrencia.pathImg, ocorrencia.id, ocorrencia.miniaturaStorage, ocorrencia.pathImg) {
        value = ocorrencia.pathImg ?: ocorrencia.miniaturaStorage?.let { carregarMiniatura?.invoke(it) }
    }
    Ocorrencia(
        situation = ocorrencia.situation,
        categoria = ocorrencia.categoria,
        status = ocorrencia.status,
        pathImg = miniatura,
        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable(role = Role.Button, onClick = onAbrir)
    )
}

@Preview(showBackground = true, locale = "pt-rPT")
@Composable
private fun PainelPublicoPreview() {
    CityFIxTheme(dynamicColor = false) {
        PainelPublicoContent(
            ocorrencias = ocorrenciasExemplo(LocalContext.current.packageName),
            mediaReparacao = "2,4 dias",
            totalOcorrencias = "1 248",
            percentagemAtivas = "18%"
        )
    }
}
