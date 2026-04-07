package com.healthbridge.alain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Foreground service that reads GPS every [LOCATION_INTERVAL_MS] milliseconds
 * (60 seconds by default) using [FusedLocationProviderClient] and writes the
 * result to Firestore at:
 *
 *   devices/alain/location/latest
 *
 * A Firebase Cloud Function is triggered by that write and broadcasts the
 * location as an FCM data message to the "alain_location" topic so that all
 * Michel devices receive it.
 *
 * Lifecycle:
 *   AlainFcmService receives FCM command → starts/stops this service via Intent
 *   AlainMainActivity provides manual Start/Stop override
 */
class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()

    private val TAG = "LocationFgService"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_tracking_start)))
        requestLocationUpdates()
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Location callbacks ───────────────────────────────────────────────────

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                Log.d(TAG, "Location update: ${loc.latitude}, ${loc.longitude}")

                updateNotification(
                    getString(R.string.notif_tracking_body, loc.latitude, loc.longitude)
                )
                writeLocationToFirestore(loc.latitude, loc.longitude, loc.accuracy.toDouble())
            }
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted: ${e.message}")
            stopSelf()
        }
    }

    // ── Firestore write ──────────────────────────────────────────────────────

    private fun writeLocationToFirestore(lat: Double, lon: Double, accuracy: Double) {
        val data = hashMapOf(
            "lat" to lat,
            "lon" to lon,
            "accuracy" to accuracy,
            "timestamp" to System.currentTimeMillis(),
            "deviceId" to ALAIN_DEVICE_ID
        )

        db.collection(COLLECTION_DEVICES)
            .document(ALAIN_DEVICE_ID)
            .collection(COLLECTION_LOCATION)
            .document(DOCUMENT_LATEST)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Location written to Firestore: $lat, $lon")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore write failed: ${e.message}")
            }
    }

    // ── Notification helpers ─────────────────────────────────────────────────

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_tracking_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "alain_location_channel"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_INTERVAL_MS = 60_000L  // 60 seconds

        const val COLLECTION_DEVICES = "devices"
        const val COLLECTION_LOCATION = "location"
        const val DOCUMENT_LATEST = "latest"
        const val ALAIN_DEVICE_ID = "alain"

        /** Updated by onStartCommand / onDestroy so the UI can reflect current state. */
        @Volatile
        var isRunning = false
            private set
    }
}
