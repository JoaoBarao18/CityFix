package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.User
import pt.isec.amov.a2024136885.cityfix.data.repository.AnexoPublicado
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaPublicada
import pt.isec.amov.a2024136885.cityfix.ui.components.AnexoVideo
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.PainelPublicoViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.estadosOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.situacoesOcorrencia
import java.text.NumberFormat

@Composable
internal fun PainelPublicoRoute(
    vm: PainelPublicoViewModel,
    utilizador: User?,
    onVerTodas: () -> Unit,
    onAbrir: (String) -> Unit,
    onCriar: (() -> Unit)?,
    onMinhas: (() -> Unit)?,
    onPerfil: () -> Unit,
    onVoltarAoInicio: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val dados = estado.dados
    val stats = dados?.estatisticas
    val idioma = LocalConfiguration.current.locales[0]
    val media = stats?.mediaDias?.let { dias ->
        val numero = NumberFormat.getNumberInstance(idioma).apply { maximumFractionDigits = 1 }.format(dias)
        pluralStringResource(R.plurals.public_dashboard_days, if (dias == 1.0) 1 else 2, numero)
    } ?: "—"

    PainelPublicoContent(
        ocorrencias = dados?.ocorrencias.orEmpty().map { it.paraUi() },
        currentUser = utilizador,
        onPerfilClick = onPerfil,
        onLogoutClick = onVoltarAoInicio,
        onVerTodas = onVerTodas,
        onAbrirOcorrencia = onAbrir,
        onCriarOcorrencia = onCriar,
        onMinhasOcorrencias = onMinhas,
        mediaReparacao = media,
        totalOcorrencias = stats?.let { NumberFormat.getIntegerInstance(idioma).format(it.total) } ?: "—",
        percentagemAtivas = stats?.let { NumberFormat.getPercentInstance(idioma).format(it.fracaoAtiva) } ?: "—",
        aCarregar = dados == null && !estado.erro,
        onTentarNovamente = (vm::tentarNovamente).takeIf { estado.erro },
        mostrarNotaVisitante = utilizador == null,
        carregarMiniatura = vm::urlAnexo
    )
}

@Composable
internal fun ListaPublicaRoute(vm: PainelPublicoViewModel, onAbrir: (String) -> Unit, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    ListaOcorrenciasPublicas(
        ocorrencias = estado.dados?.ocorrencias.orEmpty().map { it.paraUi() },
        onAbrirOcorrencia = onAbrir,
        onVoltar = onVoltar,
        aCarregar = estado.dados == null && !estado.erro,
        onTentarNovamente = (vm::tentarNovamente).takeIf { estado.erro },
        carregarMiniatura = vm::urlAnexo
    )
}

@Composable
internal fun DetalhePublicoRoute(vm: PainelPublicoViewModel, id: String, onVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val publicada = estado.dados?.ocorrencias?.firstOrNull { it.id == id }
    if (publicada == null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onVoltar) { Text(stringResource(R.string.public_report_back)) }
            if (estado.dados != null) {
                Text(stringResource(R.string.public_report_unavailable))
            } else {
                EstadoListaPublica(!estado.erro, (vm::tentarNovamente).takeIf { estado.erro })
            }
        }
        return
    }

    val ui = publicada.paraUi()
    DetalheOcorrenciaPublicaContent(
        categoria = ui.categoria,
        situacao = ui.situation,
        status = ui.status,
        descricao = "",
        latitudeAproximada = ui.latitude,
        longitudeAproximada = ui.longitude,
        onVoltar = onVoltar,
        anexosPublicados = {
            publicada.anexos.forEach { AnexoPublico(it, vm) }
        }
    )
}

@Composable
internal fun EstadoListaPublica(aCarregar: Boolean, onTentarNovamente: (() -> Unit)?) {
    when {
        aCarregar -> CircularProgressIndicator()
        onTentarNovamente != null -> Column {
            Text(stringResource(R.string.public_reports_load_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onTentarNovamente) { Text(stringResource(R.string.operational_retry)) }
        }
        else -> Text(
            stringResource(R.string.public_reports_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OcorrenciaPublicada.paraUi() = OcorrenciaPublicaUi(
    id = id,
    categoria = categoria.ifBlank { stringResource(R.string.report_category) },
    situation = situacoesOcorrencia[situacao]?.let { stringResource(it) } ?: situacao,
    status = estadosOcorrencia[estado]?.let { stringResource(it) } ?: estado,
    descricao = "",
    latitude = latitudeAproximada,
    longitude = longitudeAproximada,
    miniaturaStorage = miniatura
)

@Composable
private fun AnexoPublico(anexo: AnexoPublicado, vm: PainelPublicoViewModel) {
    val resultado by produceState<Result<String>?>(null, anexo.caminho) {
        val url = vm.urlAnexo(anexo.caminho)
        value = if (url != null) Result.success(url) else Result.failure(IllegalStateException())
    }
    val url = resultado?.getOrNull()
    when {
        resultado == null -> CircularProgressIndicator()
        url == null -> Text(stringResource(R.string.operational_attachment_error))
        anexo.tipo == "video" -> AnexoVideo(url)
        else -> FotoOcorrencia(url)
    }
}
