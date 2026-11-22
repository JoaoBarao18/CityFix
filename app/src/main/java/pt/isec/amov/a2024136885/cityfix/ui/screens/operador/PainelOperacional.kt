package pt.isec.amov.a2024136885.cityfix.ui.screens.operador

import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.estadosOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.situacoesOcorrencia
import pt.isec.amov.a2024136885.cityfix.data.model.User


@Composable
fun PainelOperacionalContent(
    ocorrencias: List<Ocorrencia>,
    onAbrirOcorrencia: (String) -> Unit,
    onAtualizarPainelPublico: (() -> Unit)? = null,
    aAtualizarPainelPublico: Boolean = false,
    mensagemPainelPublico: Int? = null,
    onPerfilClick: (() -> Unit)? = null,
    onValidateUsersClick: (() -> Unit)? = null,
    onManageCategoriesClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    aCarregarLista: Boolean = false,
    onTentarCarregar: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.operational_panel_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (onManageCategoriesClick != null && onValidateUsersClick != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onManageCategoriesClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.operator_manage_categories))
                    }
                    TextButton(onClick = onValidateUsersClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.operator_validate_users))
                    }
                }
            }
        }
        if (onPerfilClick != null && onLogoutClick != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onPerfilClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.operator_profile))
                    }
                    TextButton(onClick = onLogoutClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.operator_logout))
                    }
                }
            }
        }
        when {
            aCarregarLista -> item { CircularProgressIndicator() }
            onTentarCarregar != null -> item {
                Text(stringResource(R.string.operational_load_error), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onTentarCarregar) {
                    Text(stringResource(R.string.operational_retry))
                }
            }
            ocorrencias.isEmpty() -> item { Text(stringResource(R.string.operational_panel_empty)) }
            else -> {
                if (onAtualizarPainelPublico != null) {
                    item {
                        TextButton(onClick = onAtualizarPainelPublico, enabled = !aAtualizarPainelPublico) {
                            val mensagem = if (aAtualizarPainelPublico) R.string.public_refresh_pending
                                else R.string.public_refresh
                            Text(stringResource(mensagem))
                        }
                        if (mensagemPainelPublico != null) {
                            Text(stringResource(mensagemPainelPublico))
                        }
                    }
                }
                items(ocorrencias, key = { it.id }) { ocorrencia ->
                    val situacao = situacoesOcorrencia[ocorrencia.situacao]
                        ?.let { stringResource(it) } ?: ocorrencia.situacao
                    val estado = estadosOcorrencia[ocorrencia.estado]
                        ?.let { stringResource(it) } ?: ocorrencia.estado
                    ElevatedCard(
                        onClick = { onAbrirOcorrencia(ocorrencia.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = estado,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "ID: ${ocorrencia.id.take(8)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = ocorrencia.categoriaId.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = situacao,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (ocorrencia.descricao.isNotBlank()) {
                                Text(
                                    text = ocorrencia.descricao,
                                    style = MaterialTheme.typography.bodySmall,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class EventoHistoricoUi(
    val estado: String,
    val dataHora: String,
    val nota: String = ""
)
@Composable
fun DetalheOcoorenciaOperadorContent(
    ocorrencia: Ocorrencia,
    autor: User?,
    onVoltar: () -> Unit,
    historico: List<EventoHistoricoUi> = emptyList(),
    anexos: @Composable () -> Unit = {},
    acoes: @Composable () -> Unit = {}
){
    val situacao = situacoesOcorrencia[ocorrencia.situacao]
        ?.let { stringResource(it) } ?: ocorrencia.situacao
    val estado = estadosOcorrencia[ocorrencia.estado]
        ?.let { stringResource(it) } ?: ocorrencia.estado
    val latitude = ocorrencia.latitude
    val longitude = ocorrencia.longitude
    val contactos = listOfNotNull(autor?.nome, autor?.email, autor?.telefone)
        .filter { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.public_report_back)
                )
            }
            Text(stringResource(R.string.public_report_back), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // INFO DO AUTOR
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.operational_author_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(autor?.nome ?: "Autor Desconhecido", style = MaterialTheme.typography.bodyMedium)
                }
                if(contactos.isNotEmpty()){
                    autor?.telefone?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 26.dp))
                    }
                    autor?.email?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 26.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.operational_no_contact), style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 26.dp))
                }
            }
        }

        // INFO DA OCORRÊNCIA
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.report_description), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("ID: ${ocorrencia.id}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(ocorrencia.categoriaId.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(situacao, style = MaterialTheme.typography.bodyMedium)
                
                if (ocorrencia.descricao.isNotBlank()) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    Text(ocorrencia.descricao, style = MaterialTheme.typography.bodyMedium)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.operational_location_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if(latitude != null && longitude != null){
                        stringResource(
                            R.string.operational_coordinates,
                            latitude.toString(),
                            longitude.toString()
                        )
                    } else {
                        stringResource(R.string.operational_no_coordinates)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        anexos()
        acoes()
        
        // HISTÓRICO
        if (historico.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.operational_history_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    historico.forEach { evento ->
                        val evtEstado = estadosOcorrencia[evento.estado]?.let { stringResource(it) } ?: evento.estado
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(evtEstado, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (evento.nota.isNotBlank()) {
                                    Text(evento.nota, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            Text(evento.dataHora, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        androidx.compose.material3.HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "pt-rPT")
@Composable
private fun PainelOperacionalPreview() {
    val ocorrencias = listOf(
        Ocorrencia(
            id = "1",
            situacao = "avaria",
            descricao = "Candeeiro avariado na Praça da República.",
            estado = "aberto"
        ),
        Ocorrencia(
            id = "2",
            situacao = "dano",
            descricao = "Buraco no pavimento junto à passadeira.",
            estado = "em_resolucao",
            latitude = 40.2081,
            longitude = -8.4194
        )
    )
    var idSelecionado by remember { mutableStateOf<String?>(null) }
    val selecionada = ocorrencias.firstOrNull { it.id == idSelecionado }

    CityFIxTheme(dynamicColor = false) {
        if (selecionada == null) {
            PainelOperacionalContent(
                ocorrencias = ocorrencias,
                onAbrirOcorrencia = { id -> idSelecionado = id }
            )
        } else {
            DetalheOcoorenciaOperadorContent(
                ocorrencia = selecionada,
                autor = User(nome = "Munícipe de teste", email = "municipe@example.com"),
                onVoltar = { idSelecionado = null },
                historico = if (selecionada.id == "2") {
                    listOf(
                        EventoHistoricoUi("aberto", "21 nov., 09:30"),
                        EventoHistoricoUi("em_resolucao", "21 nov., 10:15")
                    )
                } else {
                    emptyList()
                },
                acoes = {
                    if (selecionada.estado == "em_resolucao") {
                        BotoesComprovativo({}, {})
                    }
                }
            )
        }
    }
}

@Composable
internal fun BotoesComprovativo(
    onTirarFoto: () -> Unit,
    onGravarVideo: () -> Unit,
    podeCapturar: Boolean = true
) {
    Text(
        text = stringResource(R.string.operational_proof_title),
        style = MaterialTheme.typography.titleMedium
    )
    OutlinedButton(
        onClick = onTirarFoto,
        modifier = Modifier.fillMaxWidth(),
        enabled = podeCapturar
    ) {
        Text(stringResource(R.string.report_take_photo))
    }
    OutlinedButton(
        onClick = onGravarVideo,
        modifier = Modifier.fillMaxWidth(),
        enabled = podeCapturar
    ) {
        Text(stringResource(R.string.report_record_video))
    }
}
