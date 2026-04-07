package com.healthbridge.alain

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles FCM messages on Alain's device.
 *
 * Responsibilities:
 * 1. On token refresh – persist the new FCM token to Firestore so Michel can
 *    find it and send commands.
 * 2. On data message with type "START" – start [LocationForegroundService].
 * 3. On data message with type "STOP"  – stop [LocationForegroundService].
 *
 * Firestore token path: devices/alain/info/token  { fcmToken: "..." }
 *
 * The token is written to the same path that [MichelMainActivity] reads when
 * sending a command.
 */
class AlainFcmService : FirebaseMessagingService() {

    private val TAG = "AlainFcmService"
    private val db = FirebaseFirestore.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed, persisting to Firestore")
        persistToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        Log.d(TAG, "FCM message received: $data")

        when (data["type"]) {
            "START" -> startTracking()
            "STOP"  -> stopTracking()
            else    -> Log.w(TAG, "Unknown command type: ${data["type"]}")
        }
    }

    // ── Token persistence ────────────────────────────────────────────────────

    /**
     * Writes the FCM registration token to Firestore so Michel can target this
     * device with commands.
     *
     * Called from [onNewToken] on token refresh.
     */
    private fun persistToken(token: String) {
        val data = mapOf("fcmToken" to token)
        db.collection(COLLECTION_DEVICES)
            .document(ALAIN_DEVICE_ID)
            .collection(COLLECTION_INFO)
            .document(DOCUMENT_TOKEN)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "FCM token persisted to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to persist FCM token: ${e.message}")
            }
    }

    // ── Service control ──────────────────────────────────────────────────────

    private fun startTracking() {
        Log.d(TAG, "START command received – starting LocationForegroundService")
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopTracking() {
        Log.d(TAG, "STOP command received – stopping LocationForegroundService")
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent)
    }

    companion object {
        const val COLLECTION_DEVICES = "devices"
        const val COLLECTION_INFO = "info"
        const val DOCUMENT_TOKEN = "token"
        const val ALAIN_DEVICE_ID = "alain"
    }
}
