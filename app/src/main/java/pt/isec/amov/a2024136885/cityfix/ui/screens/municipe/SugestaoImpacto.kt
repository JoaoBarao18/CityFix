package pt.isec.amov.a2024136885.cityfix.ui.screens.municipe

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.domain.sensor.SensorImpactoManager

@Composable
internal fun SugestaoImpacto(
    detImpactosAtiva: Boolean,
    onAbrirOcorrencia: () -> Unit
) {
    if (!detImpactosAtiva || LocalInspectionMode.current) return

    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mostrarSugestao by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(context, lifecycle) {
        val detetor = SensorImpactoManager(context) { mostrarSugestao = true }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> detetor.start()
                Lifecycle.Event.ON_STOP -> detetor.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            detetor.stop()
        }
    }

    if (mostrarSugestao) {
        AlertDialog(
            onDismissRequest = { mostrarSugestao = false },
            title = { Text(stringResource(R.string.impact_detection_title)) },
            text = { Text(stringResource(R.string.impact_detection_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarSugestao = false
                        onAbrirOcorrencia()
                    }
                ) {
                    Text(stringResource(R.string.impact_detection_open_report))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSugestao = false }) {
                    Text(stringResource(R.string.impact_detection_dismiss))
                }
            }
        )
    }
}
