package com.healthbridge.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private lateinit var heartRateMonitor: HeartRateMonitor
    private lateinit var tvHeartRate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHeartRate = findViewById(R.id.tv_heart_rate)

        heartRateMonitor = HeartRateMonitor(this) { bpm ->
            runOnUiThread {
                tvHeartRate.text = "$bpm BPM"
            }
            sendHeartRateToPhone(bpm)
        }

        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            heartRateMonitor.start()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                REQUEST_BODY_SENSORS
            )
        }
    }

    private fun sendHeartRateToPhone(bpm: Int) {
        val putDataMapRequest = PutDataMapRequest.create("/heart_rate").apply {
            dataMap.putInt("bpm", bpm)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(putDataRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BODY_SENSORS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            heartRateMonitor.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateMonitor.stop()
    }

    companion object {
        private const val REQUEST_BODY_SENSORS = 100
    }
}
