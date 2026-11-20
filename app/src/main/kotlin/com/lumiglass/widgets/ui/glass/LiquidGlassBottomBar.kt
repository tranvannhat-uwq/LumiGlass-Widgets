package com.lumiglass.widgets.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data representation for a navigation destination item in LiquidGlassBottomBar.
 */
data class LiquidNavDestinationItem<T>(
    val destination: T,
    val labelVi: String,
    val icon: ImageVector
)

/**
 * Premium Apple-style Floating Liquid Glass Navigation Bar:
 * - Floats gracefully above the scrolling content
 * - Translucent frosted acrylic glass with directional specular highlight rim
 * - Sliding spring pill indicator with directional light gradient
 * - Auto-adapting light/dark contrast and crisp iconography
 * - 100% native Compose implementation (zero external library crash risk)
 */
@Composable
fun <T> LiquidGlassBottomBar(
    items: List<LiquidNavDestinationItem<T>>,
    selectedDestination: T,
    onSelectDestination: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val barShape = RoundedCornerShape(32.dp)
    val selectedIndex = items.indexOfFirst { it.destination == selectedDestination }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(68.dp)
            .liquidGlassSurface(
                shape = barShape,
                blurRadius = 24f,
                tintColor = if (dark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.20f),
                rimWidth = 1.dp,
                elevation = 14.dp,
                isDarkTheme = dark
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val tabWidth = totalWidth / itemCount

            // Smooth sliding indicator driven by a critical damping spring
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorOffset"
            )

            // 1. Sliding Material Chip Indicator
            val indicatorShape = RoundedCornerShape(26.dp)
            val indicatorBg = if (dark) {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            }
            val indicatorRim = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.15f)
                )
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(indicatorShape)
                    .background(indicatorBg, indicatorShape)
                    .border(width = 0.8.dp, brush = indicatorRim, shape = indicatorShape)
            )

            // 2. Interactive Navigation Tabs
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabScale"
                    )

                    val textAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.70f,
                        label = "tabTextAlpha"
                    )

                    val itemColor = if (isSelected) {
                        if (dark) Color.White else MaterialTheme.colorScheme.onPrimary
                    } else {
                        if (dark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelectDestination(item.destination)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.labelVi,
                            tint = itemColor,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.labelVi,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemColor.copy(alpha = textAlpha)
                        )
                    }
                }
            }
        }
    }
}
