package com.healthbridge.michel

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM messages on the Michel (controller) device.
 *
 * Expected data payload from topic "alain_location":
 *   lat       – Double as String
 *   lon       – Double as String
 *   accuracy  – Double as String (optional)
 *   timestamp – Long as String (millis)
 */
class MichelFcmService : FirebaseMessagingService() {

    private val TAG = "MichelFcmService"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        Log.d(TAG, "FCM message received from: ${message.from}, data=$data")

        val lat = data["lat"]?.toDoubleOrNull() ?: return
        val lon = data["lon"]?.toDoubleOrNull() ?: return
        val accuracy = data["accuracy"]?.toDoubleOrNull()
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        // The foreground activity listens to Firestore directly, but this FCM handler
        // provides a faster in-band notification path when the app is in the foreground.
        // We post a local notification so the OS also shows an alert when app is in background.
        showLocationNotification(lat, lon, accuracy, timestamp)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Michel does not need to register its token in Firestore (it only subscribes to a topic),
        // but log it for debugging.
        Log.d(TAG, "FCM token refreshed: $token")
    }

    private fun showLocationNotification(
        lat: Double,
        lon: Double,
        accuracy: Double?,
        timestamp: Long
    ) {
        val channelId = "michel_location_channel"
        val manager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create notification channel (no-op if it already exists)
        val channel = android.app.NotificationChannel(
            channelId,
            getString(R.string.notif_channel_name),
            android.app.NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val accStr = accuracy?.let { " ±${it.toInt()}m" } ?: ""
        val body = getString(R.string.notif_location_body, lat, lon, accStr)

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_location_title))
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_location)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
