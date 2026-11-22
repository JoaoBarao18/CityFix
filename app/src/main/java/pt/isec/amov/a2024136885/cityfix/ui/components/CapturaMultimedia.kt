package pt.isec.amov.a2024136885.cityfix.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.isec.amov.a2024136885.cityfix.R
import java.io.File
import java.io.IOException
import java.util.UUID

internal class CapturaMultimedia(
    val fotoPath: String?,
    val videoPath: String?,
    val erroFoto: Int?,
    val erroVideo: Int?,
    val aGuardarFoto: Boolean,
    val aGuardarVideo: Boolean,
    val onTirarFoto: () -> Unit,
    val onEscolherGaleria: () -> Unit,
    val onGravarVideo: () -> Unit
) {
    val aGuardar: Boolean get() = aGuardarFoto || aGuardarVideo
}

@Composable
internal fun rememberCapturaMultimedia(): CapturaMultimedia {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var aGuardarFoto by remember { mutableStateOf(false) }
    var fotoPath by rememberSaveable() { mutableStateOf<String?>(null) }
    var erroFoto by rememberSaveable() { mutableStateOf<Int?>(null) }
    var videoPath by rememberSaveable() { mutableStateOf<String?>(null) }
    var erroVideo by rememberSaveable() { mutableStateOf<Int?>(null) }
    var permissaoParaVideo by rememberSaveable() { mutableStateOf(false) }
    var aGuardarVideo by remember() { mutableStateOf(false) }
    var showGaleriaPermissionDialog by remember { mutableStateOf(false) }

    // Mesmo contrato do exemplo AMOVStorageDemo: devolve uma fotografia em miniatura.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            aGuardarFoto = true
            scope.launch {
                try {
                    val ficheiro = withContext(Dispatchers.IO) { guardarImagemFromUri(context, uri) }
                    fotoPath = ficheiro
                    erroFoto = null
                } catch (e: Exception) {
                    Log.e("CityFixFoto", "Falha ao ler foto da galeria", e)
                    erroFoto = R.string.report_photo_error
                } finally {
                    aGuardarFoto = false
                }
            }
        }
    }

    val permissaoGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            galleryLauncher.launch("image/*")
        } else {
            showGaleriaPermissionDialog = true
        }
    }

    if (showGaleriaPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showGaleriaPermissionDialog = false },
            title = { Text("Permissão da Galeria Necessária") },
            text = { Text("Para selecionar fotografias para o relatório de ocorrência, a aplicação necessita da permissão de acesso às imagens do dispositivo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGaleriaPermissionDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Abrir Definições")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGaleriaPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            aGuardarFoto = true
            scope.launch {
                val ficheiro = withContext(Dispatchers.IO) { guardarFotografia(context, bitmap) }
                erroFoto = if (ficheiro == null) R.string.report_photo_error else null
                if (ficheiro != null) {
                    fotoPath = ficheiro
                }
                aGuardarFoto = false
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
                        videoPath = withContext(Dispatchers.IO) { guardarVideo(context, uri) }
                        erroVideo = null
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

    return CapturaMultimedia(
        fotoPath = fotoPath,
        videoPath = videoPath,
        erroFoto = erroFoto,
        erroVideo = erroVideo,
        aGuardarFoto = aGuardarFoto,
        aGuardarVideo = aGuardarVideo,
        onTirarFoto = {
            erroFoto = null
            pedirCamera(paraVideo = false)
        },
        onEscolherGaleria = {
            erroFoto = null
            val permissao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val concedida = ContextCompat.checkSelfPermission(context, permissao) ==
                PackageManager.PERMISSION_GRANTED

            if (concedida) {
                galleryLauncher.launch("image/*")
            } else {
                permissaoGaleria.launch(permissao)
            }
        },
        onGravarVideo = {
            erroVideo = null
            pedirCamera(paraVideo = true)
        }
    )
}

private fun guardarImagemFromUri(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val ficheiro = File(context.filesDir, "foto_${UUID.randomUUID()}.jpg")
    resolver.openInputStream(uri)?.use { input ->
        ficheiro.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IOException("Não foi possível abrir a imagem da galeria.")
    return ficheiro.absolutePath
}

// Baseado no saveImageToInternalStorage do AMOVStorageDemo.
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
