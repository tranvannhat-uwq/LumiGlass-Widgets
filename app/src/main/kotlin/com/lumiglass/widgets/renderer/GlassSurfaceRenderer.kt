package com.lumiglass.widgets.renderer

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.core.graphics.ColorUtils
import com.lumiglass.widgets.model.GlassThemeMode
import com.lumiglass.widgets.model.WidgetConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Procedural Liquid Glass Optical Engine.
 * Renders multi-layered translucent glass surfaces with:
 * 1. Ambient depth drop shadow
 * 2. Primary & secondary diagonal glass tint gradient
 * 3. Simulated frosted satin diffusion layer
 * 4. Top-left soft caustic specular sheen
 * 5. Fresnel edge refraction inner rim (optical thickness around corners)
 * 6. Asymmetric specular rim border (Sweep/Diagonal reflection along edges)
 *
 * Used identically by both Android AppWidget RemoteViews and the Jetpack Compose Widget Studio preview.
 */
object GlassSurfaceRenderer {

    data class EffectiveGlassPalette(
        val primaryTintArgb: Int,
        val secondaryTintArgb: Int,
        val borderArgb: Int,
        val effectiveOpacity: Float,
        val isDarkGlass: Boolean,
        val primaryTextArgb: Int,
        val secondaryTextArgb: Int,
        val accentTextArgb: Int,
        val shadowTextArgb: Int
    )

    /**
     * Resolves the effective optical palette taking into account Adaptive Glass, ThemeMode (Light/Dark/Auto),
     * and automatic contrast enforcement.
     */
    fun resolvePalette(
        config: WidgetConfig,
        isSystemDarkTheme: Boolean
    ): EffectiveGlassPalette {
        val useDarkMode = when (config.themeMode) {
            GlassThemeMode.DARK -> true
            GlassThemeMode.LIGHT -> false
            GlassThemeMode.AUTO -> isSystemDarkTheme
        }

        val rawPrimary = config.glassTintColorHex.toInt()
        val rawSecondary = config.glassSecondaryTintHex.toInt()
        val rawBorder = config.borderColorHex.toInt()

        val isAdaptive = config.presetId == "adaptive_glass" || config.themeMode == GlassThemeMode.AUTO

        val primaryTint: Int
        val secondaryTint: Int
        val borderTint: Int
        val effectiveOpacity: Float

        if (isAdaptive && useDarkMode && ColorUtils.calculateLuminance(rawPrimary) > 0.35) {
            // Shift bright/adaptive glass smoothly into deep smoky twilight glass in Dark Mode
            primaryTint = ColorUtils.blendARGB(rawPrimary, Color.parseColor("#0F172A"), 0.76f)
            secondaryTint = ColorUtils.blendARGB(rawSecondary, Color.parseColor("#1E293B"), 0.72f)
            borderTint = ColorUtils.blendARGB(rawBorder, Color.parseColor("#94A3B8"), 0.35f)
            effectiveOpacity = (config.glassOpacity + 0.16f).coerceIn(0.0f, 0.92f)
        } else {
            primaryTint = rawPrimary
            secondaryTint = rawSecondary
            borderTint = rawBorder
            effectiveOpacity = config.glassOpacity.coerceIn(0.0f, 1.0f)
        }

        val primaryLum = ColorUtils.calculateLuminance(primaryTint)
        val isDarkSurface = useDarkMode || (primaryLum < 0.28 && effectiveOpacity > 0.30f)

        val resolvedPrimaryText: Int = if (config.autoTextColor) {
            if (!isDarkSurface && effectiveOpacity >= 0.44f && primaryLum > 0.65) {
                Color.parseColor("#0F172A") // Deep slate on opaque frosted pearl glass
            } else {
                Color.parseColor("#FFFFFF") // Crisp white with subtle shadow on translucent/dark glass
            }
        } else {
            val userColor = config.customTextColorHex.toInt()
            if (config.autoContrastEnsure) {
                ensureReadableTextContrast(userColor, primaryTint, effectiveOpacity)
            } else {
                userColor
            }
        }

        val textIsDark = ColorUtils.calculateLuminance(resolvedPrimaryText) < 0.35
        val resolvedSecondaryText = ColorUtils.setAlphaComponent(
            resolvedPrimaryText,
            if (textIsDark) 205 else 218
        )
        val resolvedAccentText = if (textIsDark) {
            Color.parseColor("#0369A1")
        } else {
            Color.parseColor("#7DD3FC")
        }
        val shadowTextArgb = if (textIsDark) {
            Color.argb(75, 255, 255, 255)
        } else {
            Color.argb(150, 8, 15, 30)
        }

        return EffectiveGlassPalette(
            primaryTintArgb = primaryTint,
            secondaryTintArgb = secondaryTint,
            borderArgb = borderTint,
            effectiveOpacity = effectiveOpacity,
            isDarkGlass = isDarkSurface,
            primaryTextArgb = resolvedPrimaryText,
            secondaryTextArgb = resolvedSecondaryText,
            accentTextArgb = resolvedAccentText,
            shadowTextArgb = shadowTextArgb
        )
    }

    private fun ensureReadableTextContrast(
        desiredTextArgb: Int,
        glassTintArgb: Int,
        glassOpacity: Float
    ): Int {
        // When opacity is high (> 0.50), ensure sufficient contrast against the glass surface itself
        if (glassOpacity > 0.50f) {
            val opaqueBg = ColorUtils.setAlphaComponent(glassTintArgb, 255)
            val textOpaque = ColorUtils.setAlphaComponent(desiredTextArgb, 255)
            val contrast = ColorUtils.calculateContrast(textOpaque, opaqueBg)
            if (contrast < 3.2) {
                val bgLum = ColorUtils.calculateLuminance(opaqueBg)
                return if (bgLum > 0.5) Color.parseColor("#0F172A") else Color.parseColor("#FFFFFF")
            }
        }
        return ColorUtils.setAlphaComponent(desiredTextArgb, 255)
    }

    /**
     * Renders a complete Liquid Glass surface into an ARGB_8888 Bitmap optimized for RemoteViews or Compose.
     * Bounds are automatically clamped to a safe resolution to avoid Binder TransactionTooLargeException.
     */
    fun renderGlassBitmap(
        widthDp: Int,
        heightDp: Int,
        density: Float,
        config: WidgetConfig,
        isSystemDarkTheme: Boolean,
        drawInternalDividersForTier: DividerZoneLayout = DividerZoneLayout.NONE
    ): Bitmap {
        val safeDensity = density.coerceIn(1.25f, 2.25f)
        val rawW = (widthDp * safeDensity).toInt().coerceAtLeast(120)
        val rawH = (heightDp * safeDensity).toInt().coerceAtLeast(64)

        // Cap maximum pixel dimension to 760px so Bitmap stays under ~350KB in RemoteViews IPC
        val maxDim = max(rawW, rawH)
        val downscale = if (maxDim > 760) 760f / maxDim.toFloat() else 1f
        val pxW = (rawW * downscale).toInt().coerceAtLeast(100)
        val pxH = (rawH * downscale).toInt().coerceAtLeast(56)
        val effDensity = safeDensity * downscale

        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawGlassSurfaceOnCanvas(
            canvas = canvas,
            widthPx = pxW.toFloat(),
            heightPx = pxH.toFloat(),
            density = effDensity,
            config = config,
            isSystemDarkTheme = isSystemDarkTheme,
            dividerLayout = drawInternalDividersForTier
        )
        return bitmap
    }

    enum class DividerZoneLayout {
        NONE,
        VERTICAL_SPLIT_4X2,
        PREMIUM_4X3,
        DASHBOARD_4X4
    }

    /**
     * Draws the multi-layered Liquid Glass surface onto any Android [Canvas].
     */
    fun drawGlassSurfaceOnCanvas(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        density: Float,
        config: WidgetConfig,
        isSystemDarkTheme: Boolean,
        dividerLayout: DividerZoneLayout = DividerZoneLayout.NONE
    ) {
        val palette = resolvePalette(config, isSystemDarkTheme)
        val shadowStrength = config.shadowIntensity.coerceIn(0f, 1f)
        val shadowInset = if (shadowStrength > 0.03f) (4.5f * density) else (1.5f * density)
        val cornerPx = (config.cornerRadiusDp.coerceIn(8f, 38f) * density)
            .coerceAtMost(min(widthPx, heightPx) * 0.46f)

        val glassRect = RectF(
            shadowInset,
            shadowInset * 0.75f,
            widthPx - shadowInset,
            heightPx - shadowInset * 1.25f
        )

        // 1. Outer Ambient Depth Shadow
        if (shadowStrength > 0.03f) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                val shadowAlpha = (shadowStrength * 115f).toInt().coerceIn(0, 140)
                color = Color.argb(shadowAlpha, 4, 9, 20)
                val blurRad = (shadowInset * 0.92f).coerceAtLeast(1.5f)
                maskFilter = BlurMaskFilter(blurRad, BlurMaskFilter.Blur.NORMAL)
            }
            val shadowRect = RectF(glassRect).apply {
                offset(0f, shadowInset * 0.32f)
                inset(shadowInset * 0.15f, shadowInset * 0.15f)
            }
            canvas.drawRoundRect(shadowRect, cornerPx, cornerPx, shadowPaint)
        }

        val clipPath = Path().apply {
            addRoundRect(glassRect, cornerPx, cornerPx, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // 2. Translucent Optical Glass Body Gradient
        val opacity = palette.effectiveOpacity
        if (opacity > 0.005f) {
            val topAlpha = (opacity * 245f).toInt().coerceIn(0, 248)
            val midAlpha = (opacity * 195f).toInt().coerceIn(0, 235)
            val botAlpha = (opacity * 230f).toInt().coerceIn(0, 245)

            val cTop = ColorUtils.setAlphaComponent(palette.primaryTintArgb, topAlpha)
            val cMid = ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(palette.primaryTintArgb, palette.secondaryTintArgb, 0.45f),
                midAlpha
            )
            val cBot = ColorUtils.setAlphaComponent(palette.secondaryTintArgb, botAlpha)

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    glassRect.left,
                    glassRect.top,
                    glassRect.right,
                    glassRect.bottom,
                    intArrayOf(cTop, cMid, cBot),
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(glassRect, bodyPaint)
        }

        // 3. Simulated Frosted Satin Diffusion Layer (Frost / Subsurface scatter)
        val frost = config.frostAmount.coerceIn(0f, 1f)
        if (frost > 0.02f && opacity > 0.01f) {
            val frostBaseColor = if (palette.isDarkGlass) {
                Color.rgb(148, 163, 184)
            } else {
                Color.rgb(255, 255, 255)
            }
            val frostAlphaTop = (frost * opacity * 135f).toInt().coerceIn(0, 140)
            val frostAlphaBottom = (frost * opacity * 65f).toInt().coerceIn(0, 85)

            val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    glassRect.left,
                    glassRect.top,
                    glassRect.left + glassRect.width() * 0.6f,
                    glassRect.bottom,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(frostBaseColor, frostAlphaTop),
                        ColorUtils.setAlphaComponent(frostBaseColor, (frostAlphaTop + frostAlphaBottom) / 2),
                        ColorUtils.setAlphaComponent(frostBaseColor, frostAlphaBottom)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(glassRect, frostPaint)
        }

        // 4. Top-Left Caustic Specular Highlight Sheen
        val highlight = config.highlightIntensity.coerceIn(0f, 1f)
        if (highlight > 0.02f) {
            val sheenAlphaTop = (highlight * 95f).toInt().coerceIn(0, 125)
            val sheenAlphaMid = (highlight * 28f).toInt().coerceIn(0, 50)
            val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = RadialGradient(
                    glassRect.left + glassRect.width() * 0.24f,
                    glassRect.top + glassRect.height() * 0.08f,
                    max(glassRect.width(), glassRect.height()) * 0.78f,
                    intArrayOf(
                        Color.argb(sheenAlphaTop, 255, 255, 255),
                        Color.argb(sheenAlphaMid, 240, 249, 255),
                        Color.argb(0, 255, 255, 255)
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(glassRect, sheenPaint)
        }

        // 5. Edge Refraction Inner Rim (Fresnel optical thickness along perimeter)
        val refraction = config.refractionIntensity.coerceIn(0f, 1f)
        if (refraction > 0.02f) {
            val rimThickness = 3.2f * density
            val innerRimRect = RectF(glassRect).apply {
                inset(rimThickness * 0.45f, rimThickness * 0.45f)
            }
            val refractAlphaBright = (refraction * 105f).toInt().coerceIn(0, 130)
            val refractAlphaSoft = (refraction * 22f).toInt().coerceIn(0, 45)
            val refractColor = ColorUtils.blendARGB(palette.borderArgb, Color.WHITE, 0.55f)

            val refractionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = rimThickness
                shader = LinearGradient(
                    glassRect.left,
                    glassRect.top,
                    glassRect.right,
                    glassRect.bottom,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(refractColor, refractAlphaBright),
                        ColorUtils.setAlphaComponent(refractColor, refractAlphaSoft),
                        ColorUtils.setAlphaComponent(palette.secondaryTintArgb, refractAlphaSoft),
                        ColorUtils.setAlphaComponent(refractColor, (refractAlphaBright * 0.65f).toInt())
                    ),
                    floatArrayOf(0f, 0.38f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(
                innerRimRect,
                (cornerPx - rimThickness * 0.4f).coerceAtLeast(4f),
                (cornerPx - rimThickness * 0.4f).coerceAtLeast(4f),
                refractionPaint
            )
        }

        // 6. Optional Hairline Specular Internal Zone Dividers (No ugly background blocks!)
        drawInternalGlassDividers(
            canvas = canvas,
            glassRect = glassRect,
            density = density,
            borderArgb = palette.borderArgb,
            borderLuminance = config.borderLuminance,
            dividerLayout = dividerLayout
        )

        canvas.restore()

        // 7. Outer Specular Rim Border (Asymmetric sweep reflection along glass edges)
        val borderLum = config.borderLuminance.coerceIn(0f, 1f)
        if (borderLum > 0.02f) {
            val strokeW = (1.15f * density).coerceAtLeast(1.2f)
            val borderRect = RectF(glassRect).apply {
                inset(strokeW * 0.5f, strokeW * 0.5f)
            }
            val brightAlpha = (borderLum * 235f).toInt().coerceIn(0, 250)
            val midAlpha = (borderLum * 75f).toInt().coerceIn(0, 120)
            val dimAlpha = (borderLum * 28f).toInt().coerceIn(0, 65)
            val counterAlpha = (borderLum * 165f).toInt().coerceIn(0, 210)

            val specularWhite = ColorUtils.blendARGB(palette.borderArgb, Color.WHITE, 0.6f)
            val cBright = ColorUtils.setAlphaComponent(specularWhite, brightAlpha)
            val cMid = ColorUtils.setAlphaComponent(palette.borderArgb, midAlpha)
            val cDim = ColorUtils.setAlphaComponent(palette.borderArgb, dimAlpha)
            val cCounter = ColorUtils.setAlphaComponent(palette.borderArgb, counterAlpha)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                shader = SweepGradient(
                    borderRect.centerX(),
                    borderRect.centerY(),
                    intArrayOf(
                        cDim,      // 0 deg (right)
                        cCounter,  // 45 deg (bottom-right reflection)
                        cMid,      // 90 deg (bottom)
                        cDim,      // 135 deg (bottom-left)
                        cMid,      // 180 deg (left)
                        cBright,   // 225 deg (top-left primary specular)
                        cBright,   // 270 deg (top edge highlight)
                        cMid,      // 315 deg (top-right)
                        cDim       // 360 deg
                    ),
                    floatArrayOf(0f, 0.12f, 0.25f, 0.38f, 0.50f, 0.62f, 0.76f, 0.88f, 1f)
                )
            }
            canvas.drawRoundRect(
                borderRect,
                (cornerPx - strokeW * 0.5f).coerceAtLeast(4f),
                (cornerPx - strokeW * 0.5f).coerceAtLeast(4f),
                borderPaint
            )
        }
    }

    private fun drawInternalGlassDividers(
        canvas: Canvas,
        glassRect: RectF,
        density: Float,
        borderArgb: Int,
        borderLuminance: Float,
        dividerLayout: DividerZoneLayout
    ) {
        if (dividerLayout == DividerZoneLayout.NONE) return
        val alphaEdge = 0
        val alphaCenter = (borderLuminance.coerceIn(0.25f, 1f) * 85f).toInt().coerceIn(25, 110)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (0.9f * density).coerceAtLeast(1f)
        }

        when (dividerLayout) {
            DividerZoneLayout.VERTICAL_SPLIT_4X2 -> {
                val x = glassRect.left + glassRect.width() * 0.50f
                val topY = glassRect.top + glassRect.height() * 0.18f
                val botY = glassRect.bottom - glassRect.height() * 0.18f
                linePaint.shader = LinearGradient(
                    x, topY, x, botY,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge),
                        ColorUtils.setAlphaComponent(borderArgb, alphaCenter),
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawLine(x, topY, x, botY, linePaint)
            }
            DividerZoneLayout.PREMIUM_4X3 -> {
                val y = glassRect.top + glassRect.height() * 0.58f
                val leftX = glassRect.left + glassRect.width() * 0.08f
                val rightX = glassRect.right - glassRect.width() * 0.08f
                linePaint.shader = LinearGradient(
                    leftX, y, rightX, y,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge),
                        ColorUtils.setAlphaComponent(borderArgb, alphaCenter),
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawLine(leftX, y, rightX, y, linePaint)
            }
            DividerZoneLayout.DASHBOARD_4X4 -> {
                val y1 = glassRect.top + glassRect.height() * 0.44f
                val y2 = glassRect.top + glassRect.height() * 0.74f
                val leftX = glassRect.left + glassRect.width() * 0.07f
                val rightX = glassRect.right - glassRect.width() * 0.07f
                val hShader = LinearGradient(
                    leftX, y1, rightX, y1,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge),
                        ColorUtils.setAlphaComponent(borderArgb, alphaCenter),
                        ColorUtils.setAlphaComponent(borderArgb, alphaEdge)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                linePaint.shader = hShader
                canvas.drawLine(leftX, y1, rightX, y1, linePaint)
                canvas.drawLine(leftX, y2, rightX, y2, linePaint)
            }
            DividerZoneLayout.NONE -> Unit
        }
    }
}
