package com.lumiglass.widgets.data.lunar

import com.lumiglass.widgets.model.LunarDateInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Astronomical Vietnamese Lunar Calendar calculator (UTC+7 standard).
 * Based on the Ho Ngoc Duc / Jean Meeus astronomical calculation algorithms for Vietnam.
 * Works 100% offline with accurate New Moon, Solar Longitude (Tiết khí), and Leap Month handling.
 */
object VietnameseLunarCalendar {

    const val VIETNAM_TIMEZONE = 7.0

    private val CAN = arrayOf(
        "Giáp", "Ất", "Bính", "Đinh", "Mậu",
        "Kỷ", "Canh", "Tân", "Nhâm", "Quý"
    )

    private val CHI = arrayOf(
        "Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ",
        "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"
    )

    /**
     * Converts a Gregorian date (dd/mm/yyyy) into a Julian Day Number.
     */
    fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
        return jd
    }

    /**
     * Converts a Julian Day Number back to Gregorian [day, month, year].
     */
    fun jdToDate(jd: Int): IntArray {
        val a: Int
        val b: Int
        val c: Int
        if (jd > 2299160) {
            a = jd + 32044
            b = (4 * a + 3) / 146097
            c = a - (b * 146097) / 4
        } else {
            b = 0
            c = jd + 32082
        }
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = b * 100 + d - 4800 + m / 10
        return intArrayOf(day, month, year)
    }

    /**
     * Computes the Julian Day Number of the k-th New Moon since 1900-01-01 in the given timezone.
     */
    fun getNewMoonDay(k: Int, timeZone: Double = VIETNAM_TIMEZONE): Int {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180.0

        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)

        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3

        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 += 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))

        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }

        val jdNew = jd1 + c1 - deltat
        return floor(jdNew + 0.5 + timeZone / 24.0).toInt()
    }

    /**
     * Computes the sun longitude sector [0..11] at midnight local time of the given Julian Day Number.
     */
    fun getSunLongitude(jdn: Int, timeZone: Double = VIETNAM_TIMEZONE): Int {
        val t = (jdn - 2451545.5 - timeZone / 24.0) / 36525.0
        val t2 = t * t
        val dr = PI / 180.0

        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = l0 + dl
        l *= dr
        l -= PI * 2 * floor(l / (PI * 2))
        return floor(l / PI * 6).toInt()
    }

    /**
     * Finds the Julian Day Number of the start of Lunar Month 11 (containing Winter Solstice) in year yy.
     */
    fun getLunarMonth11(yy: Int, timeZone: Double = VIETNAM_TIMEZONE): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = getNewMoonDay(k, timeZone)
        val sunLong = getSunLongitude(nm, timeZone)
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone)
        }
        return nm
    }

    /**
     * Determines the offset (1..12) of the leap month after Lunar Month 11.
     */
    fun getLeapMonthOffset(a11: Int, timeZone: Double = VIETNAM_TIMEZONE): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }

    /**
     * Converts a Gregorian date (dd, mm, yy) to Vietnamese LunarDateInfo.
     */
    fun convertSolarToLunar(
        dd: Int,
        mm: Int,
        yy: Int,
        timeZone: Double = VIETNAM_TIMEZONE
    ): LunarDateInfo {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = getNewMoonDay(k + 1, timeZone)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone)
        }

        var a11 = getLunarMonth11(yy, timeZone)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1, timeZone)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1, timeZone)
        }

        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var isLeapMonth = false
        var lunarMonth = diff + 11

        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, timeZone)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    isLeapMonth = true
                }
            }
        }

        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }

        val yearCanChi = getYearCanChi(lunarYear)
        val monthCanChi = getMonthCanChi(lunarMonth, lunarYear)
        val dayCanChi = getDayCanChi(dayNumber)

        val isLastDayOfLunarYear = isGiaoThua(dayNumber, lunarMonth, isLeapMonth, timeZone)
        val specialLabel = resolveHolidayOrPhase(
            lunarDay = lunarDay,
            lunarMonth = lunarMonth,
            isLeapMonth = isLeapMonth,
            isGiaoThua = isLastDayOfLunarYear
        )

        return LunarDateInfo(
            solarDay = dd,
            solarMonth = mm,
            solarYear = yy,
            lunarDay = lunarDay,
            lunarMonth = lunarMonth,
            lunarYear = lunarYear,
            isLeapMonth = isLeapMonth,
            dayCanChi = dayCanChi,
            monthCanChi = monthCanChi,
            yearCanChi = yearCanChi,
            specialPhaseOrHolidayVi = specialLabel
        )
    }

    fun getYearCanChi(lunarYear: Int): String {
        val canIdx = ((lunarYear + 6) % 10 + 10) % 10
        val chiIdx = ((lunarYear + 8) % 12 + 12) % 12
        return "${CAN[canIdx]} ${CHI[chiIdx]}"
    }

    fun getMonthCanChi(lunarMonth: Int, lunarYear: Int): String {
        val canIdx = ((lunarYear * 12 + lunarMonth + 3) % 10 + 10) % 10
        val chiIdx = ((lunarMonth + 1) % 12 + 12) % 12
        return "${CAN[canIdx]} ${CHI[chiIdx]}"
    }

    fun getDayCanChi(jdn: Int): String {
        val canIdx = ((jdn + 9) % 10 + 10) % 10
        val chiIdx = ((jdn + 1) % 12 + 12) % 12
        return "${CAN[canIdx]} ${CHI[chiIdx]}"
    }

    private fun isGiaoThua(
        jdn: Int,
        lunarMonth: Int,
        isLeapMonth: Boolean,
        timeZone: Double
    ): Boolean {
        if (lunarMonth != 12) return false
        val nextSolar = jdToDate(jdn + 1)
        val nextDayNumber = jdn + 1
        val k = floor((nextDayNumber - 2415021.076998695) / 29.530588853).toInt()
        var nextMonthStart = getNewMoonDay(k + 1, timeZone)
        if (nextMonthStart > nextDayNumber) {
            nextMonthStart = getNewMoonDay(k, timeZone)
        }
        // If tomorrow starts a new lunar month after month 12 (or leap 12), today is Giao Thừa
        return nextMonthStart == nextDayNumber && (!isLeapMonth || nextSolar[1] <= 2)
    }

    private fun resolveHolidayOrPhase(
        lunarDay: Int,
        lunarMonth: Int,
        isLeapMonth: Boolean,
        isGiaoThua: Boolean
    ): String? {
        if (isGiaoThua) return "Đêm Giao Thừa"
        if (!isLeapMonth) {
            when {
                lunarMonth == 1 && lunarDay == 1 -> return "Mùng 1 Tết Nguyên Đán"
                lunarMonth == 1 && lunarDay == 2 -> return "Mùng 2 Tết Nguyên Đán"
                lunarMonth == 1 && lunarDay == 3 -> return "Mùng 3 Tết Nguyên Đán"
                lunarMonth == 1 && lunarDay == 15 -> return "Rằm Tháng Giêng"
                lunarMonth == 3 && lunarDay == 3 -> return "Tết Hàn Thực"
                lunarMonth == 3 && lunarDay == 10 -> return "Giỗ Tổ Hùng Vương"
                lunarMonth == 4 && lunarDay == 15 -> return "Lễ Phật Đản"
                lunarMonth == 5 && lunarDay == 5 -> return "Tết Đoan Ngọ"
                lunarMonth == 7 && lunarDay == 15 -> return "Lễ Vu Lan • Rằm Tháng 7"
                lunarMonth == 8 && lunarDay == 15 -> return "Tết Trung Thu"
                lunarMonth == 12 && lunarDay == 23 -> return "Ông Công Ông Táo"
            }
        }
        return when (lunarDay) {
            1 -> "Mùng 1 đầu tháng"
            15 -> "Ngày Rằm"
            else -> null
        }
    }
}
