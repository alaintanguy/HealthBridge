package com.healthbridge.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class HealthDataService(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val heartRateSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    fun startMonitoring() {
        heartRateSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0].toInt()
            sendHeartRateToPhone(heartRate)
            updateUI(heartRate)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendHeartRateToPhone(heartRate: Int) {
        val putDataMapRequest = PutDataMapRequest.create("/heart_rate")
        putDataMapRequest.dataMap.putInt("heart_rate", heartRate)
        putDataMapRequest.dataMap.putLong("timestamp", System.currentTimeMillis())

        val putDataRequest = putDataMapRequest.asPutDataRequest()
            .setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataRequest)
    }

    private fun updateUI(heartRate: Int) {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.findViewById<android.widget.TextView>(
                    R.id.tvHeartRate
                ).text = "$heartRate BPM"
            }
        }
    }
}
