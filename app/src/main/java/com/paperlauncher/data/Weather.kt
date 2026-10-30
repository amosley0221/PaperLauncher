package com.paperlauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class Day(val name: String, val code: Int, val hi: Int, val lo: Int)

data class WeatherData(
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val feels: Int,
    val humidity: Int,
    val wind: Int,
    val windDir: Int,
    val uv: Int,
    val hi: Int,
    val lo: Int,
    val sunrise: String,
    val sunset: String,
    val place: String,
    val hourly: List<Int>,
    val hours: List<String>,
    val days: List<Day>,
    val unit: String,
    val windUnit: String,
    val updatedAt: Long,
) {
    val condition get() = conditionFor(code)
    val glyph get() = glyphFor(code, isDay)
}

fun conditionFor(code: Int) = when (code) {
    0 -> "CLEAR SKY"
    1 -> "MAINLY CLEAR"
    2 -> "PARTLY CLOUDY"
    3 -> "OVERCAST"
    45, 48 -> "FOG"
    in 51..57 -> "DRIZZLE"
    in 61..67 -> "RAIN"
    in 71..77 -> "SNOW"
    in 80..82 -> "SHOWERS"
    85, 86 -> "SNOW SHOWERS"
    in 95..99 -> "THUNDERSTORM"
    else -> "—"
}

fun glyphFor(code: Int, day: Boolean = true) = when (code) {
    0, 1 -> if (day) "sunny" else "clear_night"
    2 -> if (day) "partly_cloudy_day" else "partly_cloudy_night"
    3 -> "cloud"
    45, 48 -> "foggy"
    in 51..67, in 80..82 -> "rainy"
    in 71..77, 85, 86 -> "weather_snowy"
    in 95..99 -> "thunderstorm"
    else -> "cloud"
}

object Weather {
    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun lastLocation(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @Suppress("DEPRECATION")
    private suspend fun placeName(ctx: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        runCatching {
            val a = Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
            (a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: "").uppercase()
        }.getOrDefault("")
    }

    suspend fun fetch(ctx: Context, fahrenheit: Boolean): WeatherData? {
        val loc = lastLocation(ctx) ?: return null
        val lat = loc.latitude
        val lon = loc.longitude
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,is_day" +
            "&hourly=temperature_2m&forecast_hours=24" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max" +
            "&timezone=auto&forecast_days=4" +
            (if (fahrenheit) "&temperature_unit=fahrenheit&wind_speed_unit=mph" else "")
        val j = Http.json(url)
        val cur = j.getJSONObject("current")
        val daily = j.getJSONObject("daily")
        val hourly = j.getJSONObject("hourly")
        val ht = hourly.getJSONArray("temperature_2m")
        val htime = hourly.getJSONArray("time")
        val dTime = daily.getJSONArray("time")
        val dCode = daily.getJSONArray("weather_code")
        val dMax = daily.getJSONArray("temperature_2m_max")
        val dMin = daily.getJSONArray("temperature_2m_min")
        val days = (1 until minOf(4, dTime.length())).map { i ->
            val date = java.time.LocalDate.parse(dTime.getString(i))
            Day(
                date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                dCode.optInt(i), Math.round(dMax.optDouble(i)).toInt(), Math.round(dMin.optDouble(i)).toInt(),
            )
        }
        fun hm(s: String) = s.substringAfter('T').take(5)
        return WeatherData(
            temp = Math.round(cur.optDouble("temperature_2m")).toInt(),
            code = cur.optInt("weather_code"),
            isDay = cur.optInt("is_day", 1) == 1,
            feels = Math.round(cur.optDouble("apparent_temperature")).toInt(),
            humidity = cur.optInt("relative_humidity_2m"),
            wind = Math.round(cur.optDouble("wind_speed_10m")).toInt(),
            windDir = cur.optInt("wind_direction_10m"),
            uv = Math.round(daily.getJSONArray("uv_index_max").optDouble(0)).toInt(),
            hi = Math.round(dMax.optDouble(0)).toInt(),
            lo = Math.round(dMin.optDouble(0)).toInt(),
            sunrise = hm(daily.getJSONArray("sunrise").optString(0)),
            sunset = hm(daily.getJSONArray("sunset").optString(0)),
            place = placeName(ctx, lat, lon),
            hourly = List(ht.length()) { Math.round(ht.optDouble(it)).toInt() },
            hours = List(htime.length()) { hm(htime.getString(it)).take(2) },
            days = days,
            unit = if (fahrenheit) "F" else "C",
            windUnit = if (fahrenheit) "MPH" else "KM/H",
            updatedAt = System.currentTimeMillis(),
        )
    }
}
