package com.lumiglass.widgets.ui.components

import android.appwidget.AppWidgetManager
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints
import com.lumiglass.widgets.model.WidgetSlotDefinition
import com.lumiglass.widgets.renderer.WidgetRemoteViewsRenderer
import kotlin.math.roundToInt

enum class SimulatedWallpaper(
    val labelVi: String,
    val isDark: Boolean,
    val gradientColors: List<Color>,
    val orbColor1: Color,
    val orbColor2: Color
) {
    AURORA_COLORFUL(
        labelVi = "Nhiều màu",
        isDark = true,
        gradientColors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B),
            Color(0xFF042F2E)
        ),
        orbColor1 = Color(0xFF38BDF8),
        orbColor2 = Color(0xFFC084FC)
    ),
    DAYLIGHT_BRIGHT(
        labelVi = "Nền sáng",
        isDark = false,
        gradientColors = listOf(
            Color(0xFFE0F2FE),
            Color(0xFFF1F5F9),
            Color(0xFFEDE9FE)
        ),
        orbColor1 = Color(0xFF7DD3FC),
        orbColor2 = Color(0xFFFDE68A)
    ),
    MIDNIGHT_DARK(
        labelVi = "Nền tối",
        isDark = true,
        gradientColors = listOf(
            Color(0xFF020617),
            Color(0xFF0F172A),
            Color(0xFF1E293B)
        ),
        orbColor1 = Color(0xFF1D4ED8),
        orbColor2 = Color(0xFF0F766E)
    ),
    SUNSET_WARM(
        labelVi = "Hoàng hôn",
        isDark = true,
        gradientColors = listOf(
            Color(0xFF31102F),
            Color(0xFF7C2D12),
            Color(0xFF1E1B4B)
        ),
        orbColor1 = Color(0xFFFB923C),
        orbColor2 = Color(0xFFF43F5E)
    )
}

/**
 * Interactive Wallpaper Stage + Real-time Liquid Glass Widget Preview.
 * Uses the same RemoteViews as the home screen widget.
 */
@Composable
fun WallpaperStageWidgetPreview(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    wallpaper: SimulatedWallpaper,
    onWallpaperChange: (SimulatedWallpaper) -> Unit,
    onSizeTierChange: ((WidgetSizeTier) -> Unit)? = null,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tier = config.preferredSizeTier
    val effectiveSnapshot = snapshot.copy(isSystemDarkTheme = wallpaper.isDark)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        val baseScale = if (availableWidth < 348.dp) {
            ((availableWidth - 20.dp) / 328.dp).coerceIn(0.84f, 1.0f)
        } else {
            1.0f
        }
        val previewScale = if (isCompact) {
            when (tier) {
                WidgetSizeTier.DASHBOARD_4X4 -> baseScale * 0.72f
                WidgetSizeTier.PREMIUM_4X3 -> baseScale * 0.84f
                WidgetSizeTier.ESSENTIAL_4X2 -> baseScale * 0.90f
                else -> baseScale * 0.94f
            }
        } else {
            baseScale
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wallpaper Stage Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (isCompact) 20.dp else 24.dp))
                    .background(Brush.linearGradient(wallpaper.gradientColors))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(if (isCompact) 20.dp else 24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = if (isCompact) 8.dp else 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Decorative background orbs to demonstrate glass translucency & edge refraction
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                wallpaper.orbColor1.copy(alpha = 0.50f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.25f, size.height * 0.28f),
                            radius = size.minDimension * 0.55f
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.28f),
                        radius = size.minDimension * 0.55f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                wallpaper.orbColor2.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.76f, size.height * 0.72f),
                            radius = size.minDimension * 0.52f
                        ),
                        center = Offset(size.width * 0.76f, size.height * 0.72f),
                        radius = size.minDimension * 0.52f
                    )
                }

                LiveWidgetGlassCard(
                    config = config,
                    snapshot = effectiveSnapshot,
                    tier = tier,
                    previewScale = previewScale
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Unified Compact Toolbar: Left Size Pills + Right Circular Wallpaper Swatch Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onSizeTierChange != null) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WidgetSizeTier.entries.forEach { itemTier ->
                            val isSelected = itemTier == tier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    )
                                    .clickable { onSizeTierChange(itemTier) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = itemTier.gridLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Text(
                        text = "Nền thử:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Compact circular wallpaper dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimulatedWallpaper.entries.forEach { wp ->
                        val selected = wp == wallpaper
                        Box(
                            modifier = Modifier
                                .size(if (selected) 22.dp else 19.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(wp.gradientColors))
                                .border(
                                    width = if (selected) 2.dp else 0.8.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .clickable { onWallpaperChange(wp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWidgetGlassCard(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier = config.preferredSizeTier,
    previewScale: Float = 1f,
    modifier: Modifier = Modifier,
    interactiveSlotEditMode: Boolean = false,
    selectedSlotId: String? = null,
    onSlotClick: ((WidgetSlotDefinition) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // TextClock updates on its own. Don't rebuild the bitmaps for every clock tick.
    val renderData = snapshot.copy(
        previewTimePrimary = "", previewTimeHour = "", previewTimeMinute = "", previewAmPm = ""
    )
    val remoteViews = remember(config, renderData, tier, context, density, interactiveSlotEditMode) {
        WidgetRemoteViewsRenderer.buildRemoteViews(
            context = context,
            appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID,
            widthDp = tier.defaultWidthDp,
            heightDp = tier.defaultHeightDp,
            config = config.copy(preferredSizeTier = tier, useCustomSlots = config.useCustomSlots || interactiveSlotEditMode),
            snapshot = renderData,
            preview = true,
            showEmptySlots = interactiveSlotEditMode
        )
    }
    val selectionColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier.layout { measurable, constraints ->
            val fullWidth = tier.defaultWidthDp.dp.roundToPx()
            val fullHeight = tier.defaultHeightDp.dp.roundToPx()
            val scale = minOf(previewScale, constraints.maxWidth.toFloat() / fullWidth,
                constraints.maxHeight.toFloat() / fullHeight)
            val placeable = measurable.measure(Constraints.fixed(fullWidth, fullHeight))
            layout(
                (fullWidth * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth),
                (fullHeight * scale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
            ) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        },
        factory = { FrameLayout(it) },
        update = { host ->
            if (host.tag !== remoteViews) {
                val existing = host.getChildAt(0)
                val layoutMatches = existing?.tag == remoteViews.layoutId
                if (existing != null && layoutMatches) {
                    remoteViews.reapply(host.context, existing)
                } else {
                    host.removeAllViews()
                    val child = remoteViews.apply(host.context, host).apply { tag = remoteViews.layoutId }
                    host.addView(child, FrameLayout.LayoutParams(-1, -1))
                }
                host.tag = remoteViews
            }
            if (interactiveSlotEditMode) {
                WidgetSlotBlueprints.slotsForTier(tier).forEachIndexed { index, slot ->
                    val cell = host.findViewById<View>(WidgetRemoteViewsRenderer.customSlotContainerId(index))
                    val content = WidgetSlotBlueprints.slotContent(config, slot.slotId)
                    cell.contentDescription = "${slot.titleVi}: ${content.shortLabelVi}"
                    cell.isSelected = selectedSlotId == slot.slotId
                    cell.foreground = if (cell.isSelected) GradientDrawable().apply {
                        cornerRadius = with(density) { 12.dp.toPx() }
                        setStroke(with(density) { 2.dp.roundToPx() }, selectionColor)
                    } else null
                    cell.setOnClickListener { onSlotClick?.invoke(slot) }
                }
            }
        }
    )
}
