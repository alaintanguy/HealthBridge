package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggleBtn = findViewById<Button>(R.id.btnToggle)
        val spinnerInterval = findViewById<Spinner>(R.id.spinnerInterval)

        // Interval options
        val intervals = arrayOf("1 min", "5 min", "15 min", "1 hour", "24 hours")
        val intervalValues = longArrayOf(1, 5, 15, 60, 1440)

        spinnerInterval.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            intervals
        )

        toggleBtn.setOnClickListener {
            if (!serviceRunning) {
                val selected = spinnerInterval.selectedItemPosition
                val interval = intervalValues[selected]

                val intent = Intent(this, LocationService::class.java)
                intent.putExtra("interval", interval)
                startForegroundService(intent)

                toggleBtn.text = "⏹ STOP Sharing"
                serviceRunning = true
            } else {
                stopService(Intent(this, LocationService::class.java))
                toggleBtn.text = "▶ START Sharing"
                serviceRunning = false
            }
        }
    }
}
