package com.healthbridge.app

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object PushoverSender {

    private const val PUSHOVER_URL = "https://api.pushover.net/1/messages.json"
    private val client = OkHttpClient()

    fun sendAlert(
        apiToken: String,
        userKey: String,
        message: String,
        title: String = "HealthBridge Alert"
    ) {
        val formBody = FormBody.Builder()
            .add("token", apiToken)
            .add("user", userKey)
            .add("title", title)
            .add("message", message)
            .add("priority", "1")
            .build()

        val request = Request.Builder()
            .url(PUSHOVER_URL)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PushoverSender", "Failed to send alert: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("PushoverSender", "Alert sent successfully")
                } else {
                    Log.e("PushoverSender", "Error sending alert: ${response.code}")
                }
            }
        })
    }
}
