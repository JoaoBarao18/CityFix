package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.Ocorrencia
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov.a2024136885.cityfix.R
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.prazoDecorrido
import pt.isec.amov.a2024136885.cityfix.ui.components.AvisoProximidade
import pt.isec.amov.a2024136885.cityfix.ui.components.ChatOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.DetalheOcorrenciaPublicaContent
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.MinhaOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.MinhasOcorrenciasViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.estadosOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.situacoesOcorrencia
import kotlin.math.round

data class OcorrnenciaPessoalUI(
    val id: String,
    val situacao: String,
    val categoria: String,
    val status: String,
    val pathImg: String? = null,
    val emGrupo: Boolean = false,
    val temAvisoProximidade: Boolean = false
)

@Composable
fun minhasOcorrenciasContent(
    ocorrencias: List<OcorrnenciaPessoalUI>,
    onAbrirOcorencia: (String) -> Unit,
    onVoltar: (() -> Unit)? = null,
    aCarregar: Boolean = false,
    onTentarNovamente: (() -> Unit)? = null
) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CabecalhoMinhasOcorrencias(onVoltar)
        }

        when {
            aCarregar -> item { CircularProgressIndicator() }
            onTentarNovamente != null -> item { ErroMinhasOcorrencias(onTentarNovamente) }
            ocorrencias.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.my_reports_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(ocorrencias, key = {it.id}){ ocorrencia ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Ocorrencia(
                    situation = ocorrencia.situacao,
                    categoria = ocorrencia.categoria,
                    status = ocorrencia.status,
                    pathImg = ocorrencia.pathImg,
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                        .clickable(role = Role.Button){
                            onAbrirOcorencia(ocorrencia.id)
                        }
                )
                if (ocorrencia.temAvisoProximidade) AvisoProximidade()
                if(ocorrencia.emGrupo){
                    Text(
                        text = stringResource(R.string.my_reports_group),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun MinhasOcorrenciasRoute(
    autorId: String,
    onAbrir: (String) -> Unit,
    onVoltar: () -> Unit
) {
    val vm: MinhasOcorrenciasViewModel = viewModel()
    var tentativa by remember { mutableIntStateOf(0) }
    val resultado by produceState<Result<List<MinhaOcorrencia>>?>(null, autorId, tentativa) {
        value = null
        vm.observar(autorId).collect { value = it }
    }
    val ocorrencias = resultado?.getOrNull().orEmpty().map { minha ->
        OcorrnenciaPessoalUI(
            id = minha.dados.id,
            situacao = situacaoPessoal(minha),
            categoria = categoriaPessoal(minha),
            status = estadoPessoal(minha),
            pathImg = minha.imagem,
            emGrupo = minha.emGrupo,
            temAvisoProximidade = minha.dados.estado == "em_resolucao" && minha.dados.proximidadeEm != null
        )
    }
    minhasOcorrenciasContent(
        ocorrencias = ocorrencias,
        onAbrirOcorencia = onAbrir,
        onVoltar = onVoltar,
        aCarregar = resultado == null,
        onTentarNovamente = if (resultado?.isFailure == true) ({ tentativa++ }) else null
    )
}

@Composable
internal fun DetalheMinhaOcorrenciaRoute(autorId: String, id: String, onVoltar: () -> Unit) {
    val vm: MinhasOcorrenciasViewModel = viewModel()
    var tentativa by remember { mutableIntStateOf(0) }
    val resultado by produceState<Result<MinhaOcorrencia>?>(null, autorId, id, tentativa) {
        value = null
        vm.observarDetalhe(autorId, id).collect { value = it }
    }
    val minha = resultado?.getOrNull()
    if (minha != null) {
        val dados = minha.dados
        DetalheOcorrenciaPublicaContent(
            categoria = categoriaPessoal(minha),
            situacao = situacaoPessoal(minha),
            status = estadoPessoal(minha),
            descricao = dados.descricao,
            latitudeAproximada = dados.latitude?.let { round(it * 10) / 10 },
            longitudeAproximada = dados.longitude?.let { round(it * 10) / 10 },
            pathImg = minha.imagem,
            pathVideo = minha.video,
            onVoltar = onVoltar,
            mostrarAvisoProximidade = dados.estado == "em_resolucao" && dados.proximidadeEm != null,
            conteudoAdicional = {
                val ultrapassouPrazo = dados.prazoDecorrido()
                ReabrirOcorrencia(
                    podeReabrir = dados.estado == OcorrenciaRepository.ESTADO_RESOLVIDO &&
                        dados.resolvidoEm != null && !ultrapassouPrazo,
                    id = id,
                    viewModel = vm
                )
                ChatOcorrencia(
                    id = id,
                    encerrada = dados.estado == OcorrenciaRepository.ESTADO_ENCERRADO || ultrapassouPrazo
                )
            }
        )
    } else {
        minhasOcorrenciasContent(
            ocorrencias = emptyList(),
            onAbrirOcorencia = {},
            onVoltar = onVoltar,
            aCarregar = resultado == null,
            onTentarNovamente = if (resultado?.isFailure == true) ({ tentativa++ }) else null
        )
    }
}

@Composable
private fun ReabrirOcorrencia(podeReabrir: Boolean, id: String, viewModel: MinhasOcorrenciasViewModel) {
    var aberto by rememberSaveable(id) { mutableStateOf(false) }
    var justificacao by rememberSaveable(id) { mutableStateOf("") }
    val estado by viewModel.reabertura.collectAsStateWithLifecycle()

    LaunchedEffect(podeReabrir) {
        if (!podeReabrir) {
            aberto = false
            justificacao = ""
        }
    }

    if (!podeReabrir) return
    OutlinedButton(
        onClick = {
            viewModel.limparErroReabertura()
            aberto = true
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.report_reopen)) }

    if (aberto) {
        AlertDialog(
            onDismissRequest = { if (!estado.aReabrir) aberto = false },
            title = { Text(stringResource(R.string.report_reopen)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = justificacao,
                        onValueChange = { justificacao = it },
                        label = { Text(stringResource(R.string.report_reopen_reason)) },
                        enabled = !estado.aReabrir,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (estado.erro) {
                        Text(stringResource(R.string.report_reopen_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.reabrir(id, justificacao) },
                    enabled = !estado.aReabrir && justificacao.trim().length >= 10
                ) { Text(stringResource(R.string.report_reopen)) }
            },
            dismissButton = {
                TextButton(onClick = { aberto = false }, enabled = !estado.aReabrir) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun CabecalhoMinhasOcorrencias(onVoltar: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onVoltar != null) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.public_report_back))
            }
        }
        Text(
            text = stringResource(R.string.my_reports_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErroMinhasOcorrencias(onTentarNovamente: () -> Unit) {
    Column {
        Text(stringResource(R.string.my_reports_load_error), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onTentarNovamente) { Text(stringResource(R.string.operational_retry)) }
    }
}

@Composable
private fun situacaoPessoal(minha: MinhaOcorrencia) =
    situacoesOcorrencia[minha.dados.situacao]?.let { stringResource(it) } ?: minha.dados.situacao

@Composable
private fun estadoPessoal(minha: MinhaOcorrencia) =
    estadosOcorrencia[minha.dados.estado]?.let { stringResource(it) } ?: minha.dados.estado

@Composable
private fun categoriaPessoal(minha: MinhaOcorrencia) =
    minha.categoria ?: stringResource(R.string.report_category)

@Preview(showBackground = true, locale = "pt-rPT")
@Composable
private fun MinhasOcorrenciasPreview() {
    CityFIxTheme(dynamicColor = false) {
        minhasOcorrenciasContent(
            ocorrencias = listOf(
                OcorrnenciaPessoalUI(
                    id = "1",
                    situacao = "Avaria",
                    categoria = "Iluminação pública",
                    status = "Aberto"
                ),
                OcorrnenciaPessoalUI(
                    id = "2",
                    situacao = "Dano",
                    categoria = "Vias públicas",
                    status = "Em resolução",
                    emGrupo = true
                )
            ),
            onAbrirOcorencia = {}
        )
    }
}
