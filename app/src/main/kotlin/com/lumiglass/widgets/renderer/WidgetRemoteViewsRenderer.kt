package com.lumiglass.widgets.renderer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.lumiglass.widgets.MainActivity
import com.lumiglass.widgets.R
import com.lumiglass.widgets.model.ContentAlignmentOption
import com.lumiglass.widgets.model.FontWeightOption
import com.lumiglass.widgets.model.ResponsiveLayoutResolver
import com.lumiglass.widgets.model.TimezoneMode
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetModuleType
import com.lumiglass.widgets.model.WidgetSizeTier

/**
 * Constructs responsive Android [RemoteViews] for any LumiGlass widget instance.
 * Combines procedural Liquid Glass background bitmaps, native [android.widget.TextClock] for zero-battery
 * real-time clock updates, custom glass weather/battery icons, and user-ordered modules.
 */
object WidgetRemoteViewsRenderer {

    fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        widthDp: Int,
        heightDp: Int,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot
    ): RemoteViews {
        val metrics = ResponsiveLayoutResolver.resolve(
            widthDp = widthDp,
            heightDp = heightDp,
            preferredTier = config.preferredSizeTier
        )
        val tier = metrics.effectiveTier
        val density = context.resources.displayMetrics.density

        val layoutResId = when (tier) {
            WidgetSizeTier.MINI_2X1 -> R.layout.widget_layout_2x1
            WidgetSizeTier.COMPACT_2X2 -> R.layout.widget_layout_2x2
            WidgetSizeTier.ESSENTIAL_4X2 -> R.layout.widget_layout_4x2
            WidgetSizeTier.PREMIUM_4X3 -> R.layout.widget_layout_4x3
            WidgetSizeTier.DASHBOARD_4X4 -> R.layout.widget_layout_4x4
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val palette = GlassSurfaceRenderer.resolvePalette(config, snapshot.isSystemDarkTheme)

        val hasRightContent = config.showWeather || config.showLunarDate ||
            (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp))

        val dividerMode = when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> {
                if ((config.showClock || config.showGregorianDate) && hasRightContent) {
                    GlassSurfaceRenderer.DividerZoneLayout.VERTICAL_SPLIT_4X2
                } else {
                    GlassSurfaceRenderer.DividerZoneLayout.NONE
                }
            }
            WidgetSizeTier.PREMIUM_4X3 -> {
                if (config.showWeather || (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp))) {
                    GlassSurfaceRenderer.DividerZoneLayout.PREMIUM_4X3
                } else {
                    GlassSurfaceRenderer.DividerZoneLayout.NONE
                }
            }
            WidgetSizeTier.DASHBOARD_4X4 -> GlassSurfaceRenderer.DividerZoneLayout.DASHBOARD_4X4
            else -> GlassSurfaceRenderer.DividerZoneLayout.NONE
        }

        // 1. Render and bind the Liquid Glass surface bitmap
        val glassBitmap = GlassSurfaceRenderer.renderGlassBitmap(
            widthDp = metrics.widthDp,
            heightDp = metrics.heightDp,
            density = density,
            config = config,
            isSystemDarkTheme = snapshot.isSystemDarkTheme,
            drawInternalDividersForTier = dividerMode
        )
        views.setImageViewBitmap(R.id.widget_glass_bg, glassBitmap)

        // 2. Apply responsive padding and alignment
        val padPx = (metrics.contentPaddingDp * density).toInt()
        views.setViewPadding(R.id.widget_content_root, padPx, padPx, padPx, padPx)

        val gravity = when (config.contentAlignment) {
            ContentAlignmentOption.START -> Gravity.START or Gravity.CENTER_VERTICAL
            ContentAlignmentOption.CENTER -> Gravity.CENTER
            ContentAlignmentOption.END -> Gravity.END or Gravity.CENTER_VERTICAL
        }
        if (tier != WidgetSizeTier.ESSENTIAL_4X2) {
            views.setInt(R.id.widget_content_root, "setGravity", gravity)
        }

        // 3. Configure Digital Clock (Native TextClock: 1-line or 2-line split)
        bindClockViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            config = config,
            primaryTextArgb = palette.primaryTextArgb,
            accentTextArgb = palette.accentTextArgb
        )

        // 4. Configure Gregorian Date & Vietnamese Lunar Date (respecting moduleOrder priority)
        bindDateAndLunarViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            config = config,
            snapshot = snapshot,
            primaryTextArgb = palette.primaryTextArgb,
            secondaryTextArgb = palette.secondaryTextArgb,
            accentTextArgb = palette.accentTextArgb
        )

        // 5. Configure Weather Module
        bindWeatherViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            density = density,
            config = config,
            snapshot = snapshot,
            primaryTextArgb = palette.primaryTextArgb,
            secondaryTextArgb = palette.secondaryTextArgb
        )

        // 6. Configure Battery % & Real Battery Temperature Module
        bindBatteryViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            density = density,
            config = config,
            snapshot = snapshot,
            primaryTextArgb = palette.primaryTextArgb,
            secondaryTextArgb = palette.secondaryTextArgb
        )

        // 7. Attach Click PendingIntent to open Widget Studio for this specific appWidgetId
        val editIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(MainActivity.EXTRA_OPEN_STUDIO_DIRECTLY, true)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 10_000,
            editIntent,
            pendingFlags
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }

    private fun bindClockViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        config: WidgetConfig,
        primaryTextArgb: Int,
        accentTextArgb: Int
    ) {
        if (!config.showClock) {
            views.setViewVisibility(R.id.widget_clock_single, View.GONE)
            views.setViewVisibility(R.id.widget_clock_split_container, View.GONE)
            return
        }

        val timeZoneId: String? = when (config.timezoneMode) {
            TimezoneMode.VIETNAM_UTC7 -> "Asia/Ho_Chi_Minh"
            TimezoneMode.SYSTEM -> null
        }

        val combinedScale = (scaleFactor * config.textScale).coerceIn(0.68f, 1.38f)
        val secondsFactor = if (config.showSeconds) 0.78f else 1.0f

        val baseSingleSp = when (tier) {
            WidgetSizeTier.MINI_2X1 -> 25f
            WidgetSizeTier.COMPACT_2X2 -> 34f
            WidgetSizeTier.ESSENTIAL_4X2 -> 40f
            WidgetSizeTier.PREMIUM_4X3 -> 46f
            WidgetSizeTier.DASHBOARD_4X4 -> 50f
        } * combinedScale * secondsFactor

        if (!config.splitClockTwoLines) {
            views.setViewVisibility(R.id.widget_clock_single, View.VISIBLE)
            views.setViewVisibility(R.id.widget_clock_split_container, View.GONE)

            val pattern = if (config.use24HourFormat) {
                if (config.showSeconds) "HH:mm:ss" else "HH:mm"
            } else {
                if (config.showSeconds) "hh:mm:ss a" else "hh:mm a"
            }
            val styledPattern = applyTypographySpans(pattern, config)
            views.setCharSequence(R.id.widget_clock_single, "setFormat24Hour", styledPattern)
            views.setCharSequence(R.id.widget_clock_single, "setFormat12Hour", styledPattern)
            views.setString(R.id.widget_clock_single, "setTimeZone", timeZoneId)
            views.setTextViewTextSize(R.id.widget_clock_single, TypedValue.COMPLEX_UNIT_SP, baseSingleSp)
            views.setTextColor(R.id.widget_clock_single, primaryTextArgb)
        } else {
            views.setViewVisibility(R.id.widget_clock_single, View.GONE)
            views.setViewVisibility(R.id.widget_clock_split_container, View.VISIBLE)

            val splitSp = (baseSingleSp * 0.88f).coerceAtLeast(18f)
            val hourPattern = applyTypographySpans(if (config.use24HourFormat) "HH" else "hh", config)
            val minPattern = applyTypographySpans(if (config.showSeconds) "mm:ss" else "mm", config)

            views.setCharSequence(R.id.widget_clock_split_hour, "setFormat24Hour", hourPattern)
            views.setCharSequence(R.id.widget_clock_split_hour, "setFormat12Hour", hourPattern)
            views.setString(R.id.widget_clock_split_hour, "setTimeZone", timeZoneId)
            views.setTextViewTextSize(R.id.widget_clock_split_hour, TypedValue.COMPLEX_UNIT_SP, splitSp)
            views.setTextColor(R.id.widget_clock_split_hour, primaryTextArgb)

            views.setCharSequence(R.id.widget_clock_split_minute, "setFormat24Hour", minPattern)
            views.setCharSequence(R.id.widget_clock_split_minute, "setFormat12Hour", minPattern)
            views.setString(R.id.widget_clock_split_minute, "setTimeZone", timeZoneId)
            views.setTextViewTextSize(R.id.widget_clock_split_minute, TypedValue.COMPLEX_UNIT_SP, splitSp)
            views.setTextColor(R.id.widget_clock_split_minute, accentTextArgb)
        }
    }

    private fun bindDateAndLunarViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        primaryTextArgb: Int,
        secondaryTextArgb: Int,
        accentTextArgb: Int
    ) {
        val combinedScale = (scaleFactor * config.textScale).coerceIn(0.72f, 1.32f)
        val dateSp = when (tier) {
            WidgetSizeTier.MINI_2X1 -> 11f
            WidgetSizeTier.COMPACT_2X2 -> 12f
            WidgetSizeTier.ESSENTIAL_4X2 -> 12.5f
            WidgetSizeTier.PREMIUM_4X3 -> 14f
            WidgetSizeTier.DASHBOARD_4X4 -> 14.5f
        } * combinedScale

        val lunarSp = (dateSp * 0.92f).coerceAtLeast(10f)

        val gregorianString = snapshot.gregorianDateText
        val lunarString = when (tier) {
            WidgetSizeTier.MINI_2X1, WidgetSizeTier.COMPACT_2X2 -> {
                snapshot.lunarDateInfo.formatCompact(config.showCanChi)
            }
            else -> {
                snapshot.lunarDateInfo.formatDisplay(
                    showCanChi = config.showCanChi,
                    showHolidays = config.showLunarHolidays
                )
            }
        }

        // Check user's module priority ordering between GREGORIAN_DATE and LUNAR_DATE
        val dateIdx = config.moduleOrder.indexOf(WidgetModuleType.GREGORIAN_DATE)
        val lunarIdx = config.moduleOrder.indexOf(WidgetModuleType.LUNAR_DATE)
        val lunarBeforeGregorian = (lunarIdx in 0 until dateIdx) && tier != WidgetSizeTier.ESSENTIAL_4X2

        if (!lunarBeforeGregorian) {
            if (config.showGregorianDate) {
                views.setViewVisibility(R.id.widget_date_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_date_text, applyTypographySpans(gregorianString, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_date_text, TypedValue.COMPLEX_UNIT_SP, dateSp)
                views.setTextColor(R.id.widget_date_text, secondaryTextArgb)
            } else {
                views.setViewVisibility(R.id.widget_date_text, View.GONE)
            }

            if (config.showLunarDate) {
                views.setViewVisibility(R.id.widget_lunar_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_lunar_text, applyTypographySpans(lunarString, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_lunar_text, TypedValue.COMPLEX_UNIT_SP, lunarSp)
                views.setTextColor(R.id.widget_lunar_text, accentTextArgb)
            } else {
                views.setViewVisibility(R.id.widget_lunar_text, View.GONE)
            }
        } else {
            // User prioritized Lunar Date above Gregorian Date
            if (config.showLunarDate) {
                views.setViewVisibility(R.id.widget_date_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_date_text, applyTypographySpans(lunarString, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_date_text, TypedValue.COMPLEX_UNIT_SP, dateSp)
                views.setTextColor(R.id.widget_date_text, accentTextArgb)
            } else {
                views.setViewVisibility(R.id.widget_date_text, View.GONE)
            }

            if (config.showGregorianDate) {
                views.setViewVisibility(R.id.widget_lunar_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_lunar_text, applyTypographySpans(gregorianString, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_lunar_text, TypedValue.COMPLEX_UNIT_SP, lunarSp)
                views.setTextColor(R.id.widget_lunar_text, secondaryTextArgb)
            } else {
                views.setViewVisibility(R.id.widget_lunar_text, View.GONE)
            }
        }
    }

    private fun bindWeatherViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        primaryTextArgb: Int,
        secondaryTextArgb: Int
    ) {
        if (tier == WidgetSizeTier.MINI_2X1 || !config.showWeather) {
            views.setViewVisibility(R.id.widget_weather_container, View.GONE)
            return
        }

        // In 2x2 Compact Glass, if both Weather and Battery are enabled, respect moduleOrder priority
        // when height is very tight (< 155dp)
        views.setViewVisibility(R.id.widget_weather_container, View.VISIBLE)

        val weather = snapshot.weatherInfo
        val iconSizeDp = (when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> 20
            WidgetSizeTier.ESSENTIAL_4X2 -> 28
            WidgetSizeTier.PREMIUM_4X3 -> 34
            WidgetSizeTier.DASHBOARD_4X4 -> 42
            else -> 24
        } * config.iconScale).toInt().coerceIn(16, 60)

        val iconBitmap = GlassIconRenderer.renderWeatherIconBitmap(
            category = weather.conditionCategory(),
            style = config.iconStyle,
            primaryTextArgb = primaryTextArgb,
            sizeDp = iconSizeDp,
            density = density
        )
        views.setImageViewBitmap(R.id.widget_weather_icon, iconBitmap)

        val tempText = weather.formatTemp(weather.currentTempC, config.temperatureUnit)
        val conditionText = if (weather.hasData) {
            "${weather.conditionTextVi} • ${weather.cityNameVi}"
        } else {
            "${weather.cityNameVi} • Chưa có dữ liệu"
        }

        val combinedScale = (scaleFactor * config.textScale).coerceIn(0.75f, 1.30f)
        val tempSp = when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> 12.5f
            WidgetSizeTier.ESSENTIAL_4X2 -> 15.5f
            WidgetSizeTier.PREMIUM_4X3 -> 17.5f
            WidgetSizeTier.DASHBOARD_4X4 -> 21f
            else -> 14f
        } * combinedScale

        val condSp = (tempSp * 0.72f).coerceIn(10f, 15f)

        views.setTextViewText(R.id.widget_weather_temp, applyTypographySpans(tempText, config))
        views.setTextViewTextSize(R.id.widget_weather_temp, TypedValue.COMPLEX_UNIT_SP, tempSp)
        views.setTextColor(R.id.widget_weather_temp, primaryTextArgb)

        views.setTextViewText(R.id.widget_weather_condition, applyTypographySpans(conditionText, config, isHeader = false))
        views.setTextViewTextSize(R.id.widget_weather_condition, TypedValue.COMPLEX_UNIT_SP, condSp)
        views.setTextColor(R.id.widget_weather_condition, secondaryTextArgb)

        if (tier == WidgetSizeTier.COMPACT_2X2) {
            views.setViewVisibility(R.id.widget_weather_details, View.GONE)
        } else {
            val detailParts = mutableListOf<String>()
            if (weather.hasData) {
                if (config.showHighLowTemp && weather.highTempC != null && weather.lowTempC != null) {
                    detailParts.add(weather.formatHighLow(config.temperatureUnit))
                }
                if (config.showHumidity && weather.humidityPercent != null) {
                    detailParts.add("Ẩm ${weather.humidityPercent}%")
                }
                if (config.showRainProbability && weather.rainProbabilityPercent != null) {
                    detailParts.add("Mưa ${weather.rainProbabilityPercent}%")
                }
                if (config.showFeelsLike && weather.feelsLikeC != null) {
                    detailParts.add("Cảm giác ${weather.formatTemp(weather.feelsLikeC, config.temperatureUnit)}")
                }
                if (weather.isFromOfflineCache && !weather.errorMessageVi.isNullOrBlank()) {
                    detailParts.add(weather.errorMessageVi)
                }
            } else {
                detailParts.add(weather.errorMessageVi ?: "Kiểm tra kết nối mạng để cập nhật")
            }

            if (detailParts.isEmpty()) {
                views.setViewVisibility(R.id.widget_weather_details, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_weather_details, View.VISIBLE)
                val detailsStr = detailParts.joinToString(" • ")
                views.setTextViewText(R.id.widget_weather_details, applyTypographySpans(detailsStr, config, isHeader = false))
                views.setTextViewTextSize(
                    R.id.widget_weather_details,
                    TypedValue.COMPLEX_UNIT_SP,
                    (condSp * 0.92f).coerceAtLeast(9.5f)
                )
                views.setTextColor(R.id.widget_weather_details, secondaryTextArgb)
            }
        }
    }

    private fun bindBatteryViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        primaryTextArgb: Int,
        secondaryTextArgb: Int
    ) {
        val showAnyBattery = config.batteryMonitoringEnabled &&
            (config.showBatteryPercent || config.showBatteryTemp)

        if (tier == WidgetSizeTier.MINI_2X1 || !showAnyBattery) {
            views.setViewVisibility(R.id.widget_battery_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_battery_container, View.VISIBLE)
        val battery = snapshot.batteryInfo
        val combinedScale = (scaleFactor * config.textScale).coerceIn(0.75f, 1.28f)

        if (config.showBatteryPercent) {
            views.setViewVisibility(R.id.widget_battery_gauge, View.VISIBLE)
            views.setViewVisibility(R.id.widget_battery_percent_text, View.VISIBLE)

            val gaugeBitmap = GlassIconRenderer.renderBatteryGaugeBitmap(
                batteryInfo = battery,
                style = config.batteryStyle,
                colorByLevel = config.colorBatteryByLevel,
                primaryTextArgb = primaryTextArgb,
                density = density
            )
            views.setImageViewBitmap(R.id.widget_battery_gauge, gaugeBitmap)

            val chargingPrefix = if (battery.isCharging) "⚡ " else ""
            val pctText = "$chargingPrefix${battery.formatPercent()}"
            val pctColor = GlassIconRenderer.resolveBatteryLevelColorArgb(
                levelPercent = battery.levelPercent,
                isCharging = battery.isCharging,
                colorByLevel = config.colorBatteryByLevel,
                fallbackArgb = primaryTextArgb
            )
            val pctSp = when (tier) {
                WidgetSizeTier.COMPACT_2X2 -> 11f
                WidgetSizeTier.ESSENTIAL_4X2 -> 11.5f
                WidgetSizeTier.PREMIUM_4X3 -> 13.5f
                WidgetSizeTier.DASHBOARD_4X4 -> 14.5f
                else -> 12f
            } * combinedScale

            views.setTextViewText(R.id.widget_battery_percent_text, applyTypographySpans(pctText, config))
            views.setTextViewTextSize(R.id.widget_battery_percent_text, TypedValue.COMPLEX_UNIT_SP, pctSp)
            views.setTextColor(R.id.widget_battery_percent_text, pctColor)
        } else {
            views.setViewVisibility(R.id.widget_battery_gauge, View.GONE)
            views.setViewVisibility(R.id.widget_battery_percent_text, View.GONE)
        }

        if (config.showBatteryTemp) {
            views.setViewVisibility(R.id.widget_battery_temp_text, View.VISIBLE)
            val rawTempStr = battery.formatTemperature(config.temperatureUnit)
            val tempLabel = when (tier) {
                WidgetSizeTier.DASHBOARD_4X4, WidgetSizeTier.PREMIUM_4X3 -> "Nhiệt độ pin: $rawTempStr"
                else -> if (config.showBatteryPercent) "• Pin $rawTempStr" else "Pin: $rawTempStr"
            }
            val tempColor = GlassIconRenderer.resolveBatteryTempColorArgb(
                tempCelsius = battery.temperatureCelsius,
                warnByColor = config.warnBatteryTempByColor,
                fallbackArgb = secondaryTextArgb
            )
            val tempSp = when (tier) {
                WidgetSizeTier.COMPACT_2X2 -> 10.5f
                WidgetSizeTier.ESSENTIAL_4X2 -> 11f
                WidgetSizeTier.PREMIUM_4X3 -> 11.5f
                WidgetSizeTier.DASHBOARD_4X4 -> 13f
                else -> 11f
            } * combinedScale

            views.setTextViewText(R.id.widget_battery_temp_text, applyTypographySpans(tempLabel, config, isHeader = false))
            views.setTextViewTextSize(R.id.widget_battery_temp_text, TypedValue.COMPLEX_UNIT_SP, tempSp)
            views.setTextColor(R.id.widget_battery_temp_text, tempColor)
        } else {
            views.setViewVisibility(R.id.widget_battery_temp_text, View.GONE)
        }
    }

    private fun applyTypographySpans(
        text: String,
        config: WidgetConfig,
        isHeader: Boolean = true
    ): CharSequence {
        if (text.isEmpty()) return text
        val spannable = SpannableString(text)
        spannable.setSpan(
            TypefaceSpan(config.fontStyle.androidFontFamily),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val styleInt = when {
            config.fontWeight == FontWeightOption.BOLD && isHeader -> Typeface.BOLD
            config.fontWeight == FontWeightOption.MEDIUM && isHeader -> Typeface.BOLD
            else -> Typeface.NORMAL
        }
        if (styleInt == Typeface.BOLD) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}
