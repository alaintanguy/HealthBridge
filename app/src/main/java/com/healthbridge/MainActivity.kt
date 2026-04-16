package com.healthbridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
//import com.healthbridge.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Link to your layout file
        setContentView(layout.activity_main)
    }
}
