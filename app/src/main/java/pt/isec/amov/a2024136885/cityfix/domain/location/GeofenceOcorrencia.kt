package pt.isec.amov.a2024136885.cityfix.domain.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import coil3.Extras
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.serialization.Contextual
import org.maplibre.compose.location.Location

internal fun criarGeofenceOcorrencia(
    ocorrenciaID: String,
    lat: Double,
    long: Double
): Geofence{
    return Geofence.Builder()
        .setRequestId(ocorrenciaID)
        .setCircularRegion(lat,long, 20f)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .build()
}

internal fun registarGeofenceOcorrencia(
        context: Context,
        ocorrenciaID: String,
        lat: Double,
        long: Double
    ): Task<Void> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
        return Tasks.forException(SecurityException("falta localização percisa"))
    }

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ){
        return Tasks.forException(SecurityException("Falta localizacao em segundo plano"))
    }
    val asked = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(criarGeofenceOcorrencia(ocorrenciaID,lat, long))
        .build()

    val flags =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        }else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

    return LocationServices.getGeofencingClient(context).addGeofences(asked, pendingIntent)
}

internal fun removerGeofenceOcorrencia(
    context: Context,
    ocorrenciaID: String
): Task<Void> {
    return LocationServices.getGeofencingClient(context)
        .removeGeofences(listOf(ocorrenciaID))
}