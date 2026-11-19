package com.lumiglass.widgets.data.datetime

import com.lumiglass.widgets.data.lunar.VietnameseLunarCalendar
import com.lumiglass.widgets.model.DateFormatStyle
import com.lumiglass.widgets.model.LunarDateInfo
import com.lumiglass.widgets.model.TimezoneMode
import com.lumiglass.widgets.model.WidgetConfig
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.DayOfWeek

object DateTimeRepository {

    private val VIETNAM_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    fun resolveZoneId(mode: TimezoneMode): ZoneId {
        return when (mode) {
            TimezoneMode.VIETNAM_UTC7 -> VIETNAM_ZONE
            TimezoneMode.SYSTEM -> ZoneId.systemDefault()
        }
    }

    fun nowZoned(mode: TimezoneMode, epochMillis: Long = System.currentTimeMillis()): ZonedDateTime {
        return Instant.ofEpochMilli(epochMillis).atZone(resolveZoneId(mode))
    }

    fun getVietnameseWeekdayFull(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Thứ Hai"
            DayOfWeek.TUESDAY -> "Thứ Ba"
            DayOfWeek.WEDNESDAY -> "Thứ Tư"
            DayOfWeek.THURSDAY -> "Thứ Năm"
            DayOfWeek.FRIDAY -> "Thứ Sáu"
            DayOfWeek.SATURDAY -> "Thứ Bảy"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
        }
    }

    fun getVietnameseWeekdayShort(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Th 2"
            DayOfWeek.TUESDAY -> "Th 3"
            DayOfWeek.WEDNESDAY -> "Th 4"
            DayOfWeek.THURSDAY -> "Th 5"
            DayOfWeek.FRIDAY -> "Th 6"
            DayOfWeek.SATURDAY -> "Th 7"
            DayOfWeek.SUNDAY -> "CN"
        }
    }

    fun getVietnameseMonthFull(month: Int): String {
        return "tháng $month"
    }

    fun getVietnameseMonthShort(month: Int): String {
        return "Thg $month"
    }

    /**
     * Formats the Gregorian date according to the user's selected DateFormatStyle and useMonthInWords.
     */
    fun formatGregorianDate(
        zdt: ZonedDateTime,
        style: DateFormatStyle,
        useMonthInWords: Boolean
    ): String {
        val day = zdt.dayOfMonth
        val month = zdt.monthValue
        val year = zdt.year
        val dayPad = day.toString().padStart(2, '0')
        val monthPad = month.toString().padStart(2, '0')
        val weekdayFull = getVietnameseWeekdayFull(zdt.dayOfWeek)
        val weekdayShort = getVietnameseWeekdayShort(zdt.dayOfWeek)

        return when (style) {
            DateFormatStyle.FULL_VIETNAMESE -> {
                if (useMonthInWords) {
                    "$weekdayFull, $day ${getVietnameseMonthFull(month)} năm $year"
                } else {
                    "$weekdayFull, $dayPad/$monthPad/$year"
                }
            }
            DateFormatStyle.COMPACT_UPPER_DOT -> {
                if (useMonthInWords) {
                    "${weekdayFull.uppercase()} • $day ${getVietnameseMonthShort(month).uppercase()}"
                } else {
                    "${weekdayFull.uppercase()} • $dayPad/$monthPad"
                }
            }
            DateFormatStyle.SHORT_WEEKDAY_MONTH -> {
                if (useMonthInWords) {
                    "$weekdayShort, $day ${getVietnameseMonthShort(month)}"
                } else {
                    "$weekdayShort, $dayPad/$monthPad"
                }
            }
            DateFormatStyle.WEEKDAY_NUMERIC_SLASH -> {
                if (useMonthInWords) {
                    "$weekdayFull, $day ${getVietnameseMonthShort(month)} $year"
                } else {
                    "$weekdayFull, $dayPad/$monthPad/$year"
                }
            }
            DateFormatStyle.NUMERIC_SLASH -> {
                if (useMonthInWords) {
                    "$day ${getVietnameseMonthFull(month)}, $year"
                } else {
                    "$dayPad/$monthPad/$year"
                }
            }
        }
    }

    fun getCurrentLunarDate(zdt: ZonedDateTime): LunarDateInfo {
        return VietnameseLunarCalendar.convertSolarToLunar(
            dd = zdt.dayOfMonth,
            mm = zdt.monthValue,
            yy = zdt.year,
            timeZone = VietnameseLunarCalendar.VIETNAM_TIMEZONE
        )
    }

    /**
     * Formats clock strings for Compose live preview (while actual widget uses TextClock patterns).
     */
    fun formatPreviewClock(
        zdt: ZonedDateTime,
        config: WidgetConfig
    ): PreviewClockFormatted {
        val hour24 = zdt.hour
        val minute = zdt.minute.toString().padStart(2, '0')
        val second = zdt.second.toString().padStart(2, '0')

        val hourDisplay = if (config.use24HourFormat) {
            hour24.toString().padStart(2, '0')
        } else {
            val h12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            h12.toString().padStart(2, '0')
        }

        val amPm = if (config.use24HourFormat) {
            ""
        } else {
            if (hour24 < 12) "SA" else "CH"
        }

        val primary = buildString {
            append(hourDisplay)
            append(":")
            append(minute)
            if (config.showSeconds) {
                append(":")
                append(second)
            }
        }

        return PreviewClockFormatted(
            primary = primary,
            hourPart = hourDisplay,
            minutePart = if (config.showSeconds) "$minute:$second" else minute,
            amPmPart = amPm
        )
    }

    /**
     * Returns the exact Android TextClock format patterns for 12h and 24h modes.
     */
    fun getTextClockFormat24h(showSeconds: Boolean): CharSequence {
        return if (showSeconds) "HH:mm:ss" else "HH:mm"
    }

    fun getTextClockFormat12h(showSeconds: Boolean): CharSequence {
        return if (showSeconds) "hh:mm:ss" else "hh:mm"
    }
}

data class PreviewClockFormatted(
    val primary: String,
    val hourPart: String,
    val minutePart: String,
    val amPmPart: String
)
