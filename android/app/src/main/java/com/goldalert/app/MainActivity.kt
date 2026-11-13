package com.goldalert.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val prefs = getSharedPreferences("gold", MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 24) }
        root.addView(TextView(this).apply { text = "Gold Alert • XAUUSD"; textSize = 24f })
        root.addView(TextView(this).apply { text = "Supertrend (10, 3) + ADX (14) > 25\nOnly finalized candles. Requires your own HTTPS backend."; textSize = 15f })
        val url = EditText(this).apply { hint = "https://your-server.example"; setSingleLine(); setText(prefs.getString("url", "")); inputType = 17 }
        root.addView(url)
        val intervals = arrayOf("1min", "5min", "15min", "30min", "1h", "4h")
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, intervals); setSelection(intervals.indexOf(prefs.getString("interval", "5min")).coerceAtLeast(0)) }
        root.addView(spinner)
        val status = TextView(this).apply { text = "This is a foreground monitoring prototype. Android may stop it; no 24/7 guarantee." }
        root.addView(status)
        root.addView(Button(this).apply { text = "Start monitoring"; setOnClickListener {
            val endpoint = url.text.toString().trim().trimEnd('/')
            if (!endpoint.startsWith("https://")) { status.text = "Please enter an HTTPS server URL"; return@setOnClickListener }
            prefs.edit().putString("url", endpoint).putString("interval", spinner.selectedItem.toString()).apply()
            try { startForegroundService(Intent(this@MainActivity, MonitorService::class.java).setAction("START")); status.text = "Monitoring requested; check persistent notification" }
            catch (e: Exception) { status.text = "Unable to start: ${e.message}" }
        } })
        root.addView(Button(this).apply { text = "Stop monitoring"; setOnClickListener { stopService(Intent(this@MainActivity, MonitorService::class.java)); status.text = "Stopped" } })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
