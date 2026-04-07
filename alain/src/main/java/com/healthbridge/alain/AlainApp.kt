package com.healthbridge.alain

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Application subclass for HealthBridgeAlain.
 *
 * On every app start, retrieves the current FCM token and persists it to
 * Firestore so Michel always has a fresh token to send commands with.
 */
class AlainApp : Application() {

    private val TAG = "AlainApp"

    override fun onCreate() {
        super.onCreate()
        refreshFcmToken()
    }

    private fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token retrieved on startup, persisting to Firestore")
                persistTokenToFirestore(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Could not retrieve FCM token: ${e.message}")
            }
    }

    private fun persistTokenToFirestore(token: String) {
        val data = mapOf("fcmToken" to token)
        FirebaseFirestore.getInstance()
            .collection(AlainFcmService.COLLECTION_DEVICES)
            .document(AlainFcmService.ALAIN_DEVICE_ID)
            .collection(AlainFcmService.COLLECTION_INFO)
            .document(AlainFcmService.DOCUMENT_TOKEN)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "FCM token persisted on startup") }
            .addOnFailureListener { e -> Log.e(TAG, "Firestore write failed: ${e.message}") }
    }
}
