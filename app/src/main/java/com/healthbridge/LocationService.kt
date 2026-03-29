package com.healthbridge

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class LocationService : Service() {

    private val CHANNEL_ID = "location_channel"
    private val NOTIFICATION_ID = 1
    private var gpsTracker: GpsTracker? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Location tracking active..."))
        gpsTracker = GpsTracker(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "Service started")

        serviceScope.launch {
            while (isActive) {
                val lat = gpsTracker?.getLatitude()
                val lon = gpsTracker?.getLongitude()

                if (lat != null && lon != null) {
                    Log.d("LocationService", "Location: $lat, $lon")
                    updateNotification("Lat: $lat, Lon: $lon")
                } else {
                    Log.d("LocationService", "Waiting for GPS...")
                }

                delay(30_000) // update every 30 seconds
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        gpsTracker?.stopUsingGPS()
        Log.d("LocationService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthBridge")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
