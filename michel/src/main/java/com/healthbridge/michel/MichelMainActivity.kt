package com.healthbridge.michel

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.healthbridge.michel.databinding.ActivityMichelMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Michel's main screen:
 *  - Map showing Alain's last known position and a polyline history.
 *  - Start / Stop tracking buttons that write Firestore commands.
 *  - Status indicator updated when FCM location messages arrive.
 *
 * Data flow:
 *   Michel writes  →  Firestore devices/alain/commands/{id}
 *   Cloud Function →  sends FCM data message to Alain's token
 *   Alain's service →  writes Firestore devices/alain/location/latest
 *   Cloud Function →  broadcasts FCM to topic "alain_location"
 *   MichelFcmService receives FCM →  broadcasts local intent
 *   This activity receives local broadcast →  updates map
 */
class MichelMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMichelMainBinding
    private val db = FirebaseFirestore.getInstance()

    private var currentMarker: Marker? = null
    private val polyline = Polyline()
    private val locationHistory = mutableListOf<GeoPoint>()

    private val TAG = "MichelMainActivity"

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid requires a user-agent; use the app package name
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityMichelMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()
        setupMap()
        subscribeToLocationTopic()
        listenForLocationUpdates()

        binding.btnStartTracking.setOnClickListener { sendCommand("START") }
        binding.btnStopTracking.setOnClickListener { sendCommand("STOP") }
    }

    // ── Map setup ────────────────────────────────────────────────────────────

    private fun setupMap() {
        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // Default centre (Paris) until we get a real location
            controller.setCenter(GeoPoint(48.8566, 2.3522))
        }

        polyline.outlinePaint.color = Color.BLUE
        polyline.outlinePaint.strokeWidth = 5f
        binding.map.overlays.add(polyline)
    }

    // ── FCM topic subscription ───────────────────────────────────────────────

    private fun subscribeToLocationTopic() {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(FCM_TOPIC_LOCATION)
            .addOnSuccessListener {
                Log.d(TAG, "Subscribed to topic: $FCM_TOPIC_LOCATION")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Topic subscription failed: ${e.message}")
            }
    }

    // ── Firestore real-time listener for location updates ────────────────────

    private fun listenForLocationUpdates() {
        db.collection(COLLECTION_DEVICES)
            .document(ALAIN_DEVICE_ID)
            .collection(COLLECTION_LOCATION)
            .document(DOCUMENT_LATEST)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Firestore listen error: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("lat") ?: return@addSnapshotListener
                    val lon = snapshot.getDouble("lon") ?: return@addSnapshotListener
                    val accuracy = snapshot.getDouble("accuracy")
                    val ts = snapshot.getLong("timestamp") ?: System.currentTimeMillis()
                    updateMap(lat, lon, accuracy, ts)
                }
            }
    }

    // ── Map update ───────────────────────────────────────────────────────────

    private fun updateMap(lat: Double, lon: Double, accuracy: Double?, timestamp: Long) {
        val point = GeoPoint(lat, lon)

        // Update or create marker
        if (currentMarker == null) {
            currentMarker = Marker(binding.map).apply {
                title = "Alain"
                binding.map.overlays.add(this)
            }
        }
        currentMarker?.position = point

        // Update polyline
        locationHistory.add(point)
        polyline.setPoints(locationHistory)

        // Pan map to new location
        binding.map.controller.animateTo(point)
        binding.map.invalidate()

        // Update status text
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date(timestamp))
        val accStr = accuracy?.let { " ±${it.toInt()}m" } ?: ""
        binding.tvStatus.text = getString(R.string.status_location, lat, lon, timeStr, accStr)
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(this, R.color.status_tracking)
        )
    }

    // ── Send START / STOP command ────────────────────────────────────────────

    private fun sendCommand(type: String) {
        lifecycleScope.launch {
            try {
                // Fetch Alain's current FCM token from Firestore
                val infoSnap = db.collection(COLLECTION_DEVICES)
                    .document(ALAIN_DEVICE_ID)
                    .collection(COLLECTION_INFO)
                    .document(DOCUMENT_TOKEN)
                    .get()
                    .await()

                val alainToken = infoSnap.getString("fcmToken")
                if (alainToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@MichelMainActivity,
                        getString(R.string.error_alain_token_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "Alain FCM token not found in Firestore")
                    return@launch
                }

                val commandId = UUID.randomUUID().toString()
                val command = hashMapOf(
                    "type" to type,
                    "targetToken" to alainToken,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection(COLLECTION_DEVICES)
                    .document(ALAIN_DEVICE_ID)
                    .collection(COLLECTION_COMMANDS)
                    .document(commandId)
                    .set(command)
                    .await()

                val msg = if (type == "START") {
                    binding.tvStatus.text = getString(R.string.status_start_sent)
                    getString(R.string.toast_start_sent)
                } else {
                    binding.tvStatus.text = getString(R.string.status_stop_sent)
                    getString(R.string.toast_stop_sent)
                }
                Toast.makeText(this@MichelMainActivity, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Command sent: $type (commandId=$commandId)")

            } catch (e: Exception) {
                Log.e(TAG, "Error sending command: ${e.message}")
                Toast.makeText(
                    this@MichelMainActivity,
                    getString(R.string.error_send_command, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ── Notification permission ──────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    companion object {
        const val FCM_TOPIC_LOCATION = "alain_location"
        const val COLLECTION_DEVICES = "devices"
        const val COLLECTION_COMMANDS = "commands"
        const val COLLECTION_LOCATION = "location"
        const val COLLECTION_INFO = "info"
        const val DOCUMENT_LATEST = "latest"
        const val DOCUMENT_TOKEN = "token"
        const val ALAIN_DEVICE_ID = "alain"
    }
}
