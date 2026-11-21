package pt.isec.amov.a2024136885.cityfix.domain.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val evt = GeofencingEvent.fromIntent(intent) ?: return

        if(evt.hasError()){
            Log.w("CityFixGeofence", "Geofencing error: ${evt.errorCode}")
            return
        }

        if(evt.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        evt.triggeringGeofences.orEmpty().forEach { geofence ->
            Log.d("CityFixGeofence", "Entrada no perímetro da ocorrência ${geofence.requestId}")
        }
    }
}
