package com.healthbridge.wear

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var healthDataService: HealthDataService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvStatus = findViewById(R.id.tvStatus)

        healthDataService = HealthDataService(this)
        healthDataService.startMonitoring()

        tvStatus.text = "Monitoring active..."
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        healthDataService.stopMonitoring()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val heartRate = dataMap.getDouble("heart_rate")
                runOnUiThread {
                    tvHeartRate.text = "${heartRate.toInt()} BPM"
                }
            }
        }
    }
}
