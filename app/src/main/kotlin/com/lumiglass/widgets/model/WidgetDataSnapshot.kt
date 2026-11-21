package com.lumiglass.widgets.model

import kotlinx.serialization.Serializable

@Serializable
data class LunarDateInfo(
    val solarDay: Int,
    val solarMonth: Int,
    val solarYear: Int,
    val lunarDay: Int,
    val lunarMonth: Int,
    val lunarYear: Int,
    val isLeapMonth: Boolean,
    val dayCanChi: String,
    val monthCanChi: String,
    val yearCanChi: String,
    val specialPhaseOrHolidayVi: String? = null
) {
    fun formatLunarShort(): String {
        val dayStr = lunarDay.toString().padStart(2, '0')
        val monthStr = lunarMonth.toString().padStart(2, '0')
        val leapSuffix = if (isLeapMonth) " (Nhuận)" else ""
        return "$dayStr/$monthStr$leapSuffix"
    }

    fun formatDisplay(showCanChi: Boolean, showHolidays: Boolean): String {
        val base = "Âm lịch: ${formatLunarShort()}"
        val canChiPart = if (showCanChi) " • $yearCanChi" else ""
        val holidayPart = if (showHolidays && !specialPhaseOrHolidayVi.isNullOrBlank()) {
            " • $specialPhaseOrHolidayVi"
        } else ""
        return "$base$canChiPart$holidayPart"
    }

    fun formatCompact(showCanChi: Boolean): String {
        val base = "${formatLunarShort()} ÂL"
        return if (showCanChi) "$base • $yearCanChi" else base
    }
}

enum class WeatherConditionCategory {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    THUNDERSTORM,
    UNKNOWN
}

@Serializable
data class WeatherInfo(
    val hasData: Boolean = false,
    val cityId: String = "hanoi",
    val cityNameVi: String = "Hà Nội",
    val currentTempC: Double? = null,
    val highTempC: Double? = null,
    val lowTempC: Double? = null,
    val feelsLikeC: Double? = null,
    val humidityPercent: Int? = null,
    val rainProbabilityPercent: Int? = null,
    val wmoCode: Int = -1,
    val conditionTextVi: String = "Chưa có dữ liệu",
    val isDay: Boolean = true,
    val updatedAtEpochMillis: Long = 0L,
    val isFromOfflineCache: Boolean = false,
    val errorMessageVi: String? = null
) {
    fun formatTemp(tempC: Double?, unit: TemperatureUnit, degreeOnly: Boolean = false): String {
        if (tempC == null) return if (degreeOnly) "--°" else "--${unit.symbol}"
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> tempC
            TemperatureUnit.FAHRENHEIT -> tempC * 9.0 / 5.0 + 32.0
        }
        val sym = if (degreeOnly) "°" else unit.symbol
        return "${Math.round(value)}$sym"
    }

    fun formatHighLow(unit: TemperatureUnit, degreeOnly: Boolean = false): String {
        if (highTempC == null || lowTempC == null) return "C:-- • T:--"
        return "C:${formatTemp(highTempC, unit, degreeOnly)} • T:${formatTemp(lowTempC, unit, degreeOnly)}"
    }

    fun formatRangeCompact(unit: TemperatureUnit, degreeOnly: Boolean = true): String {
        if (highTempC == null || lowTempC == null) return "--°–--°"
        val low = formatTemp(lowTempC, unit, degreeOnly)
        val high = formatTemp(highTempC, unit, degreeOnly)
        return "$low–$high"
    }

    fun formatRangeAndPrecip(unit: TemperatureUnit, degreeOnly: Boolean = true): String {
        val range = formatRangeCompact(unit, degreeOnly)
        val rain = rainProbabilityPercent
        val humidity = humidityPercent
        return when {
            rain != null -> "$range • Mưa $rain%"
            humidity != null -> "$range • Ẩm $humidity%"
            feelsLikeC != null -> "$range • Cảm giác ${formatTemp(feelsLikeC, unit, degreeOnly)}"
            else -> range
        }
    }

    fun conditionCategory(): WeatherConditionCategory {
        if (!hasData || wmoCode < 0) return WeatherConditionCategory.UNKNOWN
        return when (wmoCode) {
            0, 1 -> if (isDay) WeatherConditionCategory.CLEAR_DAY else WeatherConditionCategory.CLEAR_NIGHT
            2 -> WeatherConditionCategory.PARTLY_CLOUDY
            3 -> WeatherConditionCategory.CLOUDY
            45, 48 -> WeatherConditionCategory.FOG
            51, 53, 55, 56, 57 -> WeatherConditionCategory.DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> WeatherConditionCategory.RAIN
            95, 96, 99 -> WeatherConditionCategory.THUNDERSTORM
            else -> WeatherConditionCategory.PARTLY_CLOUDY
        }
    }
}

data class BatteryInfo(
    val hasLevelData: Boolean = false,
    val levelPercent: Int = -1,
    val isCharging: Boolean = false,
    val hasValidTemperature: Boolean = false,
    val temperatureCelsius: Float? = null
) {
    fun formatPercent(): String {
        return if (hasLevelData && levelPercent in 0..100) "$levelPercent%" else "--%"
    }

    fun formatTemperature(unit: TemperatureUnit = TemperatureUnit.CELSIUS): String {
        val tempC = temperatureCelsius
        if (!hasValidTemperature || tempC == null) return "--${unit.symbol}"
        return when (unit) {
            TemperatureUnit.CELSIUS -> String.format(java.util.Locale.US, "%.1f°C", tempC)
            TemperatureUnit.FAHRENHEIT -> {
                val f = tempC * 9f / 5f + 32f
                String.format(java.util.Locale.US, "%.1f°F", f)
            }
        }
    }
}

@Serializable
data class CalendarDayColumn(
    val dayOffset: Int,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val weekdayShortVi: String,
    val solarDayString: String,
    val lunarDayMonthString: String
)

data class WidgetDataSnapshot(
    val previewTimePrimary: String,
    val previewTimeHour: String,
    val previewTimeMinute: String,
    val previewAmPm: String,
    val gregorianDateText: String,
    val gregorianWeekdayShort: String,
    val lunarDateInfo: LunarDateInfo,
    val weatherInfo: WeatherInfo,
    val batteryInfo: BatteryInfo,
    val isSystemDarkTheme: Boolean,
    val weekCalendarColumns: List<CalendarDayColumn> = emptyList(),
    val ramInfo: com.lumiglass.widgets.data.ram.RamInfo = com.lumiglass.widgets.data.ram.RamInfo(),
    val storageInfo: com.lumiglass.widgets.data.storage.StorageInfo = com.lumiglass.widgets.data.storage.StorageInfo()
)
