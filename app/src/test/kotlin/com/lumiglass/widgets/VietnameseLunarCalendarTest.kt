package com.lumiglass.widgets

import com.lumiglass.widgets.data.lunar.VietnameseLunarCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseLunarCalendarTest {

    @Test
    fun testTetNguyenDanDatesAndCanChi() {
        // Mùng 1 Tết Quý Mão (22/01/2023)
        val tet2023 = VietnameseLunarCalendar.convertSolarToLunar(22, 1, 2023)
        assertEquals(1, tet2023.lunarDay)
        assertEquals(1, tet2023.lunarMonth)
        assertEquals(2023, tet2023.lunarYear)
        assertFalse(tet2023.isLeapMonth)
        assertEquals("Quý Mão", tet2023.yearCanChi)
        assertEquals("Mùng 1 Tết Nguyên Đán", tet2023.specialPhaseOrHolidayVi)

        // Mùng 1 Tết Giáp Thìn (10/02/2024)
        val tet2024 = VietnameseLunarCalendar.convertSolarToLunar(10, 2, 2024)
        assertEquals(1, tet2024.lunarDay)
        assertEquals(1, tet2024.lunarMonth)
        assertEquals(2024, tet2024.lunarYear)
        assertFalse(tet2024.isLeapMonth)
        assertEquals("Giáp Thìn", tet2024.yearCanChi)
        assertEquals("Mùng 1 Tết Nguyên Đán", tet2024.specialPhaseOrHolidayVi)

        // Mùng 1 Tết Ất Tỵ (29/01/2025)
        val tet2025 = VietnameseLunarCalendar.convertSolarToLunar(29, 1, 2025)
        assertEquals(1, tet2025.lunarDay)
        assertEquals(1, tet2025.lunarMonth)
        assertEquals(2025, tet2025.lunarYear)
        assertFalse(tet2025.isLeapMonth)
        assertEquals("Ất Tỵ", tet2025.yearCanChi)
        assertEquals("Mùng 1 Tết Nguyên Đán", tet2025.specialPhaseOrHolidayVi)

        // Mùng 1 Tết Bính Ngọ (17/02/2026)
        val tet2026 = VietnameseLunarCalendar.convertSolarToLunar(17, 2, 2026)
        assertEquals(1, tet2026.lunarDay)
        assertEquals(1, tet2026.lunarMonth)
        assertEquals(2026, tet2026.lunarYear)
        assertFalse(tet2026.isLeapMonth)
        assertEquals("Bính Ngọ", tet2026.yearCanChi)
        assertEquals("Mùng 1 Tết Nguyên Đán", tet2026.specialPhaseOrHolidayVi)
    }

    @Test
    fun testYearTransitionGiaoThua() {
        // 09/02/2024 is 30/12/2023 (Quý Mão - Đêm Giao Thừa)
        val giaoThua2024 = VietnameseLunarCalendar.convertSolarToLunar(9, 2, 2024)
        assertEquals(30, giaoThua2024.lunarDay)
        assertEquals(12, giaoThua2024.lunarMonth)
        assertEquals(2023, giaoThua2024.lunarYear)
        assertEquals("Quý Mão", giaoThua2024.yearCanChi)
        assertEquals("Đêm Giao Thừa", giaoThua2024.specialPhaseOrHolidayVi)

        // 16/02/2026 is 29/12/2025 (Ất Tỵ - Đêm Giao Thừa before Tết Bính Ngọ)
        val giaoThua2026 = VietnameseLunarCalendar.convertSolarToLunar(16, 2, 2026)
        assertEquals(29, giaoThua2026.lunarDay)
        assertEquals(12, giaoThua2026.lunarMonth)
        assertEquals(2025, giaoThua2026.lunarYear)
        assertEquals("Ất Tỵ", giaoThua2026.yearCanChi)
        assertEquals("Đêm Giao Thừa", giaoThua2026.specialPhaseOrHolidayVi)
    }

    @Test
    fun testLeapMonthsVietnamUtc7() {
        // 2023 has a leap month 2 (Tháng 2 nhuận)
        val normalMonth2_2023 = VietnameseLunarCalendar.convertSolarToLunar(20, 2, 2023)
        assertEquals(1, normalMonth2_2023.lunarDay)
        assertEquals(2, normalMonth2_2023.lunarMonth)
        assertFalse(normalMonth2_2023.isLeapMonth)

        val leapMonth2_2023 = VietnameseLunarCalendar.convertSolarToLunar(22, 3, 2023)
        assertEquals(1, leapMonth2_2023.lunarDay)
        assertEquals(2, leapMonth2_2023.lunarMonth)
        assertTrue(leapMonth2_2023.isLeapMonth)
        assertTrue(leapMonth2_2023.formatLunarShort().contains("Nhuận"))

        // 2025 has a leap month 6 (Tháng 6 nhuận)
        val normalMonth6_2025 = VietnameseLunarCalendar.convertSolarToLunar(25, 6, 2025)
        assertEquals(1, normalMonth6_2025.lunarDay)
        assertEquals(6, normalMonth6_2025.lunarMonth)
        assertFalse(normalMonth6_2025.isLeapMonth)

        val leapMonth6_2025 = VietnameseLunarCalendar.convertSolarToLunar(25, 7, 2025)
        assertEquals(1, leapMonth6_2025.lunarDay)
        assertEquals(6, leapMonth6_2025.lunarMonth)
        assertTrue(leapMonth6_2025.isLeapMonth)
    }

    @Test
    fun testReferenceDateNov19_2026() {
        // 19/11/2026 Gregorian is 11/10/2026 Lunar (Bính Ngọ)
        val nov19 = VietnameseLunarCalendar.convertSolarToLunar(19, 11, 2026)
        assertEquals(11, nov19.lunarDay)
        assertEquals(10, nov19.lunarMonth)
        assertEquals(2026, nov19.lunarYear)
        assertFalse(nov19.isLeapMonth)
        assertEquals("Bính Ngọ", nov19.yearCanChi)
        assertEquals("Âm lịch: 11/10 • Bính Ngọ", nov19.formatDisplay(showCanChi = true, showHolidays = false))
    }
}
