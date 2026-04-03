package com.healthbridge.wear

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : FragmentActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var dataClient: DataClient
    private lateinit var heartRateMonitor: HeartRateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvStatus = findViewById(R.id.tvStatus)
        dataClient = Wearable.getDataClient(this)

        heartRateMonitor = HeartRateMonitor(this) { bpm ->
            runOnUiThread {
                tvHeartRate.text = "$bpm BPM"
                tvStatus.text = "Live Monitoring"
            }
            sendHeartRateToPhone(bpm)
        }
    }

    override fun onResume() {
        super.onResume()
        heartRateMonitor.start()
        tvStatus.text = "Monitoring..."
    }

    override fun onPause() {
        super.onPause()
        heartRateMonitor.stop()
        tvStatus.text = "Paused"
    }

    private fun sendHeartRateToPhone(bpm: Int) {
        val request = PutDataMapRequest.create("/heart_rate").apply {
            dataMap.putInt("bpm", bpm)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest()
            .setUrgent()

        dataClient.putDataItem(request)
    }
}
