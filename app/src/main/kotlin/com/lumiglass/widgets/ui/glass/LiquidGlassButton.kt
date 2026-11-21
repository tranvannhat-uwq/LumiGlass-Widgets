package com.lumiglass.widgets.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.tanh

/**
 * Premium Liquid Glass Button with Apple-style tactile elasticity:
 * - Natural tanh displacement & damping
 * - Subtle scale expansion on press
 * - Directional light reflection rim
 * - Crisp adaptive typography
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    tintColor: Color = Color.White.copy(alpha = 0.12f),
    elevation: Dp = 4.dp,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    minHeight: Dp = 42.dp,
    horizontalPadding: Dp = 14.dp,
    verticalPadding: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val pressProgress = remember { Animatable(0f) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    val contentColor = when {
        isPrimary -> Color.White
        dark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    // High-end Material Glass Palette
    val bgBrush = when {
        isPrimary -> Brush.verticalGradient(
            listOf(
                Color(0xFF2563EB),
                Color(0xFF1D4ED8)
            )
        )
        dark -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.08f)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFE2E8F0).copy(alpha = 0.72f)
            )
        )
    }
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isPrimary) 0.55f else if (dark) 0.55f else 0.90f),
            Color.White.copy(alpha = if (isPrimary) 0.14f else 0.12f)
        )
    )

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .background(bgBrush, shape)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.52f
                val progress = pressProgress.value
                val scale = lerp(1f, 0.96f, progress)
                scaleX = scale
                scaleY = scale

                // Elastic displacement dampened by tanh
                val maxOffset = 12.dp.toPx()
                translationX = maxOffset * tanh(0.04f * touchOffset.x / maxOffset) * progress
                translationY = maxOffset * tanh(0.04f * touchOffset.y / maxOffset) * progress
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = { offset ->
                            touchOffset = offset - Offset(size.width / 2f, size.height / 2f)
                            scope.launch {
                                pressProgress.animateTo(
                                    1f,
                                    spring(dampingRatio = 0.7f, stiffness = 600f)
                                )
                            }
                            tryAwaitRelease()
                            scope.launch {
                                pressProgress.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.6f, stiffness = 400f)
                                )
                            }
                        },
                        onTap = { currentOnClick() }
                    )
                }
            }
            .semantics {
                role = Role.Button
                if (enabled) {
                    onClick {
                        currentOnClick()
                        true
                    }
                } else {
                    disabled()
                }
            }
            .heightIn(min = minHeight)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}
