package com.healthbridge.alain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.healthbridge.alain.databinding.ActivityAlainMainBinding

/**
 * Alain's main screen.
 *
 * Shows the current tracking status and provides manual Start/Stop override
 * buttons. In normal operation the service is started/stopped automatically
 * via FCM commands sent by Michel.
 *
 * The actual location tracking is handled by [LocationForegroundService].
 * The FCM token registration and command reception is handled by [AlainFcmService].
 */
class AlainMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlainMainBinding
    private val TAG = "AlainMainActivity"

    // Runtime permission launchers
    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
            } else {
                true
            }
            if (!fineGranted) {
                Toast.makeText(
                    this,
                    getString(R.string.error_location_permission),
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Fine location permission denied")
            }
            if (!bgGranted) {
                Log.w(TAG, "Background location permission denied – service may be interrupted")
            }
        }

    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied")
            }
        }

    // Background location must be requested separately after fine location
    private val bgLocationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Log.w(TAG, "Background location permission denied – service may stop in background")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlainMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNeeded()

        binding.btnStartService.setOnClickListener {
            startLocationService()
        }

        binding.btnStopService.setOnClickListener {
            stopLocationService()
        }

        // Reflect the current service state in the UI when resumed
        updateUiForServiceState(LocationForegroundService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        updateUiForServiceState(LocationForegroundService.isRunning)
    }

    // ── Service control ──────────────────────────────────────────────────────

    private fun startLocationService() {
        if (!hasLocationPermission()) {
            Toast.makeText(
                this,
                getString(R.string.error_location_permission),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
        updateUiForServiceState(true)
        Log.d(TAG, "LocationForegroundService start requested")
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent)
        updateUiForServiceState(false)
        Log.d(TAG, "LocationForegroundService stop requested")
    }

    private fun updateUiForServiceState(running: Boolean) {
        binding.tvStatus.text = if (running) {
            getString(R.string.status_tracking)
        } else {
            getString(R.string.status_idle)
        }
        binding.btnStartService.isEnabled = !running
        binding.btnStopService.isEnabled = running
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private fun requestPermissionsIfNeeded() {
        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Fine + Coarse location
        val locationPerms = buildList {
            if (ContextCompat.checkSelfPermission(
                    this@AlainMainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (locationPerms.isNotEmpty()) {
            locationPermLauncher.launch(locationPerms.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Android 10+ requires a separate request for background location
            bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
