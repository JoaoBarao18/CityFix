package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Looper
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.ui.components.rememberCapturaMultimedia
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CriarOcorrenciaViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.DadosAberturaOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.ErrosCriarOcorrencia
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.situacoesOcorrencia

@Composable
fun CriarOcrrencia(
    listaCategoria: List<Category>,
    onSubmeter: (DadosAberturaOcorrencia) -> Unit,
    mensagemSubmissao: Int? = null,
    podeSubmeter: Boolean = true
) {
    var descricao by rememberSaveable() { mutableStateOf("") }
    var categoriaID by rememberSaveable() { mutableStateOf<String?>(null)}
    var situacao by rememberSaveable() { mutableStateOf<String?>(null)}
    var publica by rememberSaveable() {mutableStateOf(true)}
    val captura = rememberCapturaMultimedia()
    var validacaoPedida by rememberSaveable() { mutableStateOf(false) }
    val formularioViewModel: CriarOcorrenciaViewModel = if (LocalInspectionMode.current) {
        remember() { CriarOcorrenciaViewModel() }
    } else {
        viewModel()
    }
    val context = LocalContext.current

    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var aObterLocalizacao by remember { mutableStateOf(false) }
    var erroLocalizacao by remember { mutableStateOf<Int?>(null) }
    val clienteLocalizacao = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val permissaoLocalizacao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            aObterLocalizacao = true
        } else {
            erroLocalizacao = R.string.report_location_permission
        }
    }

    LaunchedEffect(clienteLocalizacao, aObterLocalizacao) {
        if (!aObterLocalizacao) return@LaunchedEffect

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            erroLocalizacao = R.string.report_location_permission
            aObterLocalizacao = false
            return@LaunchedEffect
        }

        val tempoLimite = 20_000L
        // Uma leitura nova, segundo o exemplo de LocationRequest da aula.
        val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdateAgeMillis(0L)
            .setMaxUpdates(1)
            .setDurationMillis(tempoLimite)
            .build()

        var pedidoAtivo = true
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val localizacao = resultado.lastLocation ?: return
                if (pedidoAtivo && aObterLocalizacao) {
                    latitude = localizacao.latitude
                    longitude = localizacao.longitude
                    aObterLocalizacao = false
                }
            }
        }

        try {
            clienteLocalizacao.requestLocationUpdates(pedido, callback, Looper.getMainLooper())
                .addOnFailureListener { erro ->
                    if (pedidoAtivo && aObterLocalizacao) {
                        Log.e("CityFixLocation", "Falha ao obter a posição da ocorrência", erro)
                        erroLocalizacao = R.string.report_location_error
                        aObterLocalizacao = false
                    }
                }
            delay(tempoLimite)
            if (aObterLocalizacao) {
                erroLocalizacao = R.string.report_location_error
                aObterLocalizacao = false
            }
        } catch (_: SecurityException) {
            erroLocalizacao = R.string.report_location_permission
            aObterLocalizacao = false
        } finally {
            pedidoAtivo = false
            clienteLocalizacao.removeLocationUpdates(callback)
        }
    }

    val validacao = if (validacaoPedida) {
        formularioViewModel.validar(
            categorias = listaCategoria,
            categoriaId = categoriaID,
            situacao = situacao,
            descricao = descricao,
            latitude = latitude,
            longitude = longitude,
            temAnexo = !captura.fotoPath.isNullOrBlank() || !captura.videoPath.isNullOrBlank()
        )
    } else {
        null
    }

    CriarOcorrenciaContent(
        validacao = validacao,
        onSubmeter = {
            validacaoPedida = true
            formularioViewModel.prepararSubmissao(
                context = context,
                categorias = listaCategoria,
                categoriaId = categoriaID,
                situacao = situacao,
                descricao = descricao,
                publica = publica,
                latitude = latitude,
                longitude = longitude,
                fotoPath = fotoPath,
                videoPath = videoPath
            )
            // Simula um callback dummy se for necessário 
            onSubmeter(
                DadosAberturaOcorrencia(
                    categoriaId = categoriaID ?: "",
                    situacao = situacao ?: "",
                    descricao = descricao,
                    publica = publica,
                    latitude = latitude ?: 0.0,
                    longitude = longitude ?: 0.0,
                    fotoPath = fotoPath,
                    videoPath = videoPath
                )
            )
        },
        mensagemSubmissao = mensagemSubmissao,
        podeSubmeter = podeSubmeter,
        descricao = descricao,
        onDescricaoChange = { descricao = it },
        listacategoria = listaCategoria,
        categoriaID = categoriaID,
        onCategoriaChange = {categoriaID = it},
        situacao = situacao,
        onSituacaoChange = {situacao = it},
        publica = publica,
        onPublicaChange = {publica = it},
        localizacaoRegistada = latitude != null && longitude != null,
        aObterLocalizacao = aObterLocalizacao,
        erroLocalizacao = erroLocalizacao,
        onUsarLocalizacao = {
            erroLocalizacao = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                aObterLocalizacao = true
            } else {
                permissaoLocalizacao.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        fotoPath = captura.fotoPath,
        erroFoto = captura.erroFoto,
        onTirarFoto = captura.onTirarFoto,
        videoPath = captura.videoPath,
        erroVideo = captura.erroVideo,
        aGuardarVideo = captura.aGuardar,
        onGravarVideo = captura.onGravarVideo
    )
}

@Composable
fun CriarOcorrenciaContent(
    descricao: String,
    onDescricaoChange: (String) -> Unit,
    listacategoria: List<Category>,
    categoriaID: String?,
    onCategoriaChange: (String) -> Unit,
    situacao: String?,
    onSituacaoChange: (String) -> Unit,
    publica: Boolean,
    onPublicaChange: (Boolean) -> Unit,
    localizacaoRegistada: Boolean,
    aObterLocalizacao: Boolean,
    erroLocalizacao: Int?,
    onUsarLocalizacao: () -> Unit,
    fotoPath: String?,
    erroFoto: Int?,
    onTirarFoto: () -> Unit,
    videoPath: String?,
    erroVideo: Int?,
    aGuardarVideo: Boolean,
    onGravarVideo: () -> Unit,
    validacao: ErrosCriarOcorrencia?,
    onSubmeter: () -> Unit,
    mensagemSubmissao: Int?,
    modifier: Modifier = Modifier,
    podeSubmeter: Boolean = true
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
            onCategoriaChange = onCategoriaChange,
            erro = validacao?.categoria
        )
        CampoSituacao(
            situacao = situacao,
            onSituacaoChange = onSituacaoChange,
            erro = validacao?.situacao
        )

        //textdfiedl
        OutlinedTextField(
            value = descricao,
            onValueChange = onDescricaoChange,
            label = {
                Text(stringResource(R.string.report_description))
            },
            modifier = Modifier.fillMaxWidth(),
            isError = validacao?.descricao != null,
            supportingText = if (validacao?.descricao != null) {
                { Text(stringResource(validacao.descricao)) }
            } else null,
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onUsarLocalizacao,
                enabled = !aObterLocalizacao,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (aObterLocalizacao) R.string.checking_location
                        else R.string.report_use_location
                    )
                )
            }
            if (localizacaoRegistada) {
                Text(
                    text = stringResource(R.string.report_location_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val mensagemLocalizacao = erroLocalizacao ?: validacao?.localizacao
            if (mensagemLocalizacao != null) {
                Text(
                    text = stringResource(mensagemLocalizacao),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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

                OutlinedButton(
                    onClick = onGravarVideo,
                    enabled = !aGuardarVideo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            when {
                                aGuardarVideo -> R.string.report_saving_video
                                videoPath != null -> R.string.report_replace_video
                                else -> R.string.report_record_video
                            }
                        )
                    )
                }
                if (videoPath != null) {
                    Text(
                        text = stringResource(R.string.report_video_attached),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (erroVideo != null) {
                    Text(
                        text = stringResource(erroVideo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                validacao?.anexos?.let { erro ->
                    Text(
                        text = stringResource(erro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (mensagemSubmissao != null) {
            Text(
                text = stringResource(mensagemSubmissao),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = if (mensagemSubmissao == R.string.report_submission_unavailable) {
                    MaterialTheme.colorScheme.error
                } else MaterialTheme.colorScheme.primary
            )
        }
        Button(
            onClick = onSubmeter,
            enabled = podeSubmeter && !aObterLocalizacao && !aGuardarVideo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.report_submit))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoSituacao(
    situacao: String?,
    onSituacaoChange: (String) -> Unit,
    erro: Int?
){
    var aberto by rememberSaveable() { mutableStateOf(false)}
    val textoSelecionado = situacoesOcorrencia[situacao]?.let { stringResource(it) }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = aberto,
        onExpandedChange = {aberto = it}
    ) {
        OutlinedTextField(
            value = textoSelecionado,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = erro != null,
            supportingText = if (erro != null) {
                { Text(stringResource(erro)) }
            } else null,
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
            situacoesOcorrencia.forEach { (codigo, texto) ->
                DropdownMenuItem(
                    text = { Text(stringResource(texto)) },
                    onClick = {
                        onSituacaoChange(codigo)
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
    listaCategoria: List<Category>,
    categoriaID: String?,
    onCategoriaChange: (String) -> Unit,
    erro: Int?
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
            isError = erro != null,
            supportingText = if (erro != null) {
                { Text(stringResource(erro)) }
            } else null,
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
    var dadosRecebidos by rememberSaveable { mutableStateOf(false) }

    CityFIxTheme(dynamicColor = false) {
        CriarOcrrencia(
            listaCategoria = listOf(
                Category("iluminacao", "Iluminação pública", true),
                Category("vias", "Vias públicas", true),
                Category("agua", "Água", false)
            ),
            onSubmeter = { dadosRecebidos = true },
            mensagemSubmissao = if (dadosRecebidos) R.string.report_form_valid else null
        )
    }
}
