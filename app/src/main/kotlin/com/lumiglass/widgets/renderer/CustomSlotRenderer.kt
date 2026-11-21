package com.lumiglass.widgets.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.lumiglass.widgets.model.ClockFontStyle
import com.lumiglass.widgets.model.SlotContentType
import com.lumiglass.widgets.model.TemperatureSymbolStyle
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import kotlin.math.max
import kotlin.math.min

/**
 * Unified Procedural Canvas Renderer for Standardized Custom Widget Slots ("Cá nhân hóa").
 * Used identically by:
 * 1. Jetpack Compose Interactive Custom Widget Builder & Live Previews
 * 2. Real Android AppWidget RemoteViews on the Home Screen
 *
 * Automatically adapts typography, icon sizing, and horizontal/vertical layout
 * to the exact standardized dimensions of any widget cell (2x1, 2x2, 4x2, 4x3, 4x4).
 */
object CustomSlotRenderer {

    /**
     * Renders a single slot into a safely sized ARGB_8888 Bitmap for RemoteViews.
     */
    fun renderSlotBitmap(
        widthDp: Int,
        heightDp: Int,
        density: Float,
        contentType: SlotContentType,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        isSystemDarkTheme: Boolean,
        slotHintVi: String = "",
        showEmptyPlaceholder: Boolean = false,
        drawClockTextOnCanvas: Boolean = false
    ): Bitmap {
        val safeDensity = density.coerceIn(1.25f, 2.0f)
        val rawW = (widthDp * safeDensity).toInt().coerceAtLeast(64)
        val rawH = (heightDp * safeDensity).toInt().coerceAtLeast(48)

        val maxDim = max(rawW, rawH)
        val downscale = if (maxDim > 420) 420f / maxDim.toFloat() else 1f
        val pxW = (rawW * downscale).toInt().coerceAtLeast(48)
        val pxH = (rawH * downscale).toInt().coerceAtLeast(40)
        val effDensity = safeDensity * downscale

        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawSlotContent(
            canvas = canvas,
            w = pxW.toFloat(),
            h = pxH.toFloat(),
            density = effDensity,
            contentType = contentType,
            config = config,
            snapshot = snapshot,
            isSystemDarkTheme = isSystemDarkTheme,
            slotHintVi = slotHintVi,
            showEmptyPlaceholder = showEmptyPlaceholder,
            drawClockTextOnCanvas = drawClockTextOnCanvas
        )
        return bitmap
    }

    fun drawSlotContent(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        contentType: SlotContentType,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        isSystemDarkTheme: Boolean,
        slotHintVi: String = "",
        showEmptyPlaceholder: Boolean = false,
        drawClockTextOnCanvas: Boolean = true
    ) {
        if (w <= 4f || h <= 4f) return
        val palette = GlassSurfaceRenderer.resolvePalette(config, isSystemDarkTheme)

        when (contentType) {
            SlotContentType.EMPTY -> {
                if (showEmptyPlaceholder) {
                    drawEmptyPlusSlot(canvas, w, h, density, palette, slotHintVi)
                }
            }
            SlotContentType.CLOCK -> {
                drawClockSlot(canvas, w, h, density, config, snapshot, palette, drawClockTextOnCanvas)
            }
            SlotContentType.GREGORIAN_DATE -> {
                drawGregorianDateSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.LUNAR_DATE -> {
                drawLunarDateSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.DATE_AND_LUNAR -> {
                drawDateAndLunarSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.WEEK_CALENDAR -> {
                drawWeekCalendarSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.WEATHER -> {
                drawWeatherSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.BATTERY_WITH_TEMP -> {
                drawBatterySlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.STORAGE -> {
                drawStorageSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.RAM -> {
                drawRamSlot(canvas, w, h, density, config, snapshot, palette)
            }
            SlotContentType.TELEMETRY_TRIO -> {
                drawTelemetryTrioSlot(canvas, w, h, density, config, snapshot)
            }
        }
    }

    private fun resolveTypeface(fontStyle: ClockFontStyle, bold: Boolean): Typeface {
        val styleInt = if (bold) Typeface.BOLD else Typeface.NORMAL
        return runCatching {
            Typeface.create(fontStyle.androidFontFamily, styleInt)
        }.getOrElse {
            if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun drawSubGlassTile(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette,
        accentTint: Int? = null
    ) {
        val pad = (1.2f * density).coerceAtLeast(1.5f)
        val rect = RectF(pad, pad, w - pad, h - pad)
        val corner = (13f * density).coerceAtMost(min(w, h) * 0.34f)

        val baseColor = if (palette.isDarkGlass) {
            Color.argb(68, 15, 23, 42)
        } else {
            Color.argb(72, 255, 255, 255)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = baseColor
        }
        canvas.drawRoundRect(rect, corner, corner, fillPaint)

        if (accentTint != null) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    w * 0.5f, h * 0.35f, w * 0.5f, h - pad,
                    Color.TRANSPARENT,
                    ColorUtils.setAlphaComponent(accentTint, 46),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, corner, corner, glowPaint)
        }

        val rimColor = accentTint ?: palette.primaryTextArgb
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.0f * density).coerceIn(1.0f, 2.2f)
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                ColorUtils.setAlphaComponent(rimColor, if (accentTint != null) 145 else 90),
                ColorUtils.setAlphaComponent(rimColor, if (accentTint != null) 55 else 25),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, corner, corner, rimPaint)
    }

    private fun drawEmptyPlusSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette,
        slotHintVi: String
    ) {
        val pad = (1.8f * density).coerceAtLeast(2f)
        val rect = RectF(pad, pad, w - pad, h - pad)
        val corner = (13f * density).coerceAtMost(min(w, h) * 0.32f)

        // Subtle frosted fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 18)
        }
        canvas.drawRoundRect(rect, corner, corner, fillPaint)

        // Dashed luminous border
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.3f * density).coerceIn(1.2f, 2.6f)
            color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 125)
            pathEffect = DashPathEffect(floatArrayOf(6f * density, 4.5f * density), 0f)
        }
        canvas.drawRoundRect(rect, corner, corner, dashPaint)

        val cx = w * 0.5f
        val isHorizontalCompact = slotHintVi.isNotBlank() && h < 44f * density && w >= 85f * density
        val showVerticalHint = slotHintVi.isNotBlank() && h >= 44f * density

        val circleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(palette.accentTextArgb, 55)
        }
        val circleRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.1f * density).coerceAtLeast(1.2f)
            color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 185)
        }
        val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.9f * density).coerceIn(1.6f, 3.4f)
            strokeCap = Paint.Cap.ROUND
            color = palette.primaryTextArgb
        }

        if (isHorizontalCompact) {
            val circleRadius = (h * 0.28f).coerceIn(7.5f * density, 11f * density)
            val gap = 6f * density
            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 220)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                textSize = fitTextSize(
                    text = slotHintVi,
                    paint = this,
                    maxWidth = (w - circleRadius * 2f - gap - 14f * density).coerceAtLeast(24f),
                    preferredSize = (9.5f * density).coerceAtMost(h * 0.36f),
                    minSize = 6.8f * density
                )
            }
            val textW = hintPaint.measureText(slotHintVi)
            val totalW = circleRadius * 2f + gap + textW
            val startX = (w - totalW) * 0.5f
            val circleCx = startX + circleRadius
            val cy = h * 0.5f

            canvas.drawCircle(circleCx, cy, circleRadius, circleFill)
            canvas.drawCircle(circleCx, cy, circleRadius, circleRim)
            val plusArm = circleRadius * 0.46f
            canvas.drawLine(circleCx - plusArm, cy, circleCx + plusArm, cy, plusPaint)
            canvas.drawLine(circleCx, cy - plusArm, circleCx, cy + plusArm, plusPaint)

            val fm = hintPaint.fontMetrics
            val baseline = cy - (fm.ascent + fm.descent) * 0.5f
            canvas.drawText(slotHintVi, startX + circleRadius * 2f + gap, baseline, hintPaint)
            return
        }

        val circleRadius = (min(w, h) * 0.22f).coerceIn(9f * density, 16f * density)
        val circleCy = if (showVerticalHint) h * 0.40f else h * 0.50f

        canvas.drawCircle(cx, circleCy, circleRadius, circleFill)
        canvas.drawCircle(cx, circleCy, circleRadius, circleRim)

        val plusArm = circleRadius * 0.46f
        canvas.drawLine(cx - plusArm, circleCy, cx + plusArm, circleCy, plusPaint)
        canvas.drawLine(cx, circleCy - plusArm, cx, circleCy + plusArm, plusPaint)

        if (showVerticalHint) {
            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 215)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                textSize = fitTextSize(
                    text = slotHintVi,
                    paint = this,
                    maxWidth = w * 0.86f,
                    preferredSize = (9.5f * density).coerceAtMost(h * 0.19f),
                    minSize = 6.5f * density
                )
            }
            val textY = (circleCy + circleRadius + hintPaint.textSize * 1.15f)
                .coerceAtMost(h - 5f * density)
            canvas.drawText(slotHintVi, cx, textY, hintPaint)
        }
    }

    private fun drawClockSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette,
        drawClockTextOnCanvas: Boolean
    ) {
        if (!drawClockTextOnCanvas) return

        val timeText = snapshot.previewTimePrimary.ifBlank { "08:35" }
        val cx = w * 0.5f
        val cy = h * 0.5f

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = palette.primaryTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = true)
            val targetSize = min(h * 0.64f, w * 0.36f) * config.textScale * config.clockScale
            textSize = fitTextSize(
                text = timeText,
                paint = this,
                maxWidth = w * 0.90f,
                preferredSize = targetSize.coerceIn(14f * density, 54f * density),
                minSize = 12f * density
            )
            if (config.textGlowEnabled) {
                setShadowLayer(
                    7f * density,
                    0f,
                    1.5f * density,
                    ColorUtils.setAlphaComponent(palette.primaryTextArgb, 110)
                )
            } else if (config.textShadowEnabled) {
                setShadowLayer(4f * density, 0f, 2f * density, palette.shadowTextArgb)
            }
        }
        val fm = clockPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) * 0.5f
        canvas.drawText(timeText, cx, baseline, clockPaint)
    }

    private fun drawGregorianDateSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        drawSubGlassTile(canvas, w, h, density, palette)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val dateText = snapshot.gregorianDateText

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = palette.primaryTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = true)
            textSize = fitTextSize(
                text = dateText,
                paint = this,
                maxWidth = w * 0.88f,
                preferredSize = min(h * 0.40f, 13.5f * density) * config.textScale,
                minSize = 8f * density
            )
            if (config.textShadowEnabled) {
                setShadowLayer(3f * density, 0f, 1f * density, palette.shadowTextArgb)
            }
        }
        val fm = paint.fontMetrics
        canvas.drawText(dateText, cx, cy - (fm.ascent + fm.descent) * 0.5f, paint)
    }

    private fun drawLunarDateSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        drawSubGlassTile(canvas, w, h, density, palette, accentTint = palette.accentTextArgb)
        val cx = w * 0.5f
        val lunar = snapshot.lunarDateInfo
        val line1 = "☾ Âm lịch: ${lunar.formatLunarShort()}"
        val line2Parts = buildList {
            if (config.showCanChi) add(lunar.yearCanChi)
            if (config.showLunarHolidays && !lunar.specialPhaseOrHolidayVi.isNullOrBlank()) {
                add(lunar.specialPhaseOrHolidayVi)
            }
        }
        val line2 = line2Parts.joinToString(" • ")

        val showTwoLines = line2.isNotBlank() && h >= 42f * density
        val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = palette.accentTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = true)
            textSize = fitTextSize(
                text = line1,
                paint = this,
                maxWidth = w * 0.88f,
                preferredSize = min(h * (if (showTwoLines) 0.28f else 0.38f), 12.5f * density) * config.textScale,
                minSize = 7.5f * density
            )
            if (config.textShadowEnabled) {
                setShadowLayer(3f * density, 0f, 1f * density, palette.shadowTextArgb)
            }
        }

        if (showTwoLines) {
            val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = palette.secondaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = false)
                textSize = fitTextSize(
                    text = line2,
                    paint = this,
                    maxWidth = w * 0.88f,
                    preferredSize = min(h * 0.22f, 10f * density) * config.textScale,
                    minSize = 6.8f * density
                )
            }
            canvas.drawText(line1, cx, h * 0.44f, p1)
            canvas.drawText(line2, cx, h * 0.72f, p2)
        } else {
            val fm = p1.fontMetrics
            canvas.drawText(line1, cx, h * 0.5f - (fm.ascent + fm.descent) * 0.5f, p1)
        }
    }

    private fun drawDateAndLunarSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        drawSubGlassTile(canvas, w, h, density, palette)
        val cx = w * 0.5f
        val solar = snapshot.gregorianDateText
        val lunar = "☾ " + snapshot.lunarDateInfo.formatDisplay(
            showCanChi = config.showCanChi,
            showHolidays = config.showLunarHolidays
        )

        val pSolar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = palette.primaryTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = true)
            textSize = fitTextSize(
                text = solar,
                paint = this,
                maxWidth = w * 0.88f,
                preferredSize = min(h * 0.30f, 13f * density) * config.textScale,
                minSize = 7.5f * density
            )
            if (config.textShadowEnabled) {
                setShadowLayer(3f * density, 0f, 1f * density, palette.shadowTextArgb)
            }
        }
        val pLunar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = palette.accentTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = false)
            textSize = fitTextSize(
                text = lunar,
                paint = this,
                maxWidth = w * 0.88f,
                preferredSize = min(h * 0.23f, 10.5f * density) * config.textScale,
                minSize = 6.8f * density
            )
        }
        canvas.drawText(solar, cx, h * 0.43f, pSolar)
        canvas.drawText(lunar, cx, h * 0.74f, pLunar)
    }

    private fun drawWeekCalendarSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        val allCols = snapshot.weekCalendarColumns
        if (allCols.size < 7) return

        val useSeven = config.weekCalendarDayCount >= 7 && w >= 190f * density
        val cols = if (useSeven) allCols else allCols.subList(1, 6)
        val colCount = cols.size
        val colW = w / colCount.toFloat()

        // Top subtle hairline divider
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (0.8f * density).coerceAtLeast(1f)
            color = ColorUtils.setAlphaComponent(palette.primaryTextArgb, 48)
        }
        canvas.drawLine(w * 0.04f, 1.5f * density, w * 0.96f, 1.5f * density, linePaint)

        cols.forEachIndexed { idx, col ->
            val left = idx * colW
            val cx = left + colW * 0.5f
            val padX = (2f * density).coerceAtMost(colW * 0.08f)
            val topY = 4f * density
            val botY = h - 3f * density

            if (col.isToday) {
                val pillRect = RectF(left + padX, topY, left + colW - padX, botY)
                val corner = (10f * density).coerceAtMost(min(pillRect.width(), pillRect.height()) * 0.34f)
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        pillRect.left, pillRect.top, pillRect.left, pillRect.bottom,
                        ColorUtils.setAlphaComponent(palette.primaryTextArgb, 56),
                        ColorUtils.setAlphaComponent(palette.accentTextArgb, 42),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(pillRect, corner, corner, bgPaint)

                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (1.1f * density).coerceAtLeast(1.2f)
                    color = ColorUtils.setAlphaComponent(palette.accentTextArgb, 185)
                }
                canvas.drawRoundRect(pillRect, corner, corner, rimPaint)
            }

            val weekdayStr = if (col.isToday && !col.weekdayShortVi.contains("NAY", ignoreCase = true)) {
                "${col.weekdayShortVi}•NAY"
            } else {
                col.weekdayShortVi
            }
            val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = if (col.isToday) palette.primaryTextArgb else palette.secondaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = col.isToday)
                textSize = fitTextSize(
                    text = weekdayStr,
                    paint = this,
                    maxWidth = colW * 0.86f,
                    preferredSize = min(h * 0.20f, 9.2f * density),
                    minSize = 6.2f * density
                )
            }
            val solarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = palette.primaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = min(h * 0.30f, 14f * density).coerceAtLeast(9f * density)
            }
            val lunarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = if (col.isToday) palette.accentTextArgb else ColorUtils.setAlphaComponent(palette.secondaryTextArgb, 210)
                typeface = resolveTypeface(config.fontStyle, bold = col.isToday)
                textSize = fitTextSize(
                    text = col.lunarDayMonthString,
                    paint = this,
                    maxWidth = colW * 0.88f,
                    preferredSize = min(h * 0.18f, 8.2f * density),
                    minSize = 5.8f * density
                )
            }

            val boxH = botY - topY
            canvas.drawText(weekdayStr, cx, topY + boxH * 0.28f, weekdayPaint)
            canvas.drawText(col.solarDayString, cx, topY + boxH * 0.62f, solarPaint)
            canvas.drawText(col.lunarDayMonthString, cx, topY + boxH * 0.88f, lunarPaint)
        }
    }

    private fun drawWeatherSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        drawSubGlassTile(canvas, w, h, density, palette, accentTint = 0xFF38BDF8.toInt())
        val weather = snapshot.weatherInfo
        val degreeOnly = config.temperatureSymbolStyle == TemperatureSymbolStyle.DEGREE_ONLY
        val tempStr = weather.formatTemp(weather.currentTempC, config.temperatureUnit, degreeOnly)
        val condStr = if (weather.hasData) weather.conditionTextVi else weather.cityNameVi
        val detailStr = if (weather.hasData) {
            weather.formatRangeAndPrecip(config.temperatureUnit, degreeOnly)
        } else {
            weather.errorMessageVi ?: "Chạm để cập nhật"
        }

        val isWide = w >= h * 1.45f
        if (isWide) {
            val iconSize = (min(h * 0.64f, w * 0.24f)).coerceIn(18f * density, 38f * density)
            val iconX = (w * 0.06f).coerceAtLeast(6f * density)
            val iconY = (h - iconSize) * 0.5f

            canvas.save()
            canvas.translate(iconX, iconY)
            GlassIconRenderer.drawWeatherIcon(
                canvas = canvas,
                sizePx = iconSize,
                category = weather.conditionCategory(),
                style = config.iconStyle,
                primaryTextArgb = palette.primaryTextArgb
            )
            canvas.restore()

            val textLeft = iconX + iconSize + 7f * density
            val availW = (w - textLeft - 7f * density).coerceAtLeast(24f)
            val headline = "$condStr  $tempStr"

            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                color = palette.primaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = fitTextSize(
                    text = headline,
                    paint = this,
                    maxWidth = availW,
                    preferredSize = min(h * 0.34f, 14f * density) * config.textScale,
                    minSize = 8.2f * density
                )
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                color = palette.secondaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = false)
                textSize = fitTextSize(
                    text = detailStr,
                    paint = this,
                    maxWidth = availW,
                    preferredSize = min(h * 0.24f, 10.5f * density) * config.textScale,
                    minSize = 6.8f * density
                )
            }

            canvas.drawText(headline, textLeft, h * 0.45f, headPaint)
            canvas.drawText(detailStr, textLeft, h * 0.76f, subPaint)
        } else {
            // Compact vertical weather card
            val cx = w * 0.5f
            val iconSize = (min(w, h) * 0.34f).coerceIn(16f * density, 32f * density)
            canvas.save()
            canvas.translate(cx - iconSize * 0.5f, h * 0.08f)
            GlassIconRenderer.drawWeatherIcon(
                canvas = canvas,
                sizePx = iconSize,
                category = weather.conditionCategory(),
                style = config.iconStyle,
                primaryTextArgb = palette.primaryTextArgb
            )
            canvas.restore()

            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = palette.primaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = min(h * 0.24f, 15f * density).coerceAtLeast(10f * density)
            }
            val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = palette.secondaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = false)
                textSize = fitTextSize(
                    text = condStr,
                    paint = this,
                    maxWidth = w * 0.88f,
                    preferredSize = min(h * 0.16f, 9.5f * density),
                    minSize = 6.5f * density
                )
            }
            canvas.drawText(tempStr, cx, h * 0.66f, tempPaint)
            canvas.drawText(condStr, cx, h * 0.86f, condPaint)
        }
    }

    private fun drawBatterySlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        val battery = snapshot.batteryInfo
        if (w <= h * 1.65f) {
            // Vertical / Square neon glass Battery + Temp card
            val u = min(w / 68f, h / 78f).coerceAtLeast(0.5f)
            GlassIconRenderer.drawBatteryTrioCard(
                canvas = canvas,
                w = w,
                h = h,
                battery = battery,
                config = config,
                density = u
            )
        } else {
            // Wide horizontal Neon Battery + Temp banner
            val accentColor = GlassIconRenderer.resolveBatteryLevelColorArgb(
                levelPercent = battery.levelPercent,
                isCharging = battery.isCharging,
                colorByLevel = config.dynamicAlertColors,
                fallbackArgb = 0xFF22C55E.toInt()
            )
            val tempColor = GlassIconRenderer.resolveBatteryTempColorArgb(
                tempCelsius = battery.temperatureCelsius,
                warnByColor = config.dynamicAlertColors,
                fallbackArgb = 0xFF38BDF8.toInt()
            )
            drawSubGlassTile(canvas, w, h, density, palette, accentTint = accentColor)

            val pct = battery.levelPercent.coerceIn(0, 100)
            val frac = if (battery.hasLevelData) pct / 100f else 0.5f

            // Left circular arc with %
            val r = (h * 0.34f).coerceIn(12f * density, 24f * density)
            val arcCx = r + 10f * density
            val arcCy = h * 0.5f
            val arcRect = RectF(arcCx - r, arcCy - r, arcCx + r, arcCy + r)

            val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.6f * density
                strokeCap = Paint.Cap.ROUND
                color = ColorUtils.setAlphaComponent(accentColor, 45)
            }
            val progP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.6f * density
                strokeCap = Paint.Cap.ROUND
                color = accentColor
            }
            canvas.drawArc(arcRect, 135f, 270f, false, trackP)
            canvas.drawArc(arcRect, 135f, 270f * frac, false, progP)

            val pctP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = palette.primaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = (r * 0.62f).coerceAtLeast(8f * density)
            }
            val fm = pctP.fontMetrics
            canvas.drawText("$pct%", arcCx, arcCy - (fm.ascent + fm.descent) * 0.5f, pctP)

            // Middle label + status
            val midLeft = arcCx + r + 10f * density
            val statusStr = if (battery.isCharging) "Pin • Đang sạc ⚡" else "Pin thiết bị"
            val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                color = palette.primaryTextArgb
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = min(h * 0.28f, 12f * density)
            }
            val tempStr = "🌡 " + battery.formatTemperature(config.temperatureUnit)
            val tempP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                color = tempColor
                typeface = resolveTypeface(config.fontStyle, bold = true)
                textSize = min(h * 0.25f, 11f * density)
            }
            canvas.drawText(statusStr, midLeft, h * 0.44f, titleP)
            canvas.drawText(tempStr, midLeft, h * 0.74f, tempP)
        }
    }

    private fun drawStorageSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        val storage = snapshot.storageInfo
        if (w <= h * 1.65f) {
            val u = min(w / 68f, h / 78f).coerceAtLeast(0.5f)
            GlassIconRenderer.drawStorageTrioCard(
                canvas = canvas,
                w = w,
                h = h,
                storage = storage,
                config = config,
                density = u
            )
        } else {
            val accentColor = GlassIconRenderer.resolveStorageColorArgb(
                usedPercent = storage.usedPercent,
                warnByColor = config.dynamicAlertColors
            )
            drawHorizontalMetricBar(
                canvas = canvas,
                w = w,
                h = h,
                density = density,
                config = config,
                palette = palette,
                accentColor = accentColor,
                title = "Bộ nhớ trong",
                percent = storage.usedPercent,
                detail = storage.formatUsedAndTotalGb()
            )
        }
    }

    private fun drawRamSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette
    ) {
        val ram = snapshot.ramInfo
        if (w <= h * 1.65f) {
            val u = min(w / 68f, h / 78f).coerceAtLeast(0.5f)
            GlassIconRenderer.drawRamTrioCard(
                canvas = canvas,
                w = w,
                h = h,
                ram = ram,
                config = config,
                density = u
            )
        } else {
            val accentColor = GlassIconRenderer.resolveRamColorArgb(
                usedPercent = ram.usedPercent,
                warnByColor = config.dynamicAlertColors
            )
            drawHorizontalMetricBar(
                canvas = canvas,
                w = w,
                h = h,
                density = density,
                config = config,
                palette = palette,
                accentColor = accentColor,
                title = "Bộ nhớ RAM",
                percent = ram.usedPercent,
                detail = "${ram.formatUsedGb()} / ${ram.formatTotalGb()}G"
            )
        }
    }

    private fun drawHorizontalMetricBar(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        palette: GlassSurfaceRenderer.EffectiveGlassPalette,
        accentColor: Int,
        title: String,
        percent: Int,
        detail: String
    ) {
        drawSubGlassTile(canvas, w, h, density, palette, accentTint = accentColor)
        val padX = 10f * density
        val pct = percent.coerceIn(0, 100)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.LEFT
            color = palette.primaryTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = true)
            textSize = min(h * 0.26f, 11.5f * density)
        }
        val pctP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            color = accentColor
            typeface = resolveTypeface(config.fontStyle, bold = true)
            textSize = min(h * 0.30f, 13.5f * density)
        }
        val detailP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            color = palette.secondaryTextArgb
            typeface = resolveTypeface(config.fontStyle, bold = false)
            textSize = min(h * 0.20f, 9f * density)
        }

        canvas.drawText(title, padX, h * 0.40f, titleP)
        canvas.drawText("$pct%", w - padX, h * 0.42f, pctP)

        val barH = (4.5f * density).coerceAtMost(h * 0.14f)
        val barTop = h * 0.62f
        val barRect = RectF(padX, barTop, w - padX, barTop + barH)
        val corner = barH * 0.5f

        val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(accentColor, 48)
        }
        canvas.drawRoundRect(barRect, corner, corner, trackP)

        val fillW = (barRect.width() * (pct / 100f)).coerceAtLeast(barH)
        val fillRect = RectF(barRect.left, barRect.top, (barRect.left + fillW).coerceAtMost(barRect.right), barRect.bottom)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                fillRect.left, fillRect.centerY(), fillRect.right, fillRect.centerY(),
                ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                accentColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(fillRect, corner, corner, fillP)

        if (h >= 44f * density) {
            canvas.drawText(detail, w - padX, h * 0.90f, detailP)
        }
    }

    private fun drawTelemetryTrioSlot(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        config: WidgetConfig,
        snapshot: WidgetDataSnapshot
    ) {
        val gap = (3.5f * density).coerceAtMost(w * 0.03f)
        val cardW = ((w - gap * 2f) / 3f).coerceAtLeast(16f)
        val u = min(cardW / 68f, h / 78f).coerceAtLeast(0.45f)

        // Card 1: Battery + Temp
        canvas.save()
        GlassIconRenderer.drawBatteryTrioCard(
            canvas = canvas,
            w = cardW,
            h = h,
            battery = snapshot.batteryInfo,
            config = config,
            density = u
        )
        canvas.restore()

        // Card 2: Storage
        canvas.save()
        canvas.translate(cardW + gap, 0f)
        GlassIconRenderer.drawStorageTrioCard(
            canvas = canvas,
            w = cardW,
            h = h,
            storage = snapshot.storageInfo,
            config = config,
            density = u
        )
        canvas.restore()

        // Card 3: RAM
        canvas.save()
        canvas.translate((cardW + gap) * 2f, 0f)
        GlassIconRenderer.drawRamTrioCard(
            canvas = canvas,
            w = cardW,
            h = h,
            ram = snapshot.ramInfo,
            config = config,
            density = u
        )
        canvas.restore()
    }

    private fun fitTextSize(
        text: String,
        paint: Paint,
        maxWidth: Float,
        preferredSize: Float,
        minSize: Float
    ): Float {
        if (text.isBlank() || maxWidth <= 8f) return preferredSize
        var size = preferredSize.coerceAtLeast(minSize)
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > minSize) {
            size -= 1f
            paint.textSize = size
        }
        return size
    }
}
