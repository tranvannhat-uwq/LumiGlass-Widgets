package com.lumiglass.widgets.renderer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
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
import com.lumiglass.widgets.model.CustomSlotBounds
import com.lumiglass.widgets.model.ContentAlignmentOption
import com.lumiglass.widgets.model.FontWeightOption
import com.lumiglass.widgets.model.ResponsiveLayoutResolver
import com.lumiglass.widgets.model.SlotContentType
import com.lumiglass.widgets.model.TelemetryDisplayStyle
import com.lumiglass.widgets.model.TemperatureSymbolStyle
import com.lumiglass.widgets.model.TimezoneMode
import com.lumiglass.widgets.model.WeatherDisplayStyle
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetModuleType
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints

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
        snapshot: WidgetDataSnapshot,
        preview: Boolean = false,
        showEmptySlots: Boolean = false,
        bitmapDensityLimit: Float = 2f
    ): RemoteViews {
        val metrics = ResponsiveLayoutResolver.resolve(
            widthDp = widthDp,
            heightDp = heightDp,
            preferredTier = config.preferredSizeTier
        )
        val tier = metrics.effectiveTier
        val density = context.resources.displayMetrics.density
        val bitmapDensity = density.coerceAtMost(bitmapDensityLimit)

        if (config.useCustomSlots) {
            return buildCustomSlotsRemoteViews(
                context = context,
                appWidgetId = appWidgetId,
                widthDp = metrics.widthDp,
                heightDp = metrics.heightDp,
                tier = config.preferredSizeTier,
                density = bitmapDensity,
                config = config,
                snapshot = snapshot,
                preview = preview,
                showEmptySlots = showEmptySlots
            )
        }

        val layoutResId = when (tier) {
            WidgetSizeTier.MINI_2X1 -> R.layout.widget_layout_2x1
            WidgetSizeTier.COMPACT_2X2 -> R.layout.widget_layout_2x2
            WidgetSizeTier.ESSENTIAL_4X2 -> R.layout.widget_layout_4x2
            WidgetSizeTier.PREMIUM_4X3 -> R.layout.widget_layout_4x3
            WidgetSizeTier.DASHBOARD_4X4 -> R.layout.widget_layout_4x4
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val palette = GlassSurfaceRenderer.resolvePalette(config, snapshot.isSystemDarkTheme)

        val glassBitmap = GlassSurfaceRenderer.renderGlassBitmap(
            widthDp = metrics.widthDp,
            heightDp = metrics.heightDp,
            density = bitmapDensity,
            config = config,
            isSystemDarkTheme = snapshot.isSystemDarkTheme
        )
        views.setImageViewBitmap(R.id.widget_glass_bg, glassBitmap)

        // 2. Apply responsive padding and alignment (compact top/bottom padding for 4x2/4x3/4x4 so upper block sits higher)
        val horizPadPx = (metrics.contentPaddingDp * density).toInt()
        val vertPadPx = when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> (8.5f * metrics.scaleFactor * density).toInt()
            WidgetSizeTier.PREMIUM_4X3 -> (10f * metrics.scaleFactor * density).toInt()
            WidgetSizeTier.DASHBOARD_4X4 -> (12f * metrics.scaleFactor * density).toInt()
            else -> horizPadPx
        }
        views.setViewPadding(R.id.widget_content_root, horizPadPx, vertPadPx, horizPadPx, vertPadPx)
        val dense = metrics.heightDp < 130
        val contentConfig = if (dense) config.copy(
            showHumidity = false, showFeelsLike = false,
            showHighLowTemp = false, showRainProbability = false,
            splitClockTwoLines = config.splitClockTwoLines && metrics.heightDp >= 100
        ) else config

        val gravity = when (config.contentAlignment) {
            ContentAlignmentOption.START -> Gravity.START or Gravity.CENTER_VERTICAL
            ContentAlignmentOption.CENTER -> Gravity.CENTER
            ContentAlignmentOption.END -> Gravity.END or Gravity.CENTER_VERTICAL
        }
        if (tier == WidgetSizeTier.MINI_2X1 || tier == WidgetSizeTier.COMPACT_2X2) {
            views.setInt(R.id.widget_content_root, "setGravity", gravity)
        }

        // 3. Configure Digital Clock (Native TextClock: 1-line or 2-line split)
        bindClockViews(
            context = context,
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            contentWidthDp = metrics.widthDp - 2 * metrics.contentPaddingDp,
            contentHeightDp = metrics.heightDp - 2 * vertPadPx / density,
            config = contentConfig,
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
            config = contentConfig,
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

        // 6b. Configure Real-Time RAM Status Module (for 4x3 & 4x4)
        bindRamViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            config = config,
            snapshot = snapshot,
            primaryTextArgb = palette.primaryTextArgb,
            secondaryTextArgb = palette.secondaryTextArgb
        )

        // 6c. Configure Trio Glass Telemetry Cards (Pin + Bộ nhớ + RAM)
        bindTelemetryTrioViews(
            context = context,
            views = views,
            tier = tier,
            density = density,
            config = config,
            snapshot = snapshot
        )

        // 6d. Configure Bottom 5-Day / 7-Day Solar & Lunar Calendar Strip (for 4x2, 4x3, 4x4)
        bindWeekCalendarStripViews(
            views = views,
            tier = tier,
            scaleFactor = metrics.scaleFactor,
            config = config,
            snapshot = snapshot,
            primaryTextArgb = palette.primaryTextArgb,
            secondaryTextArgb = palette.secondaryTextArgb,
            accentTextArgb = palette.accentTextArgb
        )

        // 7. Attach Click PendingIntent to open Widget Studio for this specific appWidgetId
        if (preview) return views

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

        // 7b. Attach Weather App launcher to Weather module container (opens system/installed weather app)
        val weatherPendingIntent = createWeatherAppPendingIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_weather_container, weatherPendingIntent)

        return views
    }

    private fun bindClockViews(
        context: Context,
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        contentWidthDp: Int,
        contentHeightDp: Float,
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

        val combinedScale = (scaleFactor * config.textScale * config.clockScale).coerceIn(0.55f, 1.65f)
        val secondsFactor = if (config.showSeconds) 0.78f else 1.0f

        val baseSingleSp = when (tier) {
            WidgetSizeTier.MINI_2X1 -> 26f
            WidgetSizeTier.COMPACT_2X2 -> 34f
            WidgetSizeTier.ESSENTIAL_4X2 -> 36f
            WidgetSizeTier.PREMIUM_4X3 -> 42f
            WidgetSizeTier.DASHBOARD_4X4 -> 46f
        } * combinedScale * secondsFactor
        val halfWidth = tier == WidgetSizeTier.ESSENTIAL_4X2 || tier == WidgetSizeTier.PREMIUM_4X3
        val clockWidth = if (halfWidth) contentWidthDp * .48f - 8f else contentWidthDp - 5f
        val clockHeight = contentHeightDp * if (halfWidth || tier == WidgetSizeTier.DASHBOARD_4X4) .38f else .55f

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
            views.setTextViewTextSize(R.id.widget_clock_single, TypedValue.COMPLEX_UNIT_PX,
                fittedClockPx(context, pattern, config, baseSingleSp, clockWidth, clockHeight))
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
            views.setTextViewTextSize(R.id.widget_clock_split_hour, TypedValue.COMPLEX_UNIT_PX,
                fittedClockPx(context, "HH", config, splitSp, clockWidth, clockHeight * .60f))
            views.setTextColor(R.id.widget_clock_split_hour, primaryTextArgb)

            views.setCharSequence(R.id.widget_clock_split_minute, "setFormat24Hour", minPattern)
            views.setCharSequence(R.id.widget_clock_split_minute, "setFormat12Hour", minPattern)
            views.setString(R.id.widget_clock_split_minute, "setTimeZone", timeZoneId)
            views.setTextViewTextSize(R.id.widget_clock_split_minute, TypedValue.COMPLEX_UNIT_PX,
                fittedClockPx(context, if (config.showSeconds) "mm:ss" else "mm",
                    config, splitSp, clockWidth, clockHeight * .60f))
            views.setTextColor(R.id.widget_clock_split_minute, accentTextArgb)
        }
    }

    private fun fittedClockPx(
        context: Context,
        pattern: String,
        config: WidgetConfig,
        targetSp: Float,
        widthDp: Float,
        heightDp: Float
    ): Float {
        val metrics = context.resources.displayMetrics
        val sample = pattern.replace('H', '8').replace('h', '8')
            .replace('m', '8').replace('s', '8').replace("a", "CH")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(config.fontStyle.androidFontFamily, Typeface.BOLD)
            textSize = 100f
        }
        val measured = paint.fontMetrics
        return minOf(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, targetSp, metrics),
            widthDp.coerceAtLeast(1f) * metrics.density * 100f / paint.measureText(sample),
            heightDp.coerceAtLeast(1f) * metrics.density * 100f / (measured.bottom - measured.top)
        )
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
        val combinedScale = (scaleFactor * config.textScale * config.dateScale).coerceIn(0.55f, 1.55f)
        val dateSp = when (tier) {
            WidgetSizeTier.MINI_2X1 -> 11f
            WidgetSizeTier.COMPACT_2X2 -> 12f
            WidgetSizeTier.ESSENTIAL_4X2 -> 11.8f
            WidgetSizeTier.PREMIUM_4X3 -> 13f
            WidgetSizeTier.DASHBOARD_4X4 -> 14f
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

        // In rectangular widgets with bottom week calendar strip, the lunar date is already displayed in the bottom strip.
        // Therefore, hide the duplicate upper lunar text under the temperature.
        val hasBottomStrip = config.showWeekCalendarStrip &&
            (tier == WidgetSizeTier.ESSENTIAL_4X2 || tier == WidgetSizeTier.PREMIUM_4X3 || tier == WidgetSizeTier.DASHBOARD_4X4)
        val showUpperLunar = config.showLunarDate && !hasBottomStrip

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

            if (showUpperLunar) {
                views.setViewVisibility(R.id.widget_lunar_text, View.VISIBLE)
                views.setTextViewText(R.id.widget_lunar_text, applyTypographySpans(lunarString, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_lunar_text, TypedValue.COMPLEX_UNIT_SP, lunarSp)
                views.setTextColor(R.id.widget_lunar_text, accentTextArgb)
            } else {
                views.setViewVisibility(R.id.widget_lunar_text, View.GONE)
            }
        } else {
            // User prioritized Lunar Date above Gregorian Date
            if (showUpperLunar) {
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
            WidgetSizeTier.ESSENTIAL_4X2 -> 26
            WidgetSizeTier.PREMIUM_4X3 -> 32
            WidgetSizeTier.DASHBOARD_4X4 -> 34
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

        val degreeOnly = config.temperatureSymbolStyle == TemperatureSymbolStyle.DEGREE_ONLY
        val formattedTemp = weather.formatTemp(weather.currentTempC, config.temperatureUnit, degreeOnly = degreeOnly)

        val combinedScale = (scaleFactor * config.textScale * config.weatherScale).coerceIn(0.60f, 1.50f)
        val tempSp = when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> 12.5f
            WidgetSizeTier.ESSENTIAL_4X2 -> 15.5f
            WidgetSizeTier.PREMIUM_4X3 -> 17f
            WidgetSizeTier.DASHBOARD_4X4 -> 19f
            else -> 14f
        } * combinedScale

        val condSp = when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> 10.5f
            WidgetSizeTier.ESSENTIAL_4X2 -> 10.8f
            WidgetSizeTier.PREMIUM_4X3 -> 11.5f
            WidgetSizeTier.DASHBOARD_4X4 -> 13.0f
            else -> 11f
        } * combinedScale

        when (config.weatherDisplayStyle) {
            WeatherDisplayStyle.MAGIC_CARD -> {
                if (config.showWeatherCardBackground) {
                    views.setInt(R.id.widget_weather_container, "setBackgroundResource", R.drawable.bg_glass_weather_subcard)
                } else {
                    views.setInt(R.id.widget_weather_container, "setBackgroundResource", 0)
                }

                // Row 1: Weather Condition ("Nhiều mây") & Temperature ("15°")
                val condText = if (weather.hasData) weather.conditionTextVi else "${weather.cityNameVi} • Chờ dữ liệu"
                views.setTextViewText(R.id.widget_weather_condition, applyTypographySpans(condText, config, isHeader = true))
                views.setTextViewTextSize(R.id.widget_weather_condition, TypedValue.COMPLEX_UNIT_SP, condSp * 1.20f)
                views.setTextColor(R.id.widget_weather_condition, primaryTextArgb)

                views.setTextViewText(R.id.widget_weather_temp, applyTypographySpans(formattedTemp, config, isHeader = true))
                views.setTextViewTextSize(R.id.widget_weather_temp, TypedValue.COMPLEX_UNIT_SP, tempSp)
                views.setTextColor(R.id.widget_weather_temp, primaryTextArgb)

                // Row 2: Range & Rain ("14°–22° • Mưa 0%")
                if (tier == WidgetSizeTier.COMPACT_2X2) {
                    views.setViewVisibility(R.id.widget_weather_details, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_weather_details, View.VISIBLE)
                    val rangeDetails = weather.formatRangeAndPrecip(config.temperatureUnit, degreeOnly = degreeOnly)
                    views.setTextViewText(R.id.widget_weather_details, applyTypographySpans(rangeDetails, config, isHeader = false))
                    views.setTextViewTextSize(
                        R.id.widget_weather_details,
                        TypedValue.COMPLEX_UNIT_SP,
                        (condSp * 0.95f).coerceAtLeast(9.5f)
                    )
                    views.setTextColor(R.id.widget_weather_details, secondaryTextArgb)
                }
            }

            WeatherDisplayStyle.COMPACT_RANGE -> {
                views.setInt(R.id.widget_weather_container, "setBackgroundResource", 0)

                views.setTextViewText(R.id.widget_weather_temp, applyTypographySpans(formattedTemp, config, isHeader = true))
                views.setTextViewTextSize(R.id.widget_weather_temp, TypedValue.COMPLEX_UNIT_SP, tempSp)
                views.setTextColor(R.id.widget_weather_temp, primaryTextArgb)

                val condText = if (weather.hasData) weather.conditionTextVi else weather.cityNameVi
                views.setTextViewText(R.id.widget_weather_condition, applyTypographySpans(condText, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_weather_condition, TypedValue.COMPLEX_UNIT_SP, condSp)
                views.setTextColor(R.id.widget_weather_condition, secondaryTextArgb)

                if (tier == WidgetSizeTier.COMPACT_2X2) {
                    views.setViewVisibility(R.id.widget_weather_details, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_weather_details, View.VISIBLE)
                    val rangeCompact = weather.formatRangeCompact(config.temperatureUnit, degreeOnly = degreeOnly)
                    views.setTextViewText(R.id.widget_weather_details, applyTypographySpans(rangeCompact, config, isHeader = false))
                    views.setTextViewTextSize(
                        R.id.widget_weather_details,
                        TypedValue.COMPLEX_UNIT_SP,
                        (condSp * 0.92f).coerceAtLeast(9.5f)
                    )
                    views.setTextColor(R.id.widget_weather_details, secondaryTextArgb)
                }
            }

            WeatherDisplayStyle.CLASSIC_DETAILED -> {
                views.setInt(R.id.widget_weather_container, "setBackgroundResource", 0)

                views.setTextViewText(R.id.widget_weather_temp, applyTypographySpans(formattedTemp, config, isHeader = true))
                views.setTextViewTextSize(R.id.widget_weather_temp, TypedValue.COMPLEX_UNIT_SP, tempSp)
                views.setTextColor(R.id.widget_weather_temp, primaryTextArgb)

                val conditionText = if (weather.hasData) {
                    "${weather.conditionTextVi} • ${weather.cityNameVi}"
                } else {
                    "${weather.cityNameVi} • Chưa có dữ liệu"
                }
                views.setTextViewText(R.id.widget_weather_condition, applyTypographySpans(conditionText, config, isHeader = false))
                views.setTextViewTextSize(R.id.widget_weather_condition, TypedValue.COMPLEX_UNIT_SP, condSp)
                views.setTextColor(R.id.widget_weather_condition, secondaryTextArgb)

                if (tier == WidgetSizeTier.COMPACT_2X2) {
                    views.setViewVisibility(R.id.widget_weather_details, View.GONE)
                } else {
                    val detailParts = mutableListOf<String>()
                    if (weather.hasData) {
                        if (config.showHighLowTemp && weather.highTempC != null && weather.lowTempC != null) {
                            detailParts.add(weather.formatHighLow(config.temperatureUnit, degreeOnly = degreeOnly))
                        }
                        if (config.showHumidity && weather.humidityPercent != null) {
                            detailParts.add("Ẩm ${weather.humidityPercent}%")
                        }
                        if (config.showRainProbability && weather.rainProbabilityPercent != null) {
                            detailParts.add("Mưa ${weather.rainProbabilityPercent}%")
                        }
                        if (config.showFeelsLike && weather.feelsLikeC != null) {
                            detailParts.add("Cảm giác ${weather.formatTemp(weather.feelsLikeC, config.temperatureUnit, degreeOnly = degreeOnly)}")
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
        val isTrioActive = config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS &&
            (tier == WidgetSizeTier.ESSENTIAL_4X2 || tier == WidgetSizeTier.PREMIUM_4X3 || tier == WidgetSizeTier.DASHBOARD_4X4)

        if (isTrioActive) {
            views.setViewVisibility(R.id.widget_battery_container, View.GONE)
            return
        }

        val showAnyBattery = config.batteryMonitoringEnabled &&
            (config.showBatteryPercent || config.showBatteryTemp)

        if (tier == WidgetSizeTier.MINI_2X1 || !showAnyBattery) {
            views.setViewVisibility(R.id.widget_battery_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_battery_container, View.VISIBLE)
        val battery = snapshot.batteryInfo
        val combinedScale = (scaleFactor * config.textScale * config.batteryScale).coerceIn(0.60f, 1.50f)

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
                WidgetSizeTier.COMPACT_2X2 -> 10.5f
                WidgetSizeTier.ESSENTIAL_4X2 -> 11.0f
                WidgetSizeTier.PREMIUM_4X3 -> 12.5f
                WidgetSizeTier.DASHBOARD_4X4 -> 13.0f
                else -> 11.5f
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
                WidgetSizeTier.ESSENTIAL_4X2 -> 10.5f
                WidgetSizeTier.PREMIUM_4X3 -> 11.5f
                WidgetSizeTier.DASHBOARD_4X4 -> 12.5f
                else -> 11f
            } * combinedScale

            views.setTextViewText(R.id.widget_battery_temp_text, applyTypographySpans(tempLabel, config, isHeader = false))
            views.setTextViewTextSize(R.id.widget_battery_temp_text, TypedValue.COMPLEX_UNIT_SP, tempSp)
            views.setTextColor(R.id.widget_battery_temp_text, tempColor)
        } else {
            views.setViewVisibility(R.id.widget_battery_temp_text, View.GONE)
        }
    }

    private fun bindRamViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        primaryTextArgb: Int,
        secondaryTextArgb: Int
    ) {
        val isTrioActive = config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS &&
            (tier == WidgetSizeTier.ESSENTIAL_4X2 || tier == WidgetSizeTier.PREMIUM_4X3 || tier == WidgetSizeTier.DASHBOARD_4X4)

        if (isTrioActive) {
            views.setViewVisibility(R.id.widget_ram_container, View.GONE)
            return
        }

        val showRam = config.showRamStatus && (tier == WidgetSizeTier.PREMIUM_4X3 || tier == WidgetSizeTier.DASHBOARD_4X4)
        if (!showRam) {
            views.setViewVisibility(R.id.widget_ram_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_ram_container, View.VISIBLE)
        val ram = snapshot.ramInfo
        val text = if (tier == WidgetSizeTier.DASHBOARD_4X4) {
            ram.formatDetailed()
        } else {
            ram.formatMedium()
        }

        val combinedScale = (scaleFactor * config.textScale * config.ramScale).coerceIn(0.60f, 1.45f)
        val ramSp = when (tier) {
            WidgetSizeTier.DASHBOARD_4X4 -> 12f
            else -> 11f
        } * combinedScale

        views.setTextViewText(R.id.widget_ram_text, applyTypographySpans(text, config, isHeader = false))
        views.setTextViewTextSize(R.id.widget_ram_text, TypedValue.COMPLEX_UNIT_SP, ramSp)
        views.setTextColor(R.id.widget_ram_text, secondaryTextArgb)
    }

    private fun bindTelemetryTrioViews(
        context: Context,
        views: RemoteViews,
        tier: WidgetSizeTier,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot
    ) {
        val isEligibleTier = tier == WidgetSizeTier.ESSENTIAL_4X2 ||
            tier == WidgetSizeTier.PREMIUM_4X3 ||
            tier == WidgetSizeTier.DASHBOARD_4X4

        val showTrio = isEligibleTier && config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS

        if (!showTrio) {
            views.setViewVisibility(R.id.widget_telemetry_trio_row, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_telemetry_trio_row, View.VISIBLE)

        val cardHeightDp = when (tier) {
            WidgetSizeTier.PREMIUM_4X3 -> 76
            WidgetSizeTier.DASHBOARD_4X4 -> 80
            else -> 70
        }

        // 1. Battery Card
        if (config.showBatteryCard && config.batteryMonitoringEnabled) {
            views.setViewVisibility(R.id.widget_card_battery, View.VISIBLE)
            val bBmp = GlassIconRenderer.renderBatteryTrioCardBitmap(
                battery = snapshot.batteryInfo,
                config = config,
                density = density,
                targetWidthDp = 68,
                targetHeightDp = cardHeightDp
            )
            views.setImageViewBitmap(R.id.widget_card_battery, bBmp)

            val bIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            val bPending = PendingIntent.getActivity(
                context, 101, bIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_card_battery, bPending)
        } else {
            views.setViewVisibility(R.id.widget_card_battery, View.GONE)
        }

        // 2. Internal Storage Card (Bộ nhớ trong)
        if (config.showStorageCard) {
            views.setViewVisibility(R.id.widget_card_storage, View.VISIBLE)
            val sBmp = GlassIconRenderer.renderStorageTrioCardBitmap(
                storage = snapshot.storageInfo,
                config = config,
                density = density,
                targetWidthDp = 68,
                targetHeightDp = cardHeightDp
            )
            views.setImageViewBitmap(R.id.widget_card_storage, sBmp)

            val sIntent = Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            val sPending = PendingIntent.getActivity(
                context, 102, sIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_card_storage, sPending)
        } else {
            views.setViewVisibility(R.id.widget_card_storage, View.GONE)
        }

        // 3. RAM Card
        if (config.showRamCard && config.showRamStatus) {
            views.setViewVisibility(R.id.widget_card_ram, View.VISIBLE)
            val rBmp = GlassIconRenderer.renderRamTrioCardBitmap(
                ram = snapshot.ramInfo,
                config = config,
                density = density,
                targetWidthDp = 68,
                targetHeightDp = cardHeightDp
            )
            views.setImageViewBitmap(R.id.widget_card_ram, rBmp)

            val rIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
            val rPending = PendingIntent.getActivity(
                context, 103, rIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_card_ram, rPending)
        } else {
            views.setViewVisibility(R.id.widget_card_ram, View.GONE)
        }
    }

    private data class CalendarColViewIds(
        val boxId: Int,
        val weekdayId: Int,
        val solarId: Int,
        val lunarId: Int
    )

    private val CALENDAR_COLUMN_VIEW_IDS = listOf(
        CalendarColViewIds(R.id.cal_col_0_box, R.id.cal_col_0_weekday, R.id.cal_col_0_solar, R.id.cal_col_0_lunar),
        CalendarColViewIds(R.id.cal_col_1_box, R.id.cal_col_1_weekday, R.id.cal_col_1_solar, R.id.cal_col_1_lunar),
        CalendarColViewIds(R.id.cal_col_2_box, R.id.cal_col_2_weekday, R.id.cal_col_2_solar, R.id.cal_col_2_lunar),
        CalendarColViewIds(R.id.cal_col_3_box, R.id.cal_col_3_weekday, R.id.cal_col_3_solar, R.id.cal_col_3_lunar),
        CalendarColViewIds(R.id.cal_col_4_box, R.id.cal_col_4_weekday, R.id.cal_col_4_solar, R.id.cal_col_4_lunar),
        CalendarColViewIds(R.id.cal_col_5_box, R.id.cal_col_5_weekday, R.id.cal_col_5_solar, R.id.cal_col_5_lunar),
        CalendarColViewIds(R.id.cal_col_6_box, R.id.cal_col_6_weekday, R.id.cal_col_6_solar, R.id.cal_col_6_lunar)
    )

    private fun bindWeekCalendarStripViews(
        views: RemoteViews,
        tier: WidgetSizeTier,
        scaleFactor: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        primaryTextArgb: Int,
        secondaryTextArgb: Int,
        accentTextArgb: Int
    ) {
        if (tier == WidgetSizeTier.MINI_2X1 || tier == WidgetSizeTier.COMPACT_2X2) return

        val columns = snapshot.weekCalendarColumns
        if (!config.showWeekCalendarStrip || columns.size < 7) {
            views.setViewVisibility(R.id.widget_week_strip_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_week_strip_container, View.VISIBLE)
        val useSevenDays = config.weekCalendarDayCount >= 7
        val combinedScale = (scaleFactor * config.textScale * config.calendarStripScale).coerceIn(0.60f, 1.45f)

        val baseWeekdaySp = when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> 9.5f
            WidgetSizeTier.PREMIUM_4X3 -> 10.0f
            WidgetSizeTier.DASHBOARD_4X4 -> 10.5f
            else -> 9.5f
        } * combinedScale

        val baseSolarSp = when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> 13.0f
            WidgetSizeTier.PREMIUM_4X3 -> 14.0f
            WidgetSizeTier.DASHBOARD_4X4 -> 15.0f
            else -> 13.0f
        } * combinedScale

        val baseLunarSp = when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> 9.0f
            WidgetSizeTier.PREMIUM_4X3 -> 9.5f
            WidgetSizeTier.DASHBOARD_4X4 -> 10.0f
            else -> 9.0f
        } * combinedScale

        for (i in 0 until 7) {
            val ids = CALENDAR_COLUMN_VIEW_IDS[i]
            val isEdgeCol = (i == 0 || i == 6)
            if (isEdgeCol && !useSevenDays) {
                views.setViewVisibility(ids.boxId, View.GONE)
                continue
            }

            views.setViewVisibility(ids.boxId, View.VISIBLE)
            val col = columns[i]

            val wdColor = if (col.isToday) primaryTextArgb else secondaryTextArgb
            val solarColor = primaryTextArgb
            val lunarColor = if (col.isToday) accentTextArgb else secondaryTextArgb

            views.setTextViewText(ids.weekdayId, applyTypographySpans(col.weekdayShortVi, config, isHeader = col.isToday))
            views.setTextViewTextSize(ids.weekdayId, TypedValue.COMPLEX_UNIT_SP, baseWeekdaySp)
            views.setTextColor(ids.weekdayId, wdColor)

            val solarSp = if (col.isToday) baseSolarSp * 1.08f else baseSolarSp
            views.setTextViewText(ids.solarId, applyTypographySpans(col.solarDayString, config, isHeader = true))
            views.setTextViewTextSize(ids.solarId, TypedValue.COMPLEX_UNIT_SP, solarSp)
            views.setTextColor(ids.solarId, solarColor)

            val lunarText = if (config.showLunarDate) col.lunarDayMonthString else ""
            if (lunarText.isNotEmpty()) {
                views.setViewVisibility(ids.lunarId, View.VISIBLE)
                views.setTextViewText(ids.lunarId, applyTypographySpans(lunarText, config, isHeader = col.isToday))
                views.setTextViewTextSize(ids.lunarId, TypedValue.COMPLEX_UNIT_SP, baseLunarSp)
                views.setTextColor(ids.lunarId, lunarColor)
            } else {
                views.setViewVisibility(ids.lunarId, View.GONE)
            }
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

    /**
     * Resolves and creates a [PendingIntent] to launch the device's installed Weather app
     * (e.g. Samsung Weather, Xiaomi Weather, ColorOS, Pixel Weather, or Google Weather).
     * Falls back to opening Widget Studio if no dedicated weather app is found.
     */
    private fun createWeatherAppPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val pm = context.packageManager
        val candidatePackages = listOf(
            "com.sec.android.daemonapp",              // Samsung Weather
            "com.miui.weather2",                      // Xiaomi / Redmi / POCO
            "com.coloros.weather.service",            // OPPO / Realme
            "com.coloros.weather2",                   // ColorOS
            "com.vivo.weather",                       // Vivo
            "com.huawei.android.totemweather",        // Huawei
            "com.asus.weather",                       // Asus
            "com.lenovo.weather",                     // Lenovo / Motorola
            "com.google.android.apps.weather",        // Google Weather app (Pixel)
            "com.accuweather.android",                // AccuWeather
            "com.weather.Weather",                    // The Weather Channel
            "com.yahoo.mobile.client.android.weather" // Yahoo Weather
        )

        var targetIntent: Intent? = null
        for (pkg in candidatePackages) {
            val launch = runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull()
            if (launch != null) {
                targetIntent = launch
                break
            }
        }

        if (targetIntent == null) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/search?q=thời+tiết")
            }
            if (webIntent.resolveActivity(pm) != null) {
                targetIntent = webIntent
            }
        }

        if (targetIntent == null) {
            targetIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(MainActivity.EXTRA_OPEN_STUDIO_DIRECTLY, true)
            }
        }

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(
            context,
            appWidgetId + 20_000,
            targetIntent,
            flags
        )
    }

    fun customSlotContainerId(index: Int): Int = CUSTOM_SLOT_VIEW_IDS[index].containerId

    private data class CustomSlotViewIds(
        val slotId: String,
        val containerId: Int,
        val bitmapId: Int,
        val clockId: Int
    )

    private val CUSTOM_SLOT_VIEW_IDS = listOf(
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_0, R.id.custom_slot_0_container, R.id.custom_slot_0_bitmap, R.id.custom_slot_0_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_1, R.id.custom_slot_1_container, R.id.custom_slot_1_bitmap, R.id.custom_slot_1_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_2, R.id.custom_slot_2_container, R.id.custom_slot_2_bitmap, R.id.custom_slot_2_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_3, R.id.custom_slot_3_container, R.id.custom_slot_3_bitmap, R.id.custom_slot_3_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_4, R.id.custom_slot_4_container, R.id.custom_slot_4_bitmap, R.id.custom_slot_4_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_5, R.id.custom_slot_5_container, R.id.custom_slot_5_bitmap, R.id.custom_slot_5_clock),
        CustomSlotViewIds(WidgetSlotBlueprints.SLOT_6, R.id.custom_slot_6_container, R.id.custom_slot_6_bitmap, R.id.custom_slot_6_clock)
    )

    private fun buildCustomSlotsRemoteViews(
        context: Context,
        appWidgetId: Int,
        widthDp: Int,
        heightDp: Int,
        tier: WidgetSizeTier,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        preview: Boolean,
        showEmptySlots: Boolean
    ): RemoteViews {
        val layoutResId = when (tier) {
            WidgetSizeTier.MINI_2X1 -> R.layout.widget_custom_2x1
            WidgetSizeTier.COMPACT_2X2 -> R.layout.widget_custom_2x2
            WidgetSizeTier.ESSENTIAL_4X2 -> R.layout.widget_custom_4x2
            WidgetSizeTier.PREMIUM_4X3 -> R.layout.widget_custom_4x3
            WidgetSizeTier.DASHBOARD_4X4 -> R.layout.widget_custom_4x4
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val palette = GlassSurfaceRenderer.resolvePalette(config, snapshot.isSystemDarkTheme)

        // 1. Render main Liquid Glass surface bitmap
        val glassBitmap = GlassSurfaceRenderer.renderGlassBitmap(
            widthDp = widthDp,
            heightDp = heightDp,
            density = density,
            config = config,
            isSystemDarkTheme = snapshot.isSystemDarkTheme,
            drawInternalDividersForTier = GlassSurfaceRenderer.DividerZoneLayout.NONE
        )
        views.setImageViewBitmap(R.id.widget_glass_bg, glassBitmap)

        // Default click opens MainActivity to edit this widget
        val defaultEditIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(MainActivity.EXTRA_OPEN_STUDIO_DIRECTLY, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val defaultPendingIntent = if (preview) null else PendingIntent.getActivity(
            context,
            appWidgetId + 30_000,
            defaultEditIntent,
            piFlags
        )
        if (!preview) views.setOnClickPendingIntent(R.id.widget_root, defaultPendingIntent)

        // 2. Bind each standardized slot for this tier
        val activeSlots = WidgetSlotBlueprints.slotsForTier(tier)
        val bounds = CustomSlotBounds.forTier(tier, widthDp, heightDp)
        val tzId = if (config.timezoneMode == TimezoneMode.VIETNAM_UTC7) "Asia/Ho_Chi_Minh" else null
        val format24 = if (config.showSeconds) "HH:mm:ss" else "HH:mm"
        val format12 = if (config.showSeconds) "hh:mm:ss a" else "hh:mm a"

        activeSlots.forEachIndexed { idx, slotDef ->
            val ids = CUSTOM_SLOT_VIEW_IDS.getOrNull(idx) ?: return@forEachIndexed
            val contentType = WidgetSlotBlueprints.slotContent(config, slotDef.slotId)
            val (slotW, slotH) = bounds[idx]

            if (contentType == SlotContentType.CLOCK) {
                views.setViewVisibility(ids.bitmapId, View.GONE)
                views.setViewVisibility(ids.clockId, View.VISIBLE)

                val pattern = if (config.use24HourFormat) format24 else format12
                val styledPattern = applyTypographySpans(pattern, config)
                views.setCharSequence(ids.clockId, "setFormat24Hour", styledPattern)
                views.setCharSequence(ids.clockId, "setFormat12Hour", styledPattern)
                views.setString(ids.clockId, "setTimeZone", tzId)
                views.setTextViewTextSize(ids.clockId, TypedValue.COMPLEX_UNIT_PX,
                    fittedClockPx(context, pattern, config, 42f * config.textScale * config.clockScale,
                        (slotW - 10).toFloat(), (slotH - 6).toFloat()))
                views.setTextColor(ids.clockId, palette.primaryTextArgb)
            } else if (contentType == SlotContentType.EMPTY && !showEmptySlots) {
                views.setViewVisibility(ids.clockId, View.GONE)
                views.setViewVisibility(ids.bitmapId, View.GONE)
            } else {
                views.setViewVisibility(ids.clockId, View.GONE)
                views.setViewVisibility(ids.bitmapId, View.VISIBLE)
                val bmp = CustomSlotRenderer.renderSlotBitmap(
                    widthDp = slotW,
                    heightDp = slotH,
                    density = density,
                    contentType = contentType,
                    config = config,
                    snapshot = snapshot,
                    isSystemDarkTheme = snapshot.isSystemDarkTheme,
                    slotHintVi = slotDef.shortHintVi,
                    showEmptyPlaceholder = showEmptySlots,
                    drawClockTextOnCanvas = false
                )
                views.setImageViewBitmap(ids.bitmapId, bmp)
            }

            if (preview) return@forEachIndexed
            // Contextual click target per module
            val slotPendingIntent = when (contentType) {
                SlotContentType.WEATHER -> createWeatherAppPendingIntent(context, appWidgetId)
                SlotContentType.BATTERY_WITH_TEMP -> PendingIntent.getActivity(
                    context, appWidgetId + 41_000 + idx,
                    Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    piFlags
                )
                SlotContentType.STORAGE -> PendingIntent.getActivity(
                    context, appWidgetId + 42_000 + idx,
                    Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    piFlags
                )
                SlotContentType.RAM -> PendingIntent.getActivity(
                    context, appWidgetId + 43_000 + idx,
                    Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    piFlags
                )
                else -> defaultPendingIntent
            }
            views.setOnClickPendingIntent(ids.containerId, slotPendingIntent)
        }

        return views
    }
}
