package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Categoria


@Composable
fun CriarOcrrencia(listaCategoria: List<Categoria>){
    var descricao by rememberSaveable() { mutableStateOf("") }
    var categoriaID by rememberSaveable() { mutableStateOf<String?>(null)}
    CriarOcorrenciaContent(
        descricao = descricao,
        onDescricaoChange = { descricao = it },
        listacategoria = listaCategoria,
        categoriaID = categoriaID,
        onCategoriaChange = {categoriaID = it}
    )
}

@Composable
fun CriarOcorrenciaContent(
    descricao: String,
    onDescricaoChange: (String) -> Unit,
    listacategoria: List<Categoria>,
    categoriaID: String?,
    onCategoriaChange: (String) -> Unit,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_report_title),
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = descricao,
            onValueChange = onDescricaoChange,
            label = {
                Text(stringResource(R.string.report_description))
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}
