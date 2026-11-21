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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CriarOcorrenciaViewModel

@Composable
internal fun CriarOcorrenciaRoute() {
    val formularioViewModel: CriarOcorrenciaViewModel = viewModel()
    val resultadoCategorias by produceState<Result<List<Category>>?>(
        initialValue = null,
        key1 = formularioViewModel
    ) {
        value = formularioViewModel.carregarCategoriasAtivas()
    }
    val categorias = resultadoCategorias?.getOrNull()
    var mensagemSubmissao by rememberSaveable { mutableStateOf<Int?>(null) }

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
                onSubmeter = { mensagemSubmissao = R.string.report_submission_unavailable },
                mensagemSubmissao = mensagemSubmissao
            )
        }
    }
}
