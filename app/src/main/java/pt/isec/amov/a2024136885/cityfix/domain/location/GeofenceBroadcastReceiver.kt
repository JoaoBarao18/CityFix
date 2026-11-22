package pt.isec.amov.a2024136885.cityfix.domain.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val operadorId = intent.data?.takeIf { it.scheme == ESQUEMA_GEOFENCE }?.schemeSpecificPart
            ?.takeIf { it.isNotBlank() } ?: return
        val evento = GeofencingEvent.fromIntent(intent) ?: return

        if (evento.hasError()) {
            Log.w("CityFixGeofence", "Erro na geofence: ${evento.errorCode}")
            return
        }
        if (evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val instante = evento.triggeringLocation?.time?.takeIf { it > 0 } ?: System.currentTimeMillis()
        val pedidos = evento.triggeringGeofences.orEmpty().distinctBy { it.requestId }.map { geofence ->
            EnviarEntradaProximidadeWorker.pedido(operadorId, geofence.requestId, instante)
        }
        if (pedidos.isEmpty()) return

        val pendente = goAsync()
        try {
            val resultado = WorkManager.getInstance(context).enqueue(pedidos).result
            resultado.addListener({
                try {
                    resultado.get()
                } catch (erro: Exception) {
                    Log.w("CityFixGeofence", "Falha ao agendar o alerta de proximidade", erro)
                } finally {
                    pendente.finish()
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (erro: Exception) {
            Log.w("CityFixGeofence", "Falha ao agendar o alerta de proximidade", erro)
            pendente.finish()
        }
    }
}

internal const val ESQUEMA_GEOFENCE = "cityfix-geofence"
