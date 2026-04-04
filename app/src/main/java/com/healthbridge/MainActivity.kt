package com.healthbridge.wear

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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
        }
    }

    override fun onResume() {
        super.onResume()
        heartRateMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        heartRateMonitor.stop()
    }
}
