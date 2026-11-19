package com.example.fridgefoodsaver

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val tvBack = findViewById<TextView>(R.id.tvBack)

        tvBack.setOnClickListener {
            finish()
        }
    }
}