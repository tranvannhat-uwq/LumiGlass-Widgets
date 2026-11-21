package com.lumiglass.widgets.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.lumiglass.widgets.data.ram.RamInfo
import com.lumiglass.widgets.data.storage.StorageInfo
import com.lumiglass.widgets.model.BatteryDisplayStyle
import com.lumiglass.widgets.model.BatteryInfo
import com.lumiglass.widgets.model.GlassIconStyle
import com.lumiglass.widgets.model.TemperatureUnit
import com.lumiglass.widgets.model.WeatherConditionCategory
import com.lumiglass.widgets.model.WidgetConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Liquid Glass Icon & Telemetry Gauge Renderer.
 * Produces crisp, multi-layered weather icons and battery gauges (Pill, Circle, Progress Bar, Icon)
 * in GLASS, OUTLINE, or FILLED styles for both RemoteViews and Compose preview.
 */
object GlassIconRenderer {

    fun renderWeatherIconBitmap(
        category: WeatherConditionCategory,
        style: GlassIconStyle,
        primaryTextArgb: Int,
        sizeDp: Int,
        density: Float
    ): Bitmap {
        val px = (sizeDp * density.coerceIn(1.5f, 2.5f)).toInt().coerceIn(36, 160)
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawWeatherIcon(canvas, px.toFloat(), category, style, primaryTextArgb)
        return bitmap
    }

    fun drawWeatherIcon(
        canvas: Canvas,
        sizePx: Float,
        category: WeatherConditionCategory,
        style: GlassIconStyle,
        primaryTextArgb: Int
    ) {
        val cx = sizePx * 0.5f
        val cy = sizePx * 0.5f
        val strokeW = (sizePx * 0.068f).coerceAtLeast(1.8f)

        val sunGold = Color.parseColor("#FBBF24")
        val sunCore = Color.parseColor("#FEF08A")
        val skyBlue = Color.parseColor("#38BDF8")
        val rainCyan = Color.parseColor("#7DD3FC")

        when (category) {
            WeatherConditionCategory.CLEAR_DAY -> {
                drawSun(canvas, cx, cy, sizePx * 0.25f, style, primaryTextArgb, sunCore, sunGold, strokeW)
            }
            WeatherConditionCategory.CLEAR_NIGHT -> {
                drawCrescentMoon(canvas, cx, cy, sizePx * 0.27f, style, primaryTextArgb, strokeW)
            }
            WeatherConditionCategory.PARTLY_CLOUDY -> {
                drawSun(
                    canvas,
                    cx - sizePx * 0.13f,
                    cy - sizePx * 0.12f,
                    sizePx * 0.17f,
                    style,
                    primaryTextArgb,
                    sunCore,
                    sunGold,
                    strokeW * 0.85f
                )
                drawGlassCloud(
                    canvas,
                    cx + sizePx * 0.05f,
                    cy + sizePx * 0.08f,
                    sizePx * 0.64f,
                    style,
                    primaryTextArgb,
                    strokeW
                )
            }
            WeatherConditionCategory.CLOUDY, WeatherConditionCategory.UNKNOWN -> {
                drawGlassCloud(
                    canvas,
                    cx - sizePx * 0.08f,
                    cy - sizePx * 0.08f,
                    sizePx * 0.52f,
                    style,
                    ColorUtils.setAlphaComponent(primaryTextArgb, 145),
                    strokeW * 0.85f
                )
                drawGlassCloud(
                    canvas,
                    cx + sizePx * 0.05f,
                    cy + sizePx * 0.07f,
                    sizePx * 0.66f,
                    style,
                    primaryTextArgb,
                    strokeW
                )
            }
            WeatherConditionCategory.FOG -> {
                drawGlassCloud(
                    canvas,
                    cx,
                    cy - sizePx * 0.08f,
                    sizePx * 0.62f,
                    style,
                    primaryTextArgb,
                    strokeW
                )
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 0.85f
                    strokeCap = Paint.Cap.ROUND
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 195)
                }
                canvas.drawLine(sizePx * 0.22f, sizePx * 0.72f, sizePx * 0.78f, sizePx * 0.72f, linePaint)
                canvas.drawLine(sizePx * 0.28f, sizePx * 0.83f, sizePx * 0.72f, sizePx * 0.83f, linePaint)
            }
            WeatherConditionCategory.DRIZZLE, WeatherConditionCategory.RAIN -> {
                drawGlassCloud(
                    canvas,
                    cx,
                    cy - sizePx * 0.09f,
                    sizePx * 0.64f,
                    style,
                    primaryTextArgb,
                    strokeW
                )
                val dropColor = if (style == GlassIconStyle.OUTLINE) primaryTextArgb else rainCyan
                val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 0.85f
                    strokeCap = Paint.Cap.ROUND
                    color = dropColor
                }
                val xs = floatArrayOf(sizePx * 0.34f, sizePx * 0.50f, sizePx * 0.66f)
                for (x in xs) {
                    canvas.drawLine(x, sizePx * 0.68f, x - sizePx * 0.06f, sizePx * 0.84f, dropPaint)
                }
            }
            WeatherConditionCategory.THUNDERSTORM -> {
                drawGlassCloud(
                    canvas,
                    cx,
                    cy - sizePx * 0.10f,
                    sizePx * 0.64f,
                    style,
                    primaryTextArgb,
                    strokeW
                )
                val boltPath = Path().apply {
                    moveTo(cx + sizePx * 0.03f, sizePx * 0.56f)
                    lineTo(cx - sizePx * 0.09f, sizePx * 0.74f)
                    lineTo(cx + sizePx * 0.01f, sizePx * 0.74f)
                    lineTo(cx - sizePx * 0.04f, sizePx * 0.90f)
                    lineTo(cx + sizePx * 0.12f, sizePx * 0.69f)
                    lineTo(cx + sizePx * 0.01f, sizePx * 0.69f)
                    close()
                }
                val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = if (style == GlassIconStyle.OUTLINE) Paint.Style.STROKE else Paint.Style.FILL
                    strokeWidth = strokeW * 0.7f
                    color = if (style == GlassIconStyle.OUTLINE) primaryTextArgb else sunGold
                }
                canvas.drawPath(boltPath, boltPaint)

                val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 0.75f
                    strokeCap = Paint.Cap.ROUND
                    color = skyBlue
                }
                canvas.drawLine(sizePx * 0.30f, sizePx * 0.68f, sizePx * 0.25f, sizePx * 0.81f, dropPaint)
                canvas.drawLine(sizePx * 0.72f, sizePx * 0.68f, sizePx * 0.67f, sizePx * 0.81f, dropPaint)
            }
        }
    }

    private fun drawSun(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        style: GlassIconStyle,
        primaryTextArgb: Int,
        coreColor: Int,
        edgeColor: Int,
        strokeW: Float
    ) {
        if (style == GlassIconStyle.GLASS) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                shader = RadialGradient(
                    cx, cy, radius * 1.65f,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(coreColor, 235),
                        ColorUtils.setAlphaComponent(edgeColor, 180),
                        ColorUtils.setAlphaComponent(edgeColor, 0)
                    ),
                    floatArrayOf(0f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius * 1.65f, glowPaint)
        }

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = if (style == GlassIconStyle.OUTLINE) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = strokeW
            if (style == GlassIconStyle.OUTLINE) {
                color = primaryTextArgb
            } else if (style == GlassIconStyle.FILLED) {
                color = edgeColor
            } else {
                shader = LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    coreColor, edgeColor, Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawCircle(cx, cy, radius, corePaint)

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = strokeW * 0.82f
            strokeCap = Paint.Cap.ROUND
            color = if (style == GlassIconStyle.OUTLINE) primaryTextArgb else ColorUtils.blendARGB(edgeColor, Color.WHITE, 0.35f)
        }
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val r1 = radius * 1.32f
            val r2 = radius * 1.65f
            canvas.drawLine(
                cx + (cos(angle) * r1).toFloat(),
                cy + (sin(angle) * r1).toFloat(),
                cx + (cos(angle) * r2).toFloat(),
                cy + (sin(angle) * r2).toFloat(),
                rayPaint
            )
        }
    }

    private fun drawCrescentMoon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        style: GlassIconStyle,
        primaryTextArgb: Int,
        strokeW: Float
    ) {
        val outer = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        val inner = Path().apply {
            addCircle(cx + radius * 0.38f, cy - radius * 0.24f, radius * 0.82f, Path.Direction.CW)
        }
        val crescent = Path().apply {
            op(outer, inner, Path.Op.DIFFERENCE)
        }
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = if (style == GlassIconStyle.OUTLINE) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = strokeW
            color = if (style == GlassIconStyle.OUTLINE) primaryTextArgb else Color.parseColor("#E0F2FE")
        }
        canvas.drawPath(crescent, moonPaint)
    }

    private fun drawGlassCloud(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        style: GlassIconStyle,
        primaryTextArgb: Int,
        strokeW: Float
    ) {
        val h = width * 0.56f
        val left = cx - width * 0.5f
        val top = cy - h * 0.5f
        val bottom = cy + h * 0.45f

        val pillPath = Path().apply {
            val baseRect = RectF(left + width * 0.08f, cy - h * 0.08f, left + width * 0.92f, bottom)
            addRoundRect(baseRect, h * 0.28f, h * 0.28f, Path.Direction.CW)
        }
        val lobeLeft = Path().apply {
            addCircle(left + width * 0.34f, cy - h * 0.04f, width * 0.19f, Path.Direction.CW)
        }
        val lobeMain = Path().apply {
            addCircle(left + width * 0.56f, top + h * 0.34f, width * 0.24f, Path.Direction.CW)
        }
        pillPath.op(lobeLeft, Path.Op.UNION)
        pillPath.op(lobeMain, Path.Op.UNION)

        when (style) {
            GlassIconStyle.GLASS -> {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    shader = LinearGradient(
                        left, top, left, bottom,
                        ColorUtils.setAlphaComponent(Color.WHITE, 225),
                        ColorUtils.setAlphaComponent(primaryTextArgb, 130),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(pillPath, fillPaint)

                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 0.72f
                    color = ColorUtils.setAlphaComponent(Color.WHITE, 230)
                }
                canvas.drawPath(pillPath, rimPaint)
            }
            GlassIconStyle.FILLED -> {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = primaryTextArgb
                }
                canvas.drawPath(pillPath, fillPaint)
            }
            GlassIconStyle.OUTLINE -> {
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeJoin = Paint.Join.ROUND
                    color = primaryTextArgb
                }
                canvas.drawPath(pillPath, outlinePaint)
            }
        }
    }

    /**
     * Renders a battery visual gauge (Pill, Circle, Progress Bar, or Icon) into a Bitmap.
     */
    fun renderBatteryGaugeBitmap(
        batteryInfo: BatteryInfo,
        style: BatteryDisplayStyle,
        colorByLevel: Boolean,
        primaryTextArgb: Int,
        density: Float
    ): Bitmap {
        val safeDensity = density.coerceIn(1.5f, 2.5f)
        val (wDp, hDp) = when (style) {
            BatteryDisplayStyle.PILL -> 44 to 20
            BatteryDisplayStyle.CIRCLE -> 28 to 28
            BatteryDisplayStyle.PROGRESS_BAR -> 64 to 14
            BatteryDisplayStyle.ICON_NUMBER -> 20 to 22
        }
        val pxW = (wDp * safeDensity).toInt().coerceAtLeast(24)
        val pxH = (hDp * safeDensity).toInt().coerceAtLeast(18)

        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBatteryGauge(
            canvas = canvas,
            widthPx = pxW.toFloat(),
            heightPx = pxH.toFloat(),
            batteryInfo = batteryInfo,
            style = style,
            colorByLevel = colorByLevel,
            primaryTextArgb = primaryTextArgb
        )
        return bitmap
    }

    fun drawBatteryGauge(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        batteryInfo: BatteryInfo,
        style: BatteryDisplayStyle,
        colorByLevel: Boolean,
        primaryTextArgb: Int
    ) {
        val levelFraction = if (batteryInfo.hasLevelData) {
            (batteryInfo.levelPercent.coerceIn(0, 100)) / 100f
        } else {
            0.5f
        }
        val accentColor = resolveBatteryLevelColorArgb(
            levelPercent = batteryInfo.levelPercent,
            isCharging = batteryInfo.isCharging,
            colorByLevel = colorByLevel,
            fallbackArgb = primaryTextArgb
        )
        val strokeW = (heightPx * 0.095f).coerceIn(1.5f, 3.2f)

        when (style) {
            BatteryDisplayStyle.PILL -> {
                val capW = widthPx * 0.07f
                val bodyRect = RectF(
                    strokeW,
                    strokeW,
                    widthPx - capW - strokeW,
                    heightPx - strokeW
                )
                val corner = bodyRect.height() * 0.48f

                // Glass capsule background
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 38)
                }
                canvas.drawRoundRect(bodyRect, corner, corner, bgPaint)

                // Inner liquid fill
                val inset = strokeW * 1.25f
                val maxFillW = (bodyRect.width() - inset * 2).coerceAtLeast(2f)
                val fillW = (maxFillW * levelFraction).coerceAtLeast(bodyRect.height() * 0.35f)
                val fillRect = RectF(
                    bodyRect.left + inset,
                    bodyRect.top + inset,
                    (bodyRect.left + inset + fillW).coerceAtMost(bodyRect.right - inset),
                    bodyRect.bottom - inset
                )
                val fillCorner = fillRect.height() * 0.48f
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    shader = LinearGradient(
                        fillRect.left, fillRect.top, fillRect.right, fillRect.bottom,
                        ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                        accentColor,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(fillRect, fillCorner, fillCorner, fillPaint)

                // Outer capsule border
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 195)
                }
                canvas.drawRoundRect(bodyRect, corner, corner, borderPaint)

                // Terminal tip
                val tipRect = RectF(
                    bodyRect.right + strokeW * 0.6f,
                    heightPx * 0.33f,
                    widthPx - strokeW * 0.5f,
                    heightPx * 0.67f
                )
                val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 175)
                }
                canvas.drawRoundRect(tipRect, strokeW, strokeW, tipPaint)
            }

            BatteryDisplayStyle.CIRCLE -> {
                val size = kotlin.math.min(widthPx, heightPx)
                val pad = strokeW * 1.4f
                val arcRect = RectF(
                    (widthPx - size) * 0.5f + pad,
                    (heightPx - size) * 0.5f + pad,
                    (widthPx + size) * 0.5f - pad,
                    (heightPx + size) * 0.5f - pad
                )
                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 1.25f
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 52)
                }
                canvas.drawOval(arcRect, trackPaint)

                val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW * 1.25f
                    strokeCap = Paint.Cap.ROUND
                    color = accentColor
                }
                canvas.drawArc(arcRect, -90f, 360f * levelFraction, false, progressPaint)

                // Center dot or charging indicator
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = if (batteryInfo.isCharging) Color.parseColor("#FBBF24") else accentColor
                }
                canvas.drawCircle(widthPx * 0.5f, heightPx * 0.5f, size * 0.13f, dotPaint)
            }

            BatteryDisplayStyle.PROGRESS_BAR -> {
                val barH = (heightPx * 0.46f).coerceAtLeast(4f)
                val top = (heightPx - barH) * 0.5f
                val trackRect = RectF(strokeW, top, widthPx - strokeW, top + barH)
                val corner = barH * 0.5f

                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = ColorUtils.setAlphaComponent(primaryTextArgb, 45)
                }
                canvas.drawRoundRect(trackRect, corner, corner, trackPaint)

                val fillW = (trackRect.width() * levelFraction).coerceAtLeast(barH)
                val fillRect = RectF(trackRect.left, trackRect.top, trackRect.left + fillW, trackRect.bottom)
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    shader = LinearGradient(
                        fillRect.left, fillRect.top, fillRect.right, fillRect.bottom,
                        ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                        accentColor,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(fillRect, corner, corner, fillPaint)
            }

            BatteryDisplayStyle.ICON_NUMBER -> {
                val bodyW = widthPx * 0.62f
                val left = (widthPx - bodyW) * 0.5f
                val top = heightPx * 0.16f
                val bot = heightPx * 0.92f
                val bodyRect = RectF(left, top, left + bodyW, bot)
                val corner = bodyW * 0.22f

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    color = accentColor
                }
                canvas.drawRoundRect(bodyRect, corner, corner, borderPaint)

                val tipRect = RectF(
                    left + bodyW * 0.28f,
                    heightPx * 0.05f,
                    left + bodyW * 0.72f,
                    top
                )
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = accentColor
                }
                canvas.drawRoundRect(tipRect, strokeW * 0.5f, strokeW * 0.5f, fillPaint)

                val inset = strokeW * 1.1f
                val innerH = (bodyRect.height() - inset * 2) * levelFraction
                val innerRect = RectF(
                    bodyRect.left + inset,
                    (bodyRect.bottom - inset - innerH).coerceAtLeast(bodyRect.top + inset),
                    bodyRect.right - inset,
                    bodyRect.bottom - inset
                )
                canvas.drawRoundRect(innerRect, corner * 0.5f, corner * 0.5f, fillPaint)
            }
        }
    }

    fun resolveBatteryLevelColorArgb(
        levelPercent: Int,
        isCharging: Boolean,
        colorByLevel: Boolean,
        fallbackArgb: Int
    ): Int {
        if (!colorByLevel || levelPercent !in 0..100) {
            return fallbackArgb
        }
        return when {
            isCharging -> 0xFF00E5FF.toInt() // Electric Cyan charging
            levelPercent <= 20 -> 0xFFEF4444.toInt() // Danger Red (low battery)
            levelPercent <= 50 -> 0xFFF59E0B.toInt() // Amber Warning
            else -> 0xFF22C55E.toInt() // Neon Green
        }
    }

    /**
     * Resolves color for Battery Temperature display based on physical battery thermal thresholds.
     */
    fun resolveBatteryTempColorArgb(
        tempCelsius: Float?,
        warnByColor: Boolean,
        fallbackArgb: Int
    ): Int {
        if (!warnByColor || tempCelsius == null) return fallbackArgb
        return when {
            tempCelsius >= 40.0f -> 0xFFEF4444.toInt() // Hot warning / Overheat (Red)
            tempCelsius >= 35.0f -> 0xFFF97316.toInt() // Warm caution (Amber/Orange)
            else -> 0xFF38BDF8.toInt() // Cool Ice Blue
        }
    }

    /**
     * Resolves color for Internal Storage (Bộ nhớ) display based on usage threshold.
     */
    fun resolveStorageColorArgb(
        usedPercent: Int,
        warnByColor: Boolean,
        fallbackArgb: Int = 0xFFC084FC.toInt()
    ): Int {
        if (!warnByColor || usedPercent !in 0..100) return fallbackArgb
        return when {
            usedPercent >= 90 -> 0xFFEF4444.toInt() // Danger: storage almost full
            usedPercent >= 75 -> 0xFFF59E0B.toInt() // Warning: storage high
            else -> 0xFFC084FC.toInt() // Neon Purple/Violet
        }
    }

    /**
     * Resolves color for RAM status based on memory pressure.
     */
    fun resolveRamColorArgb(
        usedPercent: Int,
        warnByColor: Boolean,
        fallbackArgb: Int = 0xFF00E5FF.toInt()
    ): Int {
        if (!warnByColor || usedPercent !in 0..100) return fallbackArgb
        return when {
            usedPercent >= 85 -> 0xFFEF4444.toInt() // Danger: RAM overloaded
            usedPercent >= 70 -> 0xFFF59E0B.toInt() // Warning: RAM high
            else -> 0xFF00E5FF.toInt() // Electric Cyan
        }
    }

    // =========================================================================
    // TRIO GLASS TELEMETRY CARDS (PIN + BỘ NHỚ + RAM)
    // =========================================================================

    fun renderBatteryTrioCardBitmap(
        battery: BatteryInfo,
        config: WidgetConfig,
        density: Float,
        targetWidthDp: Int = 68,
        targetHeightDp: Int = 78
    ): Bitmap {
        val pxW = (targetWidthDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(100)
        val pxH = (targetHeightDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(120)
        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBatteryTrioCard(canvas, pxW.toFloat(), pxH.toFloat(), battery, config, density)
        return bitmap
    }

    fun renderStorageTrioCardBitmap(
        storage: StorageInfo,
        config: WidgetConfig,
        density: Float,
        targetWidthDp: Int = 68,
        targetHeightDp: Int = 78
    ): Bitmap {
        val pxW = (targetWidthDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(100)
        val pxH = (targetHeightDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(120)
        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawStorageTrioCard(canvas, pxW.toFloat(), pxH.toFloat(), storage, config, density)
        return bitmap
    }

    fun renderRamTrioCardBitmap(
        ram: RamInfo,
        config: WidgetConfig,
        density: Float,
        targetWidthDp: Int = 68,
        targetHeightDp: Int = 78
    ): Bitmap {
        val pxW = (targetWidthDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(100)
        val pxH = (targetHeightDp * density.coerceIn(1.5f, 3.0f)).toInt().coerceAtLeast(120)
        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawRamTrioCard(canvas, pxW.toFloat(), pxH.toFloat(), ram, config, density)
        return bitmap
    }

    private fun drawCardGlassBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        accentColor: Int,
        density: Float
    ) {
        val strokeW = 1.3f * density
        val corner = 13.5f * density
        val cardRect = RectF(strokeW, strokeW, w - strokeW, h - strokeW)

        // 1. Dark Obsidian Glass Base
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(190, 15, 23, 42) // #0F172A dark obsidian
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        // 2. Bottom Ambient Neon Glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                w * 0.5f, h * 0.45f, w * 0.5f, h - strokeW,
                Color.TRANSPARENT,
                ColorUtils.setAlphaComponent(accentColor, 55),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, corner, corner, glowPaint)

        // 3. Specular Glass Rim Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeW
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                ColorUtils.setAlphaComponent(accentColor, 180),
                ColorUtils.setAlphaComponent(accentColor, 75),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
    }

    fun drawBatteryTrioCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        battery: BatteryInfo,
        config: WidgetConfig,
        density: Float
    ) {
        val u = kotlin.math.min(w / 68f, h / 78f).coerceAtLeast(0.42f)
        val accentColor = resolveBatteryLevelColorArgb(
            levelPercent = battery.levelPercent,
            isCharging = battery.isCharging,
            colorByLevel = config.dynamicAlertColors,
            fallbackArgb = 0xFF22C55E.toInt()
        )
        val tempColor = resolveBatteryTempColorArgb(
            tempCelsius = battery.temperatureCelsius,
            warnByColor = config.dynamicAlertColors,
            fallbackArgb = 0xFF38BDF8.toInt()
        )

        drawCardGlassBackground(canvas, w, h, accentColor, u)

        val cx = w * 0.5f
        val pct = battery.levelPercent.coerceIn(0, 100)
        val levelFraction = if (battery.hasLevelData) pct / 100f else 0.5f

        // Top Battery Icon
        val iconW = 15f * u
        val iconH = 7.5f * u
        val iconTop = h * 0.08f
        val iconRect = RectF(cx - iconW * 0.5f, iconTop, cx + iconW * 0.5f, iconTop + iconH)
        val iconCorner = iconH * 0.35f

        val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = accentColor
        }
        canvas.drawRoundRect(iconRect, iconCorner, iconCorner, iconFillPaint)

        // Terminal tip
        val tipRect = RectF(iconRect.right + 0.7f * u, iconRect.centerY() - iconH * 0.22f, iconRect.right + 2.0f * u, iconRect.centerY() + iconH * 0.22f)
        canvas.drawRoundRect(tipRect, 0.7f * u, 0.7f * u, iconFillPaint)

        // Bolt inside battery icon
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val boltPath = Path().apply {
            moveTo(cx + 0.4f * u, iconRect.top + 1.0f * u)
            lineTo(cx - 2.2f * u, iconRect.centerY() + 0.4f * u)
            lineTo(cx - 0.1f * u, iconRect.centerY() + 0.4f * u)
            lineTo(cx - 0.4f * u, iconRect.bottom - 1.0f * u)
            lineTo(cx + 2.2f * u, iconRect.centerY() - 0.4f * u)
            lineTo(cx + 0.1f * u, iconRect.centerY() - 0.4f * u)
            close()
        }
        canvas.drawPath(boltPath, boltPaint)

        // Circular Arc Gauge wrapping percentage
        val arcCenterY = h * 0.40f
        val arcRadius = kotlin.math.min(w * 0.25f, h * 0.19f)
        val arcRect = RectF(cx - arcRadius, arcCenterY - arcRadius, cx + arcRadius, arcCenterY + arcRadius)
        val arcStrokeW = 2.1f * u

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = arcStrokeW
            strokeCap = Paint.Cap.ROUND
            color = ColorUtils.setAlphaComponent(accentColor, 40)
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = arcStrokeW
            strokeCap = Paint.Cap.ROUND
            color = accentColor
        }
        canvas.drawArc(arcRect, 135f, 270f * levelFraction, false, progressPaint)

        // Percentage Text inside Arc
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 11.2f * u
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            setShadowLayer(3.5f * u, 0f, 1f * u, Color.argb(160, 0, 0, 0))
        }
        val pctFm = pctPaint.fontMetrics
        canvas.drawText("$pct%", cx, arcCenterY - (pctFm.ascent + pctFm.descent) * 0.5f, pctPaint)

        // "Pin" Label cleanly positioned between arc and bottom chip
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.2f * u
            color = 0xFFE2E8F0.toInt()
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("Pin", cx, h * 0.70f, labelPaint)

        // Bottom Thermometer Chip: "🌡 29.6°C"
        val chipW = w * 0.84f
        val chipH = (h * 0.17f).coerceAtMost(13.5f * u)
        val chipTop = h * 0.77f
        val chipRect = RectF(cx - chipW * 0.5f, chipTop, cx + chipW * 0.5f, chipTop + chipH)
        val chipCorner = chipH * 0.48f

        val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(168, 10, 15, 26)
        }
        canvas.drawRoundRect(chipRect, chipCorner, chipCorner, chipBgPaint)

        val chipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f * u
            color = ColorUtils.setAlphaComponent(tempColor, 110)
        }
        canvas.drawRoundRect(chipRect, chipCorner, chipCorner, chipBorderPaint)

        val tempText = "🌡 " + battery.formatTemperature(config.temperatureUnit)
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.4f * u
            color = tempColor
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        }
        val chipFm = chipTextPaint.fontMetrics
        canvas.drawText(tempText, cx, chipRect.centerY() - (chipFm.ascent + chipFm.descent) * 0.5f, chipTextPaint)
    }

    fun drawStorageTrioCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        storage: StorageInfo,
        config: WidgetConfig,
        density: Float
    ) {
        val u = kotlin.math.min(w / 68f, h / 78f).coerceAtLeast(0.42f)
        val accentColor = resolveStorageColorArgb(
            usedPercent = storage.usedPercent,
            warnByColor = config.dynamicAlertColors,
            fallbackArgb = 0xFFC084FC.toInt()
        )

        drawCardGlassBackground(canvas, w, h, accentColor, u)

        val cx = w * 0.5f
        val pct = storage.usedPercent.coerceIn(0, 100)
        val levelFraction = pct / 100f

        // Top Database Cylinder Icon
        val diskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f * u
            color = Color.WHITE
        }
        val diskFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(accentColor, 160)
        }

        val diskTop = h * 0.09f
        val diskW = 15f * u
        val diskH = 4.2f * u

        val topOval = RectF(cx - diskW * 0.5f, diskTop, cx + diskW * 0.5f, diskTop + diskH)
        canvas.drawOval(topOval, diskFill)
        canvas.drawOval(topOval, diskPaint)

        val botOval = RectF(cx - diskW * 0.5f, diskTop + 4.2f * u, cx + diskW * 0.5f, diskTop + diskH + 4.2f * u)
        canvas.drawArc(botOval, 0f, 180f, false, diskPaint)

        // Label: "Bộ nhớ"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 8.0f * u
            color = 0xFFE2E8F0.toInt()
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("Bộ nhớ", cx, h * 0.36f, labelPaint)

        // Main Percentage: "68%"
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 13.2f * u
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            setShadowLayer(3.5f * u, 0f, 1f * u, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText("$pct%", cx, h * 0.55f, pctPaint)

        // Horizontal Glowing Progress Bar
        val barW = w * 0.78f
        val barH = 3.8f * u
        val barY = h * 0.67f
        val barRect = RectF(cx - barW * 0.5f, barY - barH * 0.5f, cx + barW * 0.5f, barY + barH * 0.5f)
        val barCorner = barH * 0.5f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(accentColor, 45)
        }
        canvas.drawRoundRect(barRect, barCorner, barCorner, trackPaint)

        val fillW = (barW * levelFraction).coerceAtLeast(barH)
        val fillRect = RectF(barRect.left, barRect.top, (barRect.left + fillW).coerceAtMost(barRect.right), barRect.bottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                fillRect.left, fillRect.centerY(), fillRect.right, fillRect.centerY(),
                ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                accentColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(fillRect, barCorner, barCorner, fillPaint)

        // Bottom Detail: "74.2 / 108 GB"
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.0f * u
            color = 0xFFCBD5E1.toInt()
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val detailText = storage.formatUsedAndTotalGb()
        canvas.drawText(detailText, cx, h * 0.87f, detailPaint)
    }

    fun drawRamTrioCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        ram: RamInfo,
        config: WidgetConfig,
        density: Float
    ) {
        val u = kotlin.math.min(w / 68f, h / 78f).coerceAtLeast(0.42f)
        val accentColor = resolveRamColorArgb(
            usedPercent = ram.usedPercent,
            warnByColor = config.dynamicAlertColors,
            fallbackArgb = 0xFF00E5FF.toInt()
        )

        drawCardGlassBackground(canvas, w, h, accentColor, u)

        val cx = w * 0.5f
        val pct = ram.usedPercent.coerceIn(0, 100)
        val levelFraction = pct / 100f

        // Top RAM Microchip Icon
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f * u
            color = Color.WHITE
        }
        val chipFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(accentColor, 150)
        }

        val iconTop = h * 0.09f
        val chipW = 16f * u
        val chipH = 8.5f * u
        val chipRect = RectF(cx - chipW * 0.5f, iconTop, cx + chipW * 0.5f, iconTop + chipH)
        val chipCorner = 1.8f * u
        canvas.drawRoundRect(chipRect, chipCorner, chipCorner, chipFill)
        canvas.drawRoundRect(chipRect, chipCorner, chipCorner, chipPaint)

        // 3 internal modules inside RAM chip
        val modPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.85f * u
            color = Color.WHITE
        }
        val mPad = 2.2f * u
        canvas.drawRect(RectF(chipRect.left + mPad, chipRect.top + 1.6f * u, chipRect.left + mPad + 2.3f * u, chipRect.bottom - 1.6f * u), modPaint)
        canvas.drawRect(RectF(cx - 1.15f * u, chipRect.top + 1.6f * u, cx + 1.15f * u, chipRect.bottom - 1.6f * u), modPaint)
        canvas.drawRect(RectF(chipRect.right - mPad - 2.3f * u, chipRect.top + 1.6f * u, chipRect.right - mPad, chipRect.bottom - 1.6f * u), modPaint)

        // Label: "RAM"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 8.0f * u
            color = 0xFFE2E8F0.toInt()
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("RAM", cx, h * 0.36f, labelPaint)

        // Main Percentage: "72%"
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 13.2f * u
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            setShadowLayer(3.5f * u, 0f, 1f * u, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText("$pct%", cx, h * 0.55f, pctPaint)

        // Sub Detail: "8.0 / 11.0G"
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.0f * u
            color = 0xFFCBD5E1.toInt()
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val detailText = "${ram.formatUsedGb()} / ${ram.formatTotalGb()}G"
        canvas.drawText(detailText, cx, h * 0.70f, detailPaint)

        // Horizontal Glowing Progress Bar at bottom
        val barW = w * 0.78f
        val barH = 3.8f * u
        val barY = h * 0.85f
        val barRect = RectF(cx - barW * 0.5f, barY - barH * 0.5f, cx + barW * 0.5f, barY + barH * 0.5f)
        val barCorner = barH * 0.5f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(accentColor, 45)
        }
        canvas.drawRoundRect(barRect, barCorner, barCorner, trackPaint)

        val fillW = (barW * levelFraction).coerceAtLeast(barH)
        val fillRect = RectF(barRect.left, barRect.top, (barRect.left + fillW).coerceAtMost(barRect.right), barRect.bottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                fillRect.left, fillRect.centerY(), fillRect.right, fillRect.centerY(),
                ColorUtils.blendARGB(accentColor, Color.WHITE, 0.25f),
                accentColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(fillRect, barCorner, barCorner, fillPaint)
    }
}
