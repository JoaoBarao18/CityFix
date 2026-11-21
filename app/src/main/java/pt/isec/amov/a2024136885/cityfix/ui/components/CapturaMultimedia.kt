package pt.isec.amov.a2024136885.cityfix.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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

    // Mesmo contrato do exemplo AMOVStorageDemo: devolve uma fotografia em miniatura.
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
        onGravarVideo = {
            erroVideo = null
            pedirCamera(paraVideo = true)
        }
    )
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
