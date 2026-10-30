package com.paperlauncher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager

data class BatteryInfo(val percent: Int, val charging: Boolean, val minutesToFull: Long)

data class SignalInfo(
    val cellBars: Int,     // 0..10 dashes
    val wifiBars: Int,     // 0..10 dashes
    val network: String,   // WI-FI / 5G / LTE / OFFLINE
    val carrier: String,
)

data class StorageInfo(val usedGb: Double, val totalGb: Double, val ramUsedGb: Double, val ramTotalGb: Double) {
    val storagePct get() = if (totalGb > 0) (usedGb / totalGb).toFloat() else 0f
    val ramPct get() = if (ramTotalGb > 0) (ramUsedGb / ramTotalGb).toFloat() else 0f
}

object Device {
    fun battery(ctx: Context): BatteryInfo {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        var mins = -1L
        if (charging && Build.VERSION.SDK_INT >= 28) {
            val bm = ctx.getSystemService(BatteryManager::class.java)
            val ms = bm?.computeChargeTimeRemaining() ?: -1L
            if (ms > 0) mins = ms / 60000
        }
        return BatteryInfo(pct, charging, mins)
    }

    fun signal(ctx: Context): SignalInfo {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val tm = ctx.getSystemService(TelephonyManager::class.java)
        val carrier = runCatching { tm?.networkOperatorName ?: "" }.getOrDefault("").uppercase()
        var cell = 0
        if (Build.VERSION.SDK_INT >= 28) {
            cell = runCatching { tm?.signalStrength?.level ?: 0 }.getOrDefault(0)
        }
        var wifi = 0
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (onWifi) {
            val rssi = if (Build.VERSION.SDK_INT >= 29) caps?.signalStrength ?: Int.MIN_VALUE else Int.MIN_VALUE
            wifi = if (rssi != Int.MIN_VALUE && rssi < 0) {
                ((rssi + 100).coerceIn(0, 50) * 4 / 50)
            } else {
                @Suppress("DEPRECATION")
                runCatching {
                    val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
                    WifiManager.calculateSignalLevel(wm.connectionInfo.rssi, 5)
                }.getOrDefault(3)
            }
        }
        val net = when {
            caps == null -> "OFFLINE"
            onWifi -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularGeneration(tm)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "ONLINE"
        }
        return SignalInfo(cell * 10 / 4, wifi * 10 / 4, net, carrier.ifEmpty { "NO SIM" })
    }

    @Suppress("MissingPermission")
    private fun cellularGeneration(tm: TelephonyManager?): String = runCatching {
        when (tm?.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            else -> "MOBILE"
        }
    }.getOrDefault("MOBILE")

    fun storage(ctx: Context): StorageInfo {
        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.totalBytes.toDouble()
        val free = st.availableBytes.toDouble()
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val g = 1_000_000_000.0
        val gib = 1024.0 * 1024 * 1024
        return StorageInfo((total - free) / g, total / g, (mi.totalMem - mi.availMem) / gib, mi.totalMem / gib)
    }
}
