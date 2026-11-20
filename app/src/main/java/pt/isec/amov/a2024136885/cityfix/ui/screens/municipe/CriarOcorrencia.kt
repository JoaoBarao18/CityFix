package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Categoria
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import java.io.File
import java.io.IOException
import java.util.UUID

@Composable
fun CriarOcrrencia(listaCategoria: List<Categoria>){
    var descricao by rememberSaveable() { mutableStateOf("") }
    var categoriaID by rememberSaveable() { mutableStateOf<String?>(null)}
    var situacao by rememberSaveable() { mutableStateOf<String?>(null)}
    var publica by rememberSaveable() {mutableStateOf(true)}
    var fotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var erroFoto by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Mesmo contrato do exemplo AMOVStorageDemo: devolve uma fotografia em miniatura.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val ficheiro = guardarFotografia(context, bitmap)
            erroFoto = if (ficheiro == null) R.string.report_photo_error else null
            if (ficheiro != null) {
                fotoPath = ficheiro
            }
        }
    }

    val abrirCamera = {
        try {
            cameraLauncher.launch(null)
        } catch (error: ActivityNotFoundException) {
            Log.e("CityFixFoto", "Aplicação de câmara indisponível", error)
            erroFoto = R.string.report_photo_error
        } catch (error: SecurityException) {
            Log.e("CityFixFoto", "Permissão de câmara indisponível", error)
            erroFoto = R.string.report_camera_permission_denied
        }
    }
    val permissaoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            abrirCamera()
        } else {
            erroFoto = R.string.report_camera_permission_denied
        }
    }

    CriarOcorrenciaContent(
        descricao = descricao,
        onDescricaoChange = { descricao = it },
        listacategoria = listaCategoria,
        categoriaID = categoriaID,
        onCategoriaChange = {categoriaID = it},
        situacao = situacao,
        onSituacaoChange = {situacao = it},
        publica = publica,
        onPublicaChange = {publica = it},
        fotoPath = fotoPath,
        erroFoto = erroFoto,
        onTirarFoto = {
            erroFoto = null
            if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                abrirCamera()
            } else {
                permissaoCamera.launch(Manifest.permission.CAMERA)
            }
        }
    )
}

@Composable
fun CriarOcorrenciaContent(
    descricao: String,
    onDescricaoChange: (String) -> Unit,
    listacategoria: List<Categoria>,
    categoriaID: String?,
    onCategoriaChange: (String) -> Unit,
    situacao: String?,
    onSituacaoChange: (String) -> Unit,
    publica: Boolean,
    onPublicaChange: (Boolean) -> Unit,
    fotoPath: String?,
    erroFoto: Int?,
    onTirarFoto: () -> Unit,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        //titulo
        Text(
            text = stringResource(R.string.create_report_title),
            style = MaterialTheme.typography.headlineSmall
        )

        CampoCategoria(
            listaCategoria = listacategoria,
            categoriaID = categoriaID,
            onCategoriaChange = onCategoriaChange
        )
        CampoSituacao(
            situacao = situacao,
            onSituacaoChange = onSituacaoChange
        )

        //textdfiedl
        OutlinedTextField(
            value = descricao,
            onValueChange = onDescricaoChange,
            label = {
                Text(stringResource(R.string.report_description))
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Card(modifier = Modifier.fillMaxWidth()){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.report_public),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            if (publica){
                                R.string.report_public_description
                            } else {
                                R.string.report_private_description
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = publica,
                    onCheckedChange = onPublicaChange
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_photo),
                    style = MaterialTheme.typography.titleMedium
                )
                val foto = remember(fotoPath) {
                    fotoPath?.let { BitmapFactory.decodeFile(it) }
                }
                if (foto != null) {
                    Image(
                        bitmap = foto.asImageBitmap(),
                        contentDescription = stringResource(R.string.report_photo_preview),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Fit
                    )
                }
                Button(
                    onClick = onTirarFoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (fotoPath == null) R.string.report_take_photo
                            else R.string.report_replace_photo
                        )
                    )
                }
                if (erroFoto != null) {
                    Text(
                        text = stringResource(erroFoto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Baseado em saveImageToInternalStorage do AMOVStorageDemo.
private fun guardarFotografia(context: Context, bitmap: Bitmap): String? {
    // Um nome por captura evita substituir uma fotografia de outra ocorrência.
    val nome = "foto_${UUID.randomUUID()}.jpg"
    val ficheiro = File(context.filesDir, nome)
    return try {
        val guardada = context.openFileOutput(nome, Context.MODE_PRIVATE).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        if (guardada) {
            ficheiro.absolutePath
        } else {
            ficheiro.delete()
            null
        }
    } catch (error: IOException) {
        Log.e("CityFixFoto", "Falha ao guardar a fotografia", error)
        ficheiro.delete()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoSituacao(
    situacao: String?,
    onSituacaoChange: (String) -> Unit
){
    var aberto by rememberSaveable() { mutableStateOf(false)}
    val opt = mapOf(
        "dano" to stringResource(R.string.report_situation_damage),
        "avaria" to stringResource(R.string.report_situation_breakdown),
        "destruicao" to stringResource(R.string.report_situation_destruction),
        "roubo" to stringResource(R.string.report_situation_theft)
        )

    ExposedDropdownMenuBox(
        expanded = aberto,
        onExpandedChange = {aberto = it}
    ) {
        OutlinedTextField(
            value = opt[situacao.orEmpty()].orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = {Text(stringResource(R.string.report_situation))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = aberto,
            onDismissRequest = {aberto = false}
        ) {
            opt.forEach { opt->
                DropdownMenuItem(
                    text = {Text(opt.value)},
                    onClick = {
                        onSituacaoChange(opt.key)
                        aberto = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoCategoria(
    listaCategoria: List<Categoria>,
    categoriaID: String?,
    onCategoriaChange: (String) -> Unit
){
    var aberto by rememberSaveable() {mutableStateOf(false) }
    val catativas = listaCategoria.filter {it.ativa}
    val selected = catativas.firstOrNull(){
        it.id == categoriaID
    }

    ExposedDropdownMenuBox(
        expanded = aberto,
        onExpandedChange = { aberto = it}
    ) {
        OutlinedTextField(
            value = selected?.nome.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = {Text(stringResource(R.string.report_category))},
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType
                    .PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = aberto,
            onDismissRequest = { aberto = false}
        ) {
            catativas.forEach { listaCategoria ->
                DropdownMenuItem(
                    text = {Text(listaCategoria.nome)},
                    onClick = {
                        onCategoriaChange(listaCategoria.id)
                        aberto = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "pt-rPT", widthDp = 390, heightDp = 844)
@Composable
private fun CriarOcorrenciaPreview() {
    CityFIxTheme(dynamicColor = false) {
        CriarOcrrencia(
            listaCategoria = listOf(
                Categoria("iluminacao", "Iluminação pública", true),
                Categoria("vias", "Vias públicas", true),
                Categoria("agua", "Água", false)
            )
        )
    }
}
