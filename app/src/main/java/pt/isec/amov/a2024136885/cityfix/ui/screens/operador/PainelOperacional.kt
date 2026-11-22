package pt.isec.amov.a2024136885.cityfix.ui.screens.operador

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
                    Card(
                        onClick = { onAbrirOcorrencia(ocorrencia.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(situacao, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = ocorrencia.descricao,
                                style = MaterialTheme.typography.bodyMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                            Text(
                                text = estado,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
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

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        IconButton(onClick = onVoltar) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.public_report_back)
            )
        }

        Text(situacao, style = MaterialTheme.typography.headlineSmall)
        Text(estado, color = MaterialTheme.colorScheme.primary)

        Text(
            stringResource(R.string.report_description),
            style = MaterialTheme.typography.titleMedium
        )
        Text(ocorrencia.descricao)
        anexos()
        Text(
            stringResource(R.string.operational_location_title),
            style = MaterialTheme.typography.titleMedium
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
            }
        )
        Text(
            stringResource(R.string.operational_author_title),
            style = MaterialTheme.typography.titleMedium
        )

        if(contactos.isEmpty()){
            Text(stringResource(R.string.operational_no_contact))
        }else{
            contactos.forEach { contacto -> Text(contacto) }
        }

        Text(
            text = stringResource(R.string.operational_history_title),
            style= MaterialTheme.typography.titleMedium
        )
        if(historico.isEmpty()){
            Text(
                stringResource(R.string.operational_history_empty)
            )
        }

        historico.forEach {
            evt ->
            val evtEstado = estadosOcorrencia[evt.estado]
                ?.let {stringResource(it)} ?: evt.estado

            Column(){
               Text(
                   evtEstado,
                   style = MaterialTheme.typography.labelLarge
               )
                if (evt.dataHora.isNotBlank()) {
                    Text(evt.dataHora, style = MaterialTheme.typography.bodySmall)
                }
                if (evt.nota.isNotBlank()) {
                    Text(evt.nota, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        acoes()
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
