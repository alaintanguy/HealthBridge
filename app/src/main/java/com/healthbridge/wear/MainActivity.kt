package com.healthbridge.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var healthDataService: HealthDataService

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startHealthMonitoring()
        } else {
            tvStatus.text = "Permission Denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvStatus = findViewById(R.id.tvStatus)

        healthDataService = HealthDataService(this)

        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startHealthMonitoring()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }
    }

    private fun startHealthMonitoring() {
        tvStatus.text = "Monitoring..."
        healthDataService.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        healthDataService.stopMonitoring()
    }
}
