package com.lumiglass.widgets

import com.lumiglass.widgets.data.battery.BatteryRepository
import com.lumiglass.widgets.data.datetime.DateTimeRepository
import com.lumiglass.widgets.data.update.AppUpdateManager
import com.lumiglass.widgets.data.weather.OpenMeteoApiClient
import kotlinx.serialization.json.jsonObject
import com.lumiglass.widgets.model.BatteryInfo
import com.lumiglass.widgets.model.DateFormatStyle
import com.lumiglass.widgets.model.OfficialGlassPresets
import com.lumiglass.widgets.model.ResponsiveLayoutResolver
import com.lumiglass.widgets.model.TemperatureUnit
import com.lumiglass.widgets.model.WeatherInfo
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.renderer.GlassIconRenderer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class LumiGlassCoreEngineTest {

    @Test
    fun testBatteryTemperatureConversionAndInvalidFallback() {
        // 345 tenths of a degree Celsius -> 34.5°C
        val validTemp = BatteryRepository.parseBatteryTemperatureTenths(345)
        assertNotNull(validTemp)
        assertEquals(34.5f, validTemp!!, 0.01f)

        val validBattery = BatteryInfo(
            hasLevelData = true,
            levelPercent = 84,
            isCharging = true,
            hasValidTemperature = true,
            temperatureCelsius = validTemp
        )
        assertEquals("84%", validBattery.formatPercent())
        assertEquals("34.5°C", validBattery.formatTemperature(TemperatureUnit.CELSIUS))

        // Invalid sensor readings must return null and display "--°C" (never fake temperatures)
        assertNull(BatteryRepository.parseBatteryTemperatureTenths(0))
        assertNull(BatteryRepository.parseBatteryTemperatureTenths(-10))
        assertNull(BatteryRepository.parseBatteryTemperatureTenths(Int.MIN_VALUE))
        assertNull(BatteryRepository.parseBatteryTemperatureTenths(980))

        val missingTempBattery = BatteryInfo(
            hasLevelData = true,
            levelPercent = 50,
            isCharging = false,
            hasValidTemperature = false,
            temperatureCelsius = null
        )
        assertEquals("--°C", missingTempBattery.formatTemperature(TemperatureUnit.CELSIUS))
    }

    @Test
    fun testVietnameseDateFormattingStyles() {
        // Thursday, 19 November 2026
        val zdt = ZonedDateTime.of(2026, 11, 19, 14, 35, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"))

        val fullVi = DateTimeRepository.formatGregorianDate(
            zdt = zdt,
            style = DateFormatStyle.FULL_VIETNAMESE,
            useMonthInWords = true
        )
        assertEquals("Thứ Năm, 19 tháng 11 năm 2026", fullVi)

        val compactUpper = DateTimeRepository.formatGregorianDate(
            zdt = zdt,
            style = DateFormatStyle.COMPACT_UPPER_DOT,
            useMonthInWords = false
        )
        assertEquals("THỨ NĂM • 19/11", compactUpper)

        val shortStyle = DateTimeRepository.formatGregorianDate(
            zdt = zdt,
            style = DateFormatStyle.SHORT_WEEKDAY_MONTH,
            useMonthInWords = true
        )
        assertEquals("Th 5, 19 Thg 11", shortStyle)

        // Verify 5/7-day Calendar Strip around 19/11/2026 (11/10 Lunar)
        val weekCols = DateTimeRepository.buildWeekCalendarColumns(zdt)
        assertEquals(7, weekCols.size)

        // Index 1 (-2 days): Tuesday 17/11 -> 09/10 Lunar
        assertEquals(-2, weekCols[1].dayOffset)
        assertEquals("T3", weekCols[1].weekdayShortVi)
        assertEquals("17", weekCols[1].solarDayString)
        assertEquals("09/10", weekCols[1].lunarDayMonthString)

        // Index 3 (0 = Today): Thursday 19/11 -> 11/10 Lunar
        assertTrue(weekCols[3].isToday)
        assertEquals("T5•NAY", weekCols[3].weekdayShortVi)
        assertEquals("19", weekCols[3].solarDayString)
        assertEquals("11/10", weekCols[3].lunarDayMonthString)

        // Index 5 (+2 days): Saturday 21/11 -> 13/10 Lunar
        assertEquals(2, weekCols[5].dayOffset)
        assertEquals("T7", weekCols[5].weekdayShortVi)
        assertEquals("21", weekCols[5].solarDayString)
        assertEquals("13/10", weekCols[5].lunarDayMonthString)
    }

    @Test
    fun testOpenMeteoResponseParsingAndNoFakeFallback() {
        val sampleJson = """
            {
              "latitude": 21.03,
              "longitude": 105.85,
              "current": {
                "time": "2026-11-19T14:30",
                "temperature_2m": 26.4,
                "relative_humidity_2m": 72,
                "apparent_temperature": 28.1,
                "is_day": 1,
                "weather_code": 2
              },
              "daily": {
                "time": ["2026-11-19"],
                "temperature_2m_max": [28.9],
                "temperature_2m_min": [21.2],
                "precipitation_probability_max": [15]
              }
            }
        """.trimIndent()

        val client = OpenMeteoApiClient()
        val parsed = client.parseOpenMeteoJson(
            rawJson = sampleJson,
            cityId = "hanoi",
            cityNameVi = "Hà Nội",
            nowMillis = 1763537400000L
        )

        assertTrue(parsed.hasData)
        assertEquals("Hà Nội", parsed.cityNameVi)
        assertEquals(26.4, parsed.currentTempC!!, 0.01)
        assertEquals(28.9, parsed.highTempC!!, 0.01)
        assertEquals(21.2, parsed.lowTempC!!, 0.01)
        assertEquals(72, parsed.humidityPercent)
        assertEquals(15, parsed.rainProbabilityPercent)
        assertEquals("26°C", parsed.formatTemp(parsed.currentTempC, TemperatureUnit.CELSIUS))
        assertEquals("26°", parsed.formatTemp(parsed.currentTempC, TemperatureUnit.CELSIUS, degreeOnly = true))
        assertEquals("21°–29°", parsed.formatRangeCompact(TemperatureUnit.CELSIUS, degreeOnly = true))
        assertEquals("21°–29° • Mưa 15%", parsed.formatRangeAndPrecip(TemperatureUnit.CELSIUS, degreeOnly = true))
        assertEquals("Có mây rải rác", parsed.conditionTextVi)

        // When no data is available (API failure with no cache), must display "--°C"
        val emptyWeather = WeatherInfo(hasData = false, currentTempC = null)
        assertFalse(emptyWeather.hasData)
        assertEquals("--°C", emptyWeather.formatTemp(emptyWeather.currentTempC, TemperatureUnit.CELSIUS))
        assertEquals("--°", emptyWeather.formatTemp(emptyWeather.currentTempC, TemperatureUnit.CELSIUS, degreeOnly = true))
        assertEquals("--°–--°", emptyWeather.formatRangeCompact(TemperatureUnit.CELSIUS, degreeOnly = true))
    }

    @Test
    fun testResponsiveWidgetResizeResolution() {
        val mini = ResponsiveLayoutResolver.resolve(150, 72, WidgetSizeTier.ESSENTIAL_4X2)
        assertEquals(WidgetSizeTier.MINI_2X1, mini.effectiveTier)

        val compact = ResponsiveLayoutResolver.resolve(170, 165, WidgetSizeTier.ESSENTIAL_4X2)
        assertEquals(WidgetSizeTier.COMPACT_2X2, compact.effectiveTier)

        val essential = ResponsiveLayoutResolver.resolve(320, 150, WidgetSizeTier.ESSENTIAL_4X2)
        assertEquals(WidgetSizeTier.ESSENTIAL_4X2, essential.effectiveTier)

        val premium = ResponsiveLayoutResolver.resolve(320, 230, WidgetSizeTier.ESSENTIAL_4X2)
        assertEquals(WidgetSizeTier.PREMIUM_4X3, premium.effectiveTier)

        val dashboard = ResponsiveLayoutResolver.resolve(330, 310, WidgetSizeTier.ESSENTIAL_4X2)
        assertEquals(WidgetSizeTier.DASHBOARD_4X4, dashboard.effectiveTier)
    }

    @Test
    fun testIndependentWidgetConfigsSerialization() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val widget1 = OfficialGlassPresets.CRYSTAL_CLEAR.defaultConfig.copy(
            appWidgetId = 101,
            glassOpacity = 0.18f,
            cityId = "hanoi"
        )
        val widget2 = OfficialGlassPresets.MIDNIGHT_GLASS.defaultConfig.copy(
            appWidgetId = 102,
            glassOpacity = 0.72f,
            cityId = "hcmc"
        )

        val raw1 = json.encodeToString(widget1)
        val raw2 = json.encodeToString(widget2)

        val restored1 = json.decodeFromString<WidgetConfig>(raw1)
        val restored2 = json.decodeFromString<WidgetConfig>(raw2)

        assertEquals(101, restored1.appWidgetId)
        assertEquals("crystal_clear", restored1.presetId)
        assertEquals("hanoi", restored1.cityId)
        assertEquals(0.18f, restored1.glassOpacity, 0.001f)

        assertEquals(102, restored2.appWidgetId)
        assertEquals("midnight_glass", restored2.presetId)
        assertEquals("hcmc", restored2.cityId)
        assertEquals(0.72f, restored2.glassOpacity, 0.001f)
    }

    @Test
    fun testGitHubInAppUpdateVersionComparisonAndReleaseParsing() {
        assertTrue(AppUpdateManager.isRemoteVersionNewer("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateManager.isRemoteVersionNewer("1.0.0", "1.1.0"))
        assertTrue(AppUpdateManager.isRemoteVersionNewer("1.0.0", "v2.0.0"))
        assertFalse(AppUpdateManager.isRemoteVersionNewer("1.0.0", "v1.0.0"))
        assertFalse(AppUpdateManager.isRemoteVersionNewer("1.2.0", "v1.1.9"))

        val sampleReleaseJson = """
            {
              "tag_name": "v1.1.0",
              "name": "LumiGlass Widgets v1.1.0 - Liquid Glass & In-App Updater",
              "body": "Cập nhật trực tiếp trong ứng dụng từ GitHub Releases.",
              "published_at": "2026-11-19T08:30:00Z",
              "html_url": "https://github.com/tranvannhat-uwq/LumiGlass-Widgets/releases/tag/v1.1.0",
              "assets": [
                {
                  "name": "LumiGlass-Widgets-v1.1.0.apk",
                  "size": 18779136,
                  "browser_download_url": "https://github.com/tranvannhat-uwq/LumiGlass-Widgets/releases/download/v1.1.0/LumiGlass-Widgets-v1.1.0.apk"
                }
              ]
            }
        """.trimIndent()

        val manager = AppUpdateManager()
        val jsonObj = Json.parseToJsonElement(sampleReleaseJson).jsonObject
        val release = manager.parseReleaseJsonObject(jsonObj, currentVersionName = "1.0.0")

        assertTrue(release.isNewerThanCurrent)
        assertEquals("v1.1.0", release.tagName)
        assertEquals("1.1.0", release.versionName)
        assertEquals("LumiGlass-Widgets-v1.1.0.apk", release.apkFileName)
        assertEquals("17.91 MB", release.formatFileSizeMb())
    }

    @Test
    fun testStorageInfoFormatting() {
        val total = 108L * 1024L * 1024L * 1024L
        val used = (74.2 * 1024.0 * 1024.0 * 1024.0).toLong()
        val storage = com.lumiglass.widgets.data.storage.StorageInfo(
            totalBytes = total,
            availBytes = total - used,
            usedBytes = used,
            usedPercent = 68
        )

        assertEquals("74.2", storage.formatUsedGb())
        assertEquals("108", storage.formatTotalGb())
        assertEquals("74.2 / 108 GB", storage.formatUsedAndTotalGb())
        assertEquals("68%", storage.formatPercent())
    }

    @Test
    fun testTelemetryAlertColorsResolution() {
        // Battery
        val chargingColor = GlassIconRenderer.resolveBatteryLevelColorArgb(
            levelPercent = 92,
            isCharging = true,
            colorByLevel = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )
        val lowBatteryColor = GlassIconRenderer.resolveBatteryLevelColorArgb(
            levelPercent = 15,
            isCharging = false,
            colorByLevel = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )
        val normalBatteryColor = GlassIconRenderer.resolveBatteryLevelColorArgb(
            levelPercent = 80,
            isCharging = false,
            colorByLevel = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )

        assertEquals(0xFF00E5FF.toInt(), chargingColor)
        assertEquals(0xFFEF4444.toInt(), lowBatteryColor)
        assertEquals(0xFF22C55E.toInt(), normalBatteryColor)

        // Battery Temp
        val hotTempColor = GlassIconRenderer.resolveBatteryTempColorArgb(
            tempCelsius = 41.5f,
            warnByColor = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )
        val warmTempColor = GlassIconRenderer.resolveBatteryTempColorArgb(
            tempCelsius = 37.0f,
            warnByColor = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )
        val coolTempColor = GlassIconRenderer.resolveBatteryTempColorArgb(
            tempCelsius = 29.6f,
            warnByColor = true,
            fallbackArgb = 0xFFFFFFFF.toInt()
        )

        assertEquals(0xFFEF4444.toInt(), hotTempColor)
        assertEquals(0xFFF97316.toInt(), warmTempColor)
        assertEquals(0xFF38BDF8.toInt(), coolTempColor)

        // Storage
        val fullStorageColor = GlassIconRenderer.resolveStorageColorArgb(
            usedPercent = 94,
            warnByColor = true
        )
        val normalStorageColor = GlassIconRenderer.resolveStorageColorArgb(
            usedPercent = 68,
            warnByColor = true
        )
        assertEquals(0xFFEF4444.toInt(), fullStorageColor)
        assertEquals(0xFFC084FC.toInt(), normalStorageColor)

        // RAM
        val highRamColor = GlassIconRenderer.resolveRamColorArgb(
            usedPercent = 88,
            warnByColor = true
        )
        val normalRamColor = GlassIconRenderer.resolveRamColorArgb(
            usedPercent = 72,
            warnByColor = true
        )
        assertEquals(0xFFEF4444.toInt(), highRamColor)
        assertEquals(0xFFF59E0B.toInt(), normalRamColor)
    }

    @Test
    fun testNeonCyberObsidianPresetPresence() {
        val preset = OfficialGlassPresets.findById("neon_cyber_obsidian")
        assertEquals("neon_cyber_obsidian", preset.id)
        assertEquals("07", preset.codeNumber)
        assertEquals(com.lumiglass.widgets.model.TelemetryDisplayStyle.TRIO_GLASS_CARDS, preset.defaultConfig.telemetryDisplayStyle)
        assertTrue(preset.defaultConfig.showStorageCard)
        assertTrue(preset.defaultConfig.showBatteryCard)
        assertTrue(preset.defaultConfig.showRamCard)
    }
}
