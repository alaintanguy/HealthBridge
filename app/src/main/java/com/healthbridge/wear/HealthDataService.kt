package com.healthbridge.wear

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HealthDataService(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val measureClient = HealthServices.getClient(context).measureClient

    private val heartRateCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            Log.d("HealthDataService", "Availability: $availability")
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRatePoints = data.getData(DataType.HEART_RATE_BPM)
            heartRatePoints.lastOrNull()?.let { point ->
                val heartRate = point.value
                Log.d("HealthDataService", "Heart Rate: $heartRate")
                sendHeartRateToPhone(heartRate)
            }
        }
    }

    fun startMonitoring() {
        scope.launch {
            measureClient.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
        }
    }

    fun stopMonitoring() {
        scope.launch {
            measureClient.unregisterMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
        }
        scope.cancel()
    }

    private fun sendHeartRateToPhone(heartRate: Double) {
        val putDataMapRequest = PutDataMapRequest.create("/heart_rate").apply {
            dataMap.putDouble("heart_rate", heartRate)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(putDataRequest)
    }
}
