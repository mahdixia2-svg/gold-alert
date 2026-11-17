package com.goldalert.app

import android.app.*
import android.content.Intent
import android.os.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MonitorService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val channel = "gold_alert_signals"
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channel, "Gold alerts", NotificationManager.IMPORTANCE_HIGH))
        manager.createNotificationChannel(NotificationChannel("monitor", "Monitoring status", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START") {
            val ongoing = Notification.Builder(this, "monitor").setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("Gold Alert running").setContentText("Checking closed candles").setOngoing(true).build()
            startForeground(100, ongoing)
            if (!executor.isShutdown && !started) {
                started = true
                executor.scheduleWithFixedDelay({ try { checkSignal() } catch (_: Exception) {} }, 0, 60, TimeUnit.SECONDS)
            }
        }
        return START_NOT_STICKY
    }
    private var started = false
    private fun checkSignal() {
        val prefs = getSharedPreferences("gold", MODE_PRIVATE)
        val base = prefs.getString("url", "") ?: return
        val interval = prefs.getString("interval", "5min") ?: "5min"
        if (!base.startsWith("https://")) return
        val connection = URL("$base/signals?interval=$interval" +
                "&atr_period=${prefs.getString("atr_period", "10")}" +
                "&multiplier=${prefs.getString("multiplier", "3")}" +
                "&adx_period=${prefs.getString("adx_period", "14")}" +
                "&min_adx=${prefs.getString("min_adx", "25")}").openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        try {
            if (connection.responseCode != 200) return
            val payload = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latest = payload.getJSONObject("latest")
            val signal = latest.optString("signal", "")
            val stamp = latest.optString("timestamp", "")
            if (signal != "BUY" && signal != "SELL") return
            val key = "$interval:$stamp:$signal"
            // First observed signal is deliberately not notified: avoids old historical alerts.
            val old = prefs.getString("last_seen_$interval", null)
            prefs.edit().putString("last_seen_$interval", key).apply()
            if (old == key) return
            // Only alert for the candle just closed, never for a historical signal.
            val ageMs = System.currentTimeMillis() - java.time.LocalDateTime.parse(stamp.replace(" ", "T")).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            val periodMs = when (interval) { "1min" -> 60000L; "5min" -> 300000L; "15min" -> 900000L; "30min" -> 1800000L; "1h" -> 3600000L; else -> 14400000L }
            if (ageMs < periodMs || ageMs > periodMs + 120000L) return
            val notification = Notification.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("XAUUSD $signal")
                .setContentText("Supertrend flipped • ADX ${latest.optDouble("adx")} • $interval • $stamp UTC")
                .setAutoCancel(true).build()
            getSystemService(NotificationManager::class.java).notify(stamp.hashCode(), notification)
        } finally { connection.disconnect() }
    }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
