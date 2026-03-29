package com.healthbridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PushoverSender {

    // 🔑 Your credentials here
    private const val APP_TOKEN = "YOUR_APP_TOKEN"
    private const val USER_KEY  = "YOUR_USER_KEY"

    suspend fun sendLocation(lat: Double, lon: Double) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.pushover.net/1/messages.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val message = "📍 Alain's location:\n" +
                    "Lat: $lat\n" +
                    "Lon: $lon\n" +
                    "🗺️ https://maps.google.com/?q=$lat,$lon"

                val params = "token=${URLEncoder.encode(APP_TOKEN, "UTF-8")}" +
                    "&user=${URLEncoder.encode(USER_KEY, "UTF-8")}" +
                    "&message=${URLEncoder.encode(message, "UTF-8")}" +
                    "&title=HealthBridge+GPS"

                conn.outputStream.write(params.toByteArray())
                conn.responseCode // trigger send
                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
