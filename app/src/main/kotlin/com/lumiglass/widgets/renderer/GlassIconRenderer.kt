package com.lumiglass.widgets.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.lumiglass.widgets.model.BatteryDisplayStyle
import com.lumiglass.widgets.model.BatteryInfo
import com.lumiglass.widgets.model.GlassIconStyle
import com.lumiglass.widgets.model.WeatherConditionCategory
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
            isCharging -> Color.parseColor("#34D399")
            levelPercent <= 20 -> Color.parseColor("#F87171")
            levelPercent <= 45 -> Color.parseColor("#FBBF24")
            else -> Color.parseColor("#34D399")
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
            tempCelsius >= 42.0f -> Color.parseColor("#F87171") // Hot warning (Coral Red)
            tempCelsius >= 38.0f -> Color.parseColor("#FBBF24") // Warm caution (Amber)
            else -> fallbackArgb
        }
    }
}
