package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.flowOf
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CriarOcorrenciaViewModel

@Composable
internal fun CriarOcorrenciaRoute(autorId: String) {
    val formularioViewModel: CriarOcorrenciaViewModel = viewModel()
    val resultadoCategorias by produceState<Result<List<Category>>?>(
        initialValue = null,
        key1 = formularioViewModel
    ) {
        value = formularioViewModel.carregarCategoriasAtivas()
    }
    val categorias = resultadoCategorias?.getOrNull()
    val context = LocalContext.current.applicationContext
    val id by formularioViewModel.idSubmissao.collectAsStateWithLifecycle()
    val agendamento by formularioViewModel.estadoAgendamento.collectAsStateWithLifecycle()
    val trabalhos by remember(context, id) {
        id?.let { formularioViewModel.observarEnvio(context, it) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val estadoEnvio = trabalhos.firstOrNull()?.state
    val mensagemSubmissao = agendamento ?: when (estadoEnvio) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> R.string.report_submission_queued
        WorkInfo.State.RUNNING -> R.string.report_submission_sending
        WorkInfo.State.SUCCEEDED -> R.string.report_submission_success
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> R.string.report_submission_unavailable
        null -> null
    }
    val podeSubmeter = agendamento != R.string.report_submission_sending &&
        (estadoEnvio == null || estadoEnvio in listOf(WorkInfo.State.FAILED, WorkInfo.State.CANCELLED))

    when {
        resultadoCategorias?.isFailure == true -> Text(
            text = stringResource(R.string.report_categories_load_error),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        categorias == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        else -> Column {
            if (categorias.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.report_categories_empty),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CriarOcrrencia(
                listaCategoria = categorias.orEmpty(),
                onSubmeter = { dados -> formularioViewModel.submeter(context, autorId, dados) },
                mensagemSubmissao = mensagemSubmissao,
                podeSubmeter = podeSubmeter
            )
        }
    }
}
