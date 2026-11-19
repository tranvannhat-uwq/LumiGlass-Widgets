package com.lumiglass.widgets.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lumiglass.widgets.model.BatteryInfo

/**
 * Reads real device battery percentage, charging status, and actual battery temperature (°C)
 * from Android's sticky ACTION_BATTERY_CHANGED broadcast and BatteryManager system service.
 * Never runs a continuous background service and never fabricates data.
 */
object BatteryRepository {

    fun readCurrentBattery(context: Context, enabled: Boolean = true): BatteryInfo {
        if (!enabled) {
            return BatteryInfo(
                hasLevelData = false,
                levelPercent = -1,
                isCharging = false,
                hasValidTemperature = false,
                temperatureCelsius = null
            )
        }

        val batteryIntent: Intent? = runCatching {
            context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE

        var computedPercent = parseBatteryPercent(level, scale)
        if (computedPercent == null) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val propCap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (propCap in 0..100) {
                computedPercent = propCap
            }
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val tempCelsius = parseBatteryTemperatureTenths(tempTenths)

        return BatteryInfo(
            hasLevelData = computedPercent != null,
            levelPercent = computedPercent ?: -1,
            isCharging = isCharging,
            hasValidTemperature = tempCelsius != null,
            temperatureCelsius = tempCelsius
        )
    }

    /**
     * Converts raw EXTRA_LEVEL and EXTRA_SCALE into an integer percentage [0..100], or null if invalid.
     */
    fun parseBatteryPercent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) return null
        val pct = ((level.toFloat() / scale.toFloat()) * 100f).toInt()
        return if (pct in 0..100) pct else null
    }

    /**
     * Converts Android BatteryManager.EXTRA_TEMPERATURE (tenths of a degree Celsius) into °C.
     * Example: 345 -> 34.5°C.
     * Returns null if the sensor value is missing or outside physical battery bounds [1.0°C .. 85.0°C].
     */
    fun parseBatteryTemperatureTenths(tempTenths: Int): Float? {
        if (tempTenths < 10 || tempTenths > 850) return null
        return tempTenths / 10.0f
    }
}
