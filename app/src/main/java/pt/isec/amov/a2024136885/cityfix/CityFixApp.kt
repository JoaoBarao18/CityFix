package pt.isec.amov.a2024136885.cityfix

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import org.maplibre.android.MapLibre

class CityFixApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(persistentCacheSettings {})
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings

        MapLibre.getInstance(this)

    }
}