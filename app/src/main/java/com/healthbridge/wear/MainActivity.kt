package com.healthbridge.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthDataService: HealthDataService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        healthDataService = HealthDataService(this)

        lifecycleScope.launch {
            healthDataService.startMonitoring()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        healthDataService.stopMonitoring()
    }
}
