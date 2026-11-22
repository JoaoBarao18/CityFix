package pt.isec.amov.a2024136885.cityfix.ui.screens.operador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.MediaAttachment
import pt.isec.amov.a2024136885.cityfix.ui.components.AnexoVideo
import pt.isec.amov.a2024136885.cityfix.ui.components.ChatOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.components.rememberCapturaMultimedia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.PainelOperacionalViewModel
import java.io.File
import java.text.DateFormat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import pt.isec.amov.a2024136885.cityfix.data.repository.GuardarResolucaoWorker

@Composable
internal fun PainelOperacionalScreen(
    operadorId: String,
    viewModel: PainelOperacionalViewModel,
    onAbrirOcorrencia: (String) -> Unit,
    onPerfilClick: () -> Unit,
    onValidateUsersClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(operadorId) { viewModel.carregarLista(operadorId) }

    PainelOperacionalContent(
        ocorrencias = estado.ocorrencias,
        onAbrirOcorrencia = onAbrirOcorrencia,
        onAtualizarPainelPublico = { viewModel.atualizarPainelPublico(operadorId) },
        aAtualizarPainelPublico = estado.aAtualizarPainelPublico,
        mensagemPainelPublico = estado.mensagemPainelPublico,
        onPerfilClick = onPerfilClick,
        onValidateUsersClick = onValidateUsersClick,
        onManageCategoriesClick = onManageCategoriesClick,
        onLogoutClick = onLogoutClick,
        aCarregarLista = estado.aCarregarLista,
        onTentarCarregar = if (estado.erroLista) {
            { viewModel.carregarLista(operadorId) }
        } else null
    )
}

@Composable
internal fun DetalheOperacionalScreen(
    operadorId: String,
    id: String,
    viewModel: PainelOperacionalViewModel,
    onVoltar: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(operadorId, id) { viewModel.carregarDetalhe(operadorId, id) }

    val context = LocalContext.current
    val trabalhoResolucao = remember(context, operadorId, id) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(GuardarResolucaoWorker.nome(operadorId, id))
    }
    val trabalhos by trabalhoResolucao.collectAsStateWithLifecycle(initialValue = emptyList())
    val trabalho = trabalhos.firstOrNull()
    val envioPendente = trabalho?.state?.isFinished == false
    val detalhe = estado.detalhe?.takeIf { it.ocorrencia.id == id }
    if (estado.erroDetalhe) {
        ErroAoCarregar { viewModel.carregarDetalhe(operadorId, id) }
        return
    }
    if (detalhe == null || estado.aCarregarDetalhe) {
        ACarregarOcorrencias()
        return
    }

    val captura = rememberCapturaMultimedia()
    val proximidade = rememberAtivarProximidade(id, operadorId, viewModel)
    val ocorrencia = detalhe.ocorrencia
    LaunchedEffect(trabalho?.id, trabalho?.state, ocorrencia.estado) {
        if (trabalho?.state == WorkInfo.State.SUCCEEDED) {
            when {
                ocorrencia.estado == "em_resolucao" ->
                    viewModel.atualizarResolucao(context.applicationContext, operadorId, id)
                ocorrencia.estado == "resolvido" && trabalho.outputData.getBoolean("erroProximidade", false) ->
                    viewModel.removerProximidade(context.applicationContext, id)
            }
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val dataHora = remember(locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    }
    val historico = remember(detalhe.historico, dataHora) {
        detalhe.historico.map { evento ->
            EventoHistoricoUi(
                evento.estado,
                evento.instante?.toDate()?.let(dataHora::format).orEmpty(),
                evento.nota
            )
        }
    }
    BackHandler(enabled = estado.aGuardar) {}

    DetalheOcoorenciaOperadorContent(
        ocorrencia = ocorrencia,
        autor = detalhe.autor,
        onVoltar = { if (!estado.aGuardar) onVoltar() },
        historico = historico,
        anexos = {
            AnexosGuardados(R.string.report_photo, ocorrencia.ficheiros) { caminho ->
                viewModel.urlAnexo(operadorId, caminho)
            }
        },
        acoes = {
            if (ocorrencia.estado == "aberto") {
                OutlinedButton(
                    onClick = { viewModel.pedirInformacoes(operadorId, id) },
                    enabled = !estado.aGuardar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.operational_request_info))
                }
            }
            if (ocorrencia.estado in listOf("aberto", "pedido_informacoes")) {
                Button(
                    onClick = { viewModel.iniciarResolucao(operadorId, id) { proximidade() } },
                    enabled = !estado.aGuardar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.operational_start_resolution))
                }
            }
            if (ocorrencia.estado == "em_resolucao") {
                if (id !in estado.geofencesAtivas) {
                    Text(stringResource(R.string.operational_geofence_permission))
                    OutlinedButton(
                        onClick = proximidade,
                        enabled = !estado.aGuardar && !envioPendente,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.operational_enable_geofence))
                    }
                }
                BotoesComprovativo(
                    onTirarFoto = captura.onTirarFoto,
                    onGravarVideo = captura.onGravarVideo,
                    podeCapturar = !captura.aGuardar && !estado.aGuardar && !envioPendente
                )
                captura.fotoPath?.let { caminho ->
                    AsyncImage(
                        model = File(caminho),
                        contentDescription = stringResource(R.string.operational_proof_photo),
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                captura.videoPath?.let {
                    Text(stringResource(R.string.report_video_attached))
                    AnexoVideo(Uri.fromFile(File(it)).toString())
                }
                listOfNotNull(captura.erroFoto, captura.erroVideo).forEach { erro ->
                    Text(stringResource(erro), color = MaterialTheme.colorScheme.error)
                }
                if (captura.aGuardar) {
                    CircularProgressIndicator()
                }
                Button(
                    onClick = {
                        viewModel.resolver(context.applicationContext, operadorId, id, captura.fotoPath, captura.videoPath)
                    },
                    enabled = !estado.aGuardar && !captura.aGuardar && !envioPendente &&
                        (captura.fotoPath != null || captura.videoPath != null),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(when {
                        trabalho?.state == WorkInfo.State.ENQUEUED -> R.string.operational_queued
                        estado.aGuardar || envioPendente -> R.string.operational_saving
                        else -> R.string.operational_resolve
                    }))
                }
                if (trabalho?.state == WorkInfo.State.FAILED) {
                    Text(stringResource(R.string.operational_resolve_error), color = MaterialTheme.colorScheme.error)
                }
            }
            if (ocorrencia.estado == "resolvido" && estado.mensagemProximidade == R.string.operational_geofence_remove_error) {
                OutlinedButton(onClick = { viewModel.removerProximidade(context.applicationContext, id) }) {
                    Text(stringResource(R.string.operational_remove_geofence))
                }
            }
            estado.erroAcao?.let { erro ->
                Text(stringResource(erro), color = MaterialTheme.colorScheme.error)
            }
            estado.mensagemProximidade?.let { mensagem ->
                Text(stringResource(mensagem), style = MaterialTheme.typography.bodySmall)
            }
            AnexosGuardados(R.string.operational_proof_title, ocorrencia.comprovativosResolucao) { caminho ->
                viewModel.urlAnexo(operadorId, caminho)
            }
            ChatOcorrencia(id = id, encerrada = ocorrencia.estado == "encerrado")
        }
    )
}

@Composable
private fun rememberAtivarProximidade(
    id: String,
    operadorId: String,
    viewModel: PainelOperacionalViewModel
): () -> Unit {
    val context = LocalContext.current
    var permissaoPedida by remember(id) { mutableStateOf(false) }

    fun temPermissao(permissao: String): Boolean =
        ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED

    fun concluir() {
        permissaoPedida = false
        val ocorrencia = viewModel.estado.value.detalhe?.ocorrencia?.takeIf { it.id == id } ?: return
        viewModel.registarProximidade(context.applicationContext, operadorId, ocorrencia)
    }

    val segundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) concluir() else permissaoPedida = false
    }
    val definicoes = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (temPermissao(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) concluir() else permissaoPedida = false
    }

    fun pedirSegundoPlano() {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                temPermissao(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> concluir()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                definicoes.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            else -> segundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val precisa = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true) pedirSegundoPlano() else permissaoPedida = false
    }
    return {
        if (!permissaoPedida) {
            permissaoPedida = true
            if (temPermissao(Manifest.permission.ACCESS_FINE_LOCATION)) {
                pedirSegundoPlano()
            } else {
                precisa.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }
}

@Composable
private fun ACarregarOcorrencias() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErroAoCarregar(onTentarNovamente: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.operational_load_error), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onTentarNovamente) { Text(stringResource(R.string.operational_retry)) }
    }
}

@Composable
private fun AnexosGuardados(
    titulo: Int,
    anexos: List<MediaAttachment>,
    resolverUrl: suspend (String) -> String
) {
    if (anexos.isEmpty()) return
    Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
    anexos.forEach { anexo ->
        var pedido by remember(anexo.storagePath) { mutableStateOf(false) }
        if (pedido) {
            val resultado by produceState<Result<String>?>(null, anexo.storagePath) {
                value = try {
                    Result.success(resolverUrl(anexo.storagePath))
                } catch (erro: Exception) {
                    if (erro is CancellationException) throw erro
                    Result.failure(erro)
                }
            }
            val url = resultado?.getOrNull()
            when {
                resultado?.isFailure == true -> Text(stringResource(R.string.operational_attachment_error))
                url == null -> CircularProgressIndicator()
                anexo.tipo == "video" -> AnexoVideo(url, abertoInicialmente = true)
                else -> AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.public_report_photo),
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            OutlinedButton(onClick = { pedido = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (anexo.tipo == "video") R.string.public_report_play_video else R.string.operational_view_photo))
            }
        }
    }
}
