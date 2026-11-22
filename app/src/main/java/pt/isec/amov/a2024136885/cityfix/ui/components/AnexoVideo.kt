package pt.isec.amov.a2024136885.cityfix.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pt.isec.amov.a2024136885.cityfix.R

@Composable
internal fun AnexoVideo(uri: String, abertoInicialmente: Boolean = false) {
    var aberto by remember(uri) { mutableStateOf(abertoInicialmente) }
    if (aberto) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            factory = { context ->
                VideoView(context).apply {
                    setMediaController(MediaController(context))
                    setOnPreparedListener { it.start() }
                    setVideoURI(Uri.parse(uri))
                }
            },
            onReset = null,
            onRelease = { it.stopPlayback() }
        )
        OutlinedButton(onClick = { aberto = false }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.public_report_close_video))
        }
    } else {
        Button(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.public_report_play_video))
        }
    }
}
