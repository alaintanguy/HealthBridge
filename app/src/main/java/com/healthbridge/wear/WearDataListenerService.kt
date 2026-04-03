package com.healthbridge.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path
            // Handle incoming data from phone if needed
            when (path) {
                "/settings" -> handleSettingsUpdate(event)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/start_monitoring" -> {
                // Start heart rate monitoring
            }
            "/stop_monitoring" -> {
                // Stop heart rate monitoring
            }
        }
    }

    private fun handleSettingsUpdate(event: com.google.android.gms.wearable.DataEvent) {
        // Process settings sent from phone app
    }
}
