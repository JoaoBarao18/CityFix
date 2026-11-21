package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.Ocorrencia
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pt.isec.amov.a2024136885.cityfix.R
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme

data class OcorrnenciaPessoalUI(
    val id: String,
    val situacao: String,
    val categoria: String,
    val status: String,
    val pathImg: String? = null,
    val emGrupo: Boolean = false
)

@Composable
fun minhasOcorrenciasContent(
    ocorrencias: List<OcorrnenciaPessoalUI>,
    onAbrirOcorencia: (String) -> Unit
) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item{
            Text(
                text = stringResource(R.string.my_reports_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if(ocorrencias.isEmpty()){
            item {
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
