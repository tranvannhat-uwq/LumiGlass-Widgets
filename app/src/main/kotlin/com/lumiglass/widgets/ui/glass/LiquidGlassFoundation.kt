package com.lumiglass.widgets.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/**
 * Checks if the current Android runtime supports AGSL RuntimeShader (API 33+).
 * This ensures 100% graceful degradation on older devices or emulators.
 */
fun isLiquidGlassShaderSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * Vibrant ColorMatrix that enriches saturation by ~1.2x to recreate natural vibrancy
 * on Backdrop 1.0.6 as recommended in the component recipes.
 */
val LiquidGlassVibrancyFilter: ColorFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            1.22f, -0.11f, -0.11f, 0f, 0f,
            -0.11f, 1.22f, -0.11f, 0f, 0f,
            -0.11f, -0.11f, 1.22f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * High-end Liquid Glass Surface Modifier:
 * Applies optical refraction (lens), frosted blur, vibrancy filter, and directional specular highlight rim.
 * Falls back gracefully to translucent acrylic glass when RuntimeShader is unavailable.
 */
fun Modifier.liquidGlassSurface(
    backdrop: LayerBackdrop,
    shape: Shape = RoundedCornerShape(26.dp),
    blurRadius: Float = 14f,
    refractionHeight: Float = 22f,
    refractionAmount: Float = 22f,
    tintColor: Color = Color.White.copy(alpha = 0.08f),
    rimWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    isDarkTheme: Boolean? = null
): Modifier = composed {
    val dark = isDarkTheme ?: isSystemInDarkTheme()
    val canRenderShader = remember { isLiquidGlassShaderSupported() }

    val shadowColor = if (dark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.16f)

    if (canRenderShader) {
        this
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    colorFilter(LiquidGlassVibrancyFilter)
                    blur(blurRadius)
                    lens(
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        depthEffect = false,
                        chromaticAberration = false
                    )
                },
                highlight = {
                    Highlight(
                        width = rimWidth,
                        alpha = if (dark) 0.85f else 0.70f,
                        style = HighlightStyle.Default
                    )
                },
                shadow = { Shadow.Default },
                onDrawSurface = {
                    // Soft translucent tint over refracted surface
                    drawRect(tintColor)
                    if (dark) {
                        // Subtle dark scrim to maintain text contrast in dark theme
                        drawRect(Color.Black.copy(alpha = 0.22f))
                    } else {
                        // Subtle bright pearl sheen in light theme
                        drawRect(Color.White.copy(alpha = 0.14f))
                    }
                }
            )
    } else {
        // Fallback: Elegant Frosted Acrylic Surface with Directional Specular Border
        val fallbackBg = if (dark) {
            Color(0xFF1E293B).copy(alpha = 0.82f)
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.86f)
        }
        val rimBrush = if (dark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.90f),
                    Color.White.copy(alpha = 0.25f)
                )
            )
        }

        this
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(fallbackBg, shape)
            .border(width = rimWidth, brush = rimBrush, shape = shape)
    }
}

