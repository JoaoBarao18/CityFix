package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import pt.isec.amov.a2024136885.cityfix.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme

@Composable
fun ListaOcorrenciasPublicas(ocorrencias: List<OcorrenciaPublicaUi>){
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.public_reports_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(ocorrencias){
            ocorrencia -> Ocorrencia(
                situation = ocorrencia.situation,
                categoria = ocorrencia.categoria,
                status = ocorrencia.status,
                pathImg = ocorrencia.pathImg
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListarOcorrenciasPublicasContentePreview() {
    CityFIxTheme {
        ListaOcorrenciasPublicas(
            ocorrencias = listOf(
                OcorrenciaPublicaUi(
                    categoria = "Iluminação pública",
                    situation = "Avaria na lâmpada",
                    status = "Aberto"
                ),
                OcorrenciaPublicaUi(
                    categoria = "Vias públicas",
                    situation = "Buraco na estrada",
                    status = "Em resolução"
                ),
                OcorrenciaPublicaUi(
                    categoria = "Espaços Verdes",
                    situation = "Árvore caída",
                    status = "Concluído"
                )
            )
        )
    }
}

