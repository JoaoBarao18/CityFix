package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Categoria
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import java.io.File
import java.io.IOException
import java.util.UUID

@Composable
fun CriarOcrrencia(listaCategoria: List<Categoria>) {
    var descricao by rememberSaveable() { mutableStateOf("") }
    var categoriaID by rememberSaveable() { mutableStateOf<String?>(null)}
    var situacao by rememberSaveable() { mutableStateOf<String?>(null)}
    var publica by rememberSaveable() {mutableStateOf(true)}
    var fotoPath by rememberSaveable() { mutableStateOf<String?>(null) }
    var erroFoto by rememberSaveable() { mutableStateOf<Int?>(null) }
    var videoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var erroVideo by rememberSaveable { mutableStateOf<Int?>(null) }
    var permissaoParaVideo by rememberSaveable { mutableStateOf(false) }
    var aGuardarVideo by remember { mutableStateOf(false) }
    var lat by rememberSaveable() {mutableStateOf<Double?>(null) }
    var long by rememberSaveable() {mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()
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

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val uri = resultado.data?.data
            if (uri == null) {
                erroVideo = R.string.report_video_error
            } else {
                aGuardarVideo = true
                scope.launch {
                    try {
                        videoPath = withContext(Dispatchers.IO) {
                            guardarVideo(context, uri)
                        }
                    } catch (erro: IOException) {
                        Log.e("CityFixVideo", "Falha ao guardar o vídeo", erro)
                        erroVideo = R.string.report_video_error
                    } catch (erro: SecurityException) {
                        Log.e("CityFixVideo", "Falha ao ler o vídeo", erro)
                        erroVideo = R.string.report_video_error
                    } finally {
                        aGuardarVideo = false
                    }
                }
            }
        }
    }

    val abrirCameraVideo = {
        try {
            videoLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
        } catch (erro: ActivityNotFoundException) {
            erroVideo = R.string.report_video_error
        } catch (erro: SecurityException) {
            erroVideo = R.string.report_camera_permission_denied
        }
    }

    val permissaoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        when {
            concedida && permissaoParaVideo -> abrirCameraVideo()
            concedida -> abrirCamera()
            permissaoParaVideo -> erroVideo = R.string.report_camera_permission_denied
            else -> erroFoto = R.string.report_camera_permission_denied
        }
    }

    fun pedirCamera(paraVideo: Boolean) {
        permissaoParaVideo = paraVideo
        val concedida = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        when {
            !concedida -> permissaoCamera.launch(Manifest.permission.CAMERA)
            paraVideo -> abrirCameraVideo()
            else -> abrirCamera()
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
        fotoPath = fotoPath,
        erroFoto = erroFoto,
        localRegis = lat !=null && long != null,
        onLocalizacao = {newLat, newLong ->
            lat = newLat
            long = newLong
        },
        onTirarFoto = {
            erroFoto = null
            pedirCamera(paraVideo = false)
        },
        videoPath = videoPath,
        erroVideo = erroVideo,
        aGuardarVideo = aGuardarVideo,
        onGravarVideo = {
            erroVideo = null
            pedirCamera(paraVideo = true)
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
    localRegis: Boolean,
    onLocalizacao: (Double, Double) -> Unit,
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
            if (erroLocalizacao != null) {
                Text(
                    text = stringResource(erroLocalizacao),
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
            }
        }
    }
}

// based no saveImageToInternalStorage do AMOVStorageDemo.
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

private fun guardarVideo(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val extensao = resolver.getType(uri)
        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        ?: "video"
    val ficheiro = File(context.filesDir, "video_${UUID.randomUUID()}.$extensao")

    var guardado = false
    try {
        val entrada = resolver.openInputStream(uri)
            ?: throw IOException("Não foi possível abrir o vídeo.")
        val bytes = entrada.use { origem ->
            ficheiro.outputStream().use { destino -> origem.copyTo(destino) }
        }
        if (bytes == 0L) {
            throw IOException("O ficheiro de vídeo está vazio.")
        }
        guardado = true
        return ficheiro.absolutePath
    } finally {
        if (!guardado) {
            ficheiro.delete()
        }
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
            ),
        )
    }
}
