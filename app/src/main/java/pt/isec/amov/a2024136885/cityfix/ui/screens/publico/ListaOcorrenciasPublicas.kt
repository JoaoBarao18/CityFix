package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.a2024136885.cityfix.R

@Composable
fun ListaOcorrenciasPublicas(
    ocorrencias: List<OcorrenciaPublicaUi>,
    onAbrirOcorrencia: (String) -> Unit = {},
    onVoltar: () -> Unit = {},
    aCarregar: Boolean = false,
    onTentarNovamente: (() -> Unit)? = null,
    carregarMiniatura: (suspend (String) -> String?)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.public_report_back)
                )
            }
            Text(
                text = stringResource(R.string.public_reports_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (aCarregar || onTentarNovamente != null || ocorrencias.isEmpty()) {
                item { EstadoListaPublica(aCarregar, onTentarNovamente) }
            }
            items(ocorrencias, key = { it.id }) { ocorrencia ->
                CartaoOcorrenciaPublica(ocorrencia, carregarMiniatura) {
                    onAbrirOcorrencia(ocorrencia.id)
                }
            }
        }
    }
}
