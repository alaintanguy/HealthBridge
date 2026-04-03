package com.healthbridge.wear

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HealthDataService(private val context: Context) {

    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val heartRateCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: androidx.health.services.client.data.Availability
        ) {
            // Handle availability changes
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRateSamples = data.getData(DataType.HEART_RATE_BPM)
            heartRateSamples.forEach { sample ->
                handleHeartRateData(sample)
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
            measureClient.unregisterMeasureCallbackAsync(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
        }
        scope.cancel()
    }

    private fun handleHeartRateData(sample: SampleDataPoint<Double>) {
        val heartRate = sample.value
        val timestamp = sample.timeDurationFromBoot

        // Send data to phone via Wearable Data Layer API
        sendDataToPhone(heartRate, timestamp.toMillis())
    }

    private fun sendDataToPhone(heartRate: Double, timestamp: Long) {
        scope.launch {
            val dataClient = com.google.android.gms.wearable.Wearable
                .getDataClient(context)

            val putDataReq = com.google.android.gms.wearable.PutDataMapRequest
                .create("/health_data").apply {
                    dataMap.putDouble("heart_rate", heartRate)
                    dataMap.putLong("timestamp", timestamp)
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(putDataReq)
        }
    }
}
