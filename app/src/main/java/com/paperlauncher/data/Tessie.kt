package com.paperlauncher.data

import android.graphics.Bitmap
import org.json.JSONObject

data class CarData(
    val vin: String,
    val name: String,
    val model: String,
    val battery: Int,
    val rangeMi: Int,
    val chargingState: String,     // Charging / Complete / Disconnected / Stopped
    val minutesToFull: Int,
    val chargerKw: Int,
    val chargeLimit: Int,
    val locked: Boolean,
    val climateOn: Boolean,
    val setTemp: Int,
    val insideTemp: Int?,
    val outsideTemp: Int?,
    val tirePsi: Int?,
    val odometer: Int,
    val asleep: Boolean,
    val location: String,
    val updatedAt: Long,
) {
    val charging get() = chargingState.equals("Charging", true)
}

object Tessie {
    private const val BASE = "https://api.tessie.com"

    private fun auth(token: String) = mapOf("Authorization" to "Bearer $token")

    /** Cached state; does not wake the car. */
    suspend fun fetch(token: String): CarData? {
        if (token.isBlank()) return null
        val j = Http.json("$BASE/vehicles?only_active=true", auth(token))
        val results = j.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val v = results.getJSONObject(0)
        val vin = v.optString("vin")
        val s = v.optJSONObject("last_state") ?: JSONObject()
        val charge = s.optJSONObject("charge_state") ?: JSONObject()
        val climate = s.optJSONObject("climate_state") ?: JSONObject()
        val vs = s.optJSONObject("vehicle_state") ?: JSONObject()
        val cfg = s.optJSONObject("vehicle_config") ?: JSONObject()
        val asleep = runCatching {
            Http.json("$BASE/$vin/status", auth(token)).optString("status") == "asleep"
        }.getOrDefault(false)
        val psiFl = vs.optDouble("tpms_pressure_fl", Double.NaN)
        val model = when (cfg.optString("car_type")) {
            "model3" -> "MODEL 3"; "modely" -> "MODEL Y"; "models" -> "MODEL S"; "modelx" -> "MODEL X"
            "cybertruck" -> "CYBERTRUCK"; else -> "TESLA"
        }
        return CarData(
            vin = vin,
            name = s.optString("display_name", v.optString("display_name", "")).uppercase(),
            model = model,
            battery = charge.optInt("battery_level"),
            rangeMi = Math.round(charge.optDouble("battery_range", 0.0)).toInt(),
            chargingState = charge.optString("charging_state", ""),
            minutesToFull = charge.optInt("minutes_to_full_charge"),
            chargerKw = charge.optInt("charger_power"),
            chargeLimit = charge.optInt("charge_limit_soc", 80),
            locked = vs.optBoolean("locked", true),
            climateOn = climate.optBoolean("is_climate_on", false),
            setTemp = Math.round(climate.optDouble("driver_temp_setting", 21.0)).toInt(),
            insideTemp = climate.optDouble("inside_temp", Double.NaN).takeIf { !it.isNaN() }?.let { Math.round(it).toInt() },
            outsideTemp = climate.optDouble("outside_temp", Double.NaN).takeIf { !it.isNaN() }?.let { Math.round(it).toInt() },
            tirePsi = if (psiFl.isNaN()) null else Math.round(psiFl * 14.5038).toInt(),
            odometer = Math.round(vs.optDouble("odometer", 0.0)).toInt(),
            asleep = asleep,
            location = if (s.optJSONObject("drive_state")?.optString("shift_state").isNullOrEmpty()) "PARKED" else "DRIVING",
            updatedAt = (s.optLong("vehicle_state_timestamp", 0L).takeIf { it > 0 }
                ?: vs.optLong("timestamp", System.currentTimeMillis())),
        )
    }

    /** command ∈ lock, unlock, start_climate, stop_climate, open_charge_port, honk */
    suspend fun command(token: String, vin: String, command: String): Boolean = runCatching {
        Http.post("$BASE/$vin/command/$command", auth(token))
        true
    }.getOrDefault(false)

    /** Side-view render from Tesla's compositor: Model 3, Pearl White, 18" Aero. */
    suspend fun render(): Bitmap? = Http.bitmap(
        "https://static-assets.tesla.com/configurator/compositor?context=design_studio_2&options=\$MT322,\$PPSW,\$W38B,\$IPB1&view=STUD_SIDE&model=m3&size=800&bkba_opt=1&crop=0,0,0,0"
    )
}
