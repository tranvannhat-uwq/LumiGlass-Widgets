package com.lumiglass.widgets.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Premium Apple-style Liquid Glass Toggle / Switch:
 * - Fluid spring animation on state change
 * - Translucent glass track with rim reflection
 * - Dynamic thumb with tactile scale and light gradient
 */
@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val isPressed = remember { mutableStateOf(false) }

    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val animatedFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toggleFraction"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed.value) 1.22f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "thumbScale"
    )

    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f)

    val trackBrush = if (checked) {
        Brush.horizontalGradient(
            listOf(activeTrackColor.copy(alpha = 0.85f), activeTrackColor)
        )
    } else {
        Brush.horizontalGradient(
            listOf(inactiveTrackColor, inactiveTrackColor)
        )
    }

    val trackRim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.50f else 0.80f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    val thumbBrush = Brush.verticalGradient(
        listOf(
            Color.White,
            Color(0xFFE2E8F0)
        )
    )

    val trackShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(trackShape)
            .background(trackBrush, trackShape)
            .border(width = 0.8.dp, brush = trackRim, shape = trackShape)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed.value = true
                            tryAwaitRelease()
                            isPressed.value = false
                        },
                        onTap = {
                            onCheckedChange(!checked)
                        }
                    )
                }
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)
        val currentOffset = maxOffset * animatedFraction

        Box(
            modifier = Modifier
                .offset(x = currentOffset)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbBrush, CircleShape)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Premium Apple-style Liquid Glass Slider:
 * - Fluid translucent glass track with inner glowing progress
 * - Liquid glass thumb with elastic spring dynamics
 */
@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    var isDragging by remember { mutableStateOf(false) }
    val currentValueRange by rememberUpdatedState(valueRange)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    val thumbRadiusPx = with(LocalDensity.current) { 13.dp.toPx() }

    fun updateValueAt(positionX: Float, widthPx: Float) {
        val range = currentValueRange
        val usableWidthPx = (widthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)
        var fraction = ((positionX - thumbRadiusPx) / usableWidthPx).coerceIn(0f, 1f)
        if (steps > 0) {
            val intervals = steps + 1
            fraction = (fraction * intervals).roundToInt().toFloat() / intervals
        }
        currentOnValueChange(range.start + fraction * (range.endInclusive - range.start))
    }

    val interactionModifier = Modifier
        .pointerInput(enabled, valueRange, steps, thumbRadiusPx) {
            if (enabled) {
                detectTapGestures { position ->
                    updateValueAt(position.x, size.width.toFloat())
                }
            }
        }
        .pointerInput(enabled, valueRange, steps, thumbRadiusPx) {
            if (enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { position ->
                        isDragging = true
                        updateValueAt(position.x, size.width.toFloat())
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        updateValueAt(change.position.x, size.width.toFloat())
                    }
                )
            }
        }

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sliderThumbScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(interactionModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val trackHeight = 10.dp
        val thumbRadius = 13.dp
        val usableWidth = totalWidth - (thumbRadius * 2)

        val trackShape = RoundedCornerShape(50)
        val inactiveTrackBrush = Brush.verticalGradient(
            if (dark) listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f))
            else listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.04f))
        )
        val activeProgressBrush = Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.primary
            )
        )

        // Track Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(trackShape)
                .background(inactiveTrackBrush, trackShape)
                .border(
                    0.6.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    trackShape
                )
        ) {
            // Filled Progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(usableWidth * normalizedValue + thumbRadius)
                    .clip(trackShape)
                    .background(activeProgressBrush, trackShape)
            )
        }

        // Draggable Thumb
        val thumbOffset = usableWidth * normalizedValue

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbRadius * 2)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
                    CircleShape
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.3f))),
                    CircleShape
                )
        )
    }
}

