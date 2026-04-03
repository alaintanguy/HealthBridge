package com.healthbridge.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path
            when (path) {
                "/command" -> handleCommand(event)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/start_monitoring" -> {
                // Start health monitoring on watch
                val intent = android.content.Intent(this, MainActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            "/stop_monitoring" -> {
                // Stop monitoring
            }
        }
    }

    private fun handleCommand(event: com.google.android.gms.wearable.DataEvent) {
        val dataMap = com.google.android.gms.wearable.DataMapItem
            .fromDataItem(event.dataItem).dataMap
        val command = dataMap.getString("command")

        when (command) {
            "start" -> { /* handle start */ }
            "stop"  -> { /* handle stop  */ }
        }
    }
}
