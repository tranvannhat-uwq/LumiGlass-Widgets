package com.lumiglass.widgets.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lumiglass.widgets.model.BatteryDisplayStyle
import com.lumiglass.widgets.model.ClockFontStyle
import com.lumiglass.widgets.model.ContentAlignmentOption
import com.lumiglass.widgets.model.FontWeightOption
import com.lumiglass.widgets.model.SlotContentType
import com.lumiglass.widgets.model.TelemetryDisplayStyle
import com.lumiglass.widgets.model.TemperatureSymbolStyle
import com.lumiglass.widgets.model.WeatherDisplayStyle
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetModuleType
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints
import com.lumiglass.widgets.model.WidgetSlotDefinition
import com.lumiglass.widgets.renderer.CustomSlotRenderer
import com.lumiglass.widgets.renderer.GlassIconRenderer
import com.lumiglass.widgets.renderer.GlassSurfaceRenderer

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
 * Uses the exact same [GlassSurfaceRenderer] and [GlassIconRenderer] as the Home Screen AppWidget.
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
        // True 1:1 scale with actual home screen launcher widget
        val availableWidth = maxWidth
        val previewScale = if (availableWidth < 348.dp) {
            ((availableWidth - 20.dp) / 328.dp).coerceIn(0.84f, 1.0f)
        } else {
            1.0f
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wallpaper Stage Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(wallpaper.gradientColors))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Wallpaper Simulator Selector Chips (Always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimulatedWallpaper.entries.forEach { wp ->
                    val selected = wp == wallpaper
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                            .border(
                                width = if (selected) 1.5.dp else 0.5.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onWallpaperChange(wp) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(wp.gradientColors))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = wp.labelVi,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Size Tier Switcher (Always visible when onSizeTierChange is provided)
            if (onSizeTierChange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WidgetSizeTier.entries.forEach { itemTier ->
                        val isSelected = itemTier == tier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable { onSizeTierChange(itemTier) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemTier.gridLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the Liquid Glass Widget card at the requested [WidgetSizeTier] dimensions.
 */
@Composable
fun LiveWidgetGlassCard(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier = config.preferredSizeTier,
    previewScale: Float = 1.0f,
    modifier: Modifier = Modifier,
    interactiveSlotEditMode: Boolean = false,
    selectedSlotId: String? = null,
    onSlotClick: ((WidgetSlotDefinition) -> Unit)? = null
) {
    val palette = GlassSurfaceRenderer.resolvePalette(config, snapshot.isSystemDarkTheme)
    val primaryColor = Color(palette.primaryTextArgb)
    val secondaryColor = Color(palette.secondaryTextArgb)
    val accentColor = Color(palette.accentTextArgb)

    val renderAsCustomSlots = config.useCustomSlots || interactiveSlotEditMode

    val (cardWidthDp, cardHeightDp) = if (interactiveSlotEditMode) {
        when (tier) {
            WidgetSizeTier.MINI_2X1 -> (230 * previewScale).dp to (96 * previewScale).dp
            WidgetSizeTier.COMPACT_2X2 -> (204 * previewScale).dp to (204 * previewScale).dp
            WidgetSizeTier.ESSENTIAL_4X2 -> (336 * previewScale).dp to (172 * previewScale).dp
            WidgetSizeTier.PREMIUM_4X3 -> (336 * previewScale).dp to (242 * previewScale).dp
            WidgetSizeTier.DASHBOARD_4X4 -> (336 * previewScale).dp to (308 * previewScale).dp
        }
    } else {
        when (tier) {
            WidgetSizeTier.MINI_2X1 -> (160 * previewScale).dp to (72 * previewScale).dp
            WidgetSizeTier.COMPACT_2X2 -> (160 * previewScale).dp to (160 * previewScale).dp
            WidgetSizeTier.ESSENTIAL_4X2 -> {
                val h4x2 = if (config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS || renderAsCustomSlots) 166 else 144
                (328 * previewScale).dp to (h4x2 * previewScale).dp
            }
            WidgetSizeTier.PREMIUM_4X3 -> (328 * previewScale).dp to (218 * previewScale).dp
            WidgetSizeTier.DASHBOARD_4X4 -> (328 * previewScale).dp to (288 * previewScale).dp
        }
    }

    val hasRightContent = config.showWeather || config.showLunarDate ||
        (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp))

    val dividerLayout = if (renderAsCustomSlots) {
        GlassSurfaceRenderer.DividerZoneLayout.NONE
    } else {
        when (tier) {
            WidgetSizeTier.ESSENTIAL_4X2 -> {
                if ((config.showClock || config.showGregorianDate) && hasRightContent) {
                    GlassSurfaceRenderer.DividerZoneLayout.VERTICAL_SPLIT_4X2
                } else GlassSurfaceRenderer.DividerZoneLayout.NONE
            }
            WidgetSizeTier.PREMIUM_4X3 -> {
                if (config.showWeather || (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp))) {
                    GlassSurfaceRenderer.DividerZoneLayout.PREMIUM_4X3
                } else GlassSurfaceRenderer.DividerZoneLayout.NONE
            }
            WidgetSizeTier.DASHBOARD_4X4 -> GlassSurfaceRenderer.DividerZoneLayout.DASHBOARD_4X4
            else -> GlassSurfaceRenderer.DividerZoneLayout.NONE
        }
    }

    val horizontalAlign = when (config.contentAlignment) {
        ContentAlignmentOption.START -> Alignment.Start
        ContentAlignmentOption.CENTER -> Alignment.CenterHorizontally
        ContentAlignmentOption.END -> Alignment.End
    }
    val textAlign = when (config.contentAlignment) {
        ContentAlignmentOption.START -> TextAlign.Start
        ContentAlignmentOption.CENTER -> TextAlign.Center
        ContentAlignmentOption.END -> TextAlign.End
    }

    Box(
        modifier = modifier.size(width = cardWidthDp, height = cardHeightDp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Procedural Liquid Glass Canvas Surface (Exact same engine as AppWidget RemoteViews)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                GlassSurfaceRenderer.drawGlassSurfaceOnCanvas(
                    canvas = composeCanvas.nativeCanvas,
                    widthPx = size.width,
                    heightPx = size.height,
                    density = density,
                    config = config,
                    isSystemDarkTheme = snapshot.isSystemDarkTheme,
                    dividerLayout = dividerLayout
                )
            }
        }

        if (renderAsCustomSlots) {
            LiveCustomSlotsGridOverlay(
                config = config,
                snapshot = snapshot,
                tier = tier,
                previewScale = previewScale,
                interactiveEditMode = interactiveSlotEditMode,
                selectedSlotId = selectedSlotId,
                onSlotClick = onSlotClick
            )
            return@Box
        }

        // 2. Content Layer Sitting Directly on the Liquid Glass Surface
        val pad = (when (tier) {
            WidgetSizeTier.MINI_2X1 -> 12
            WidgetSizeTier.COMPACT_2X2 -> 15
            WidgetSizeTier.ESSENTIAL_4X2 -> 16
            WidgetSizeTier.PREMIUM_4X3 -> 18
            WidgetSizeTier.DASHBOARD_4X4 -> 20
        } * previewScale).dp

        when (tier) {
            WidgetSizeTier.MINI_2X1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((10 * previewScale).dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = horizontalAlign
                ) {
                    if (config.showClock) {
                        PreviewClockText(
                            config = config,
                            snapshot = snapshot,
                            baseSp = 26f * previewScale,
                            primaryColor = primaryColor,
                            accentColor = accentColor,
                            paletteShadowArgb = palette.shadowTextArgb,
                            horizontalSplit = true
                        )
                    }
                    PreviewDateAndLunarLines(
                        config = config,
                        snapshot = snapshot,
                        tier = tier,
                        dateSp = 11f * previewScale,
                        lunarSp = 10f * previewScale,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor,
                        paletteShadowArgb = palette.shadowTextArgb,
                        textAlign = textAlign
                    )
                }
            }

            WidgetSizeTier.COMPACT_2X2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((12 * previewScale).dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = horizontalAlign
                ) {
                    if (config.showClock) {
                        PreviewClockText(
                            config = config,
                            snapshot = snapshot,
                            baseSp = 34f * previewScale,
                            primaryColor = primaryColor,
                            accentColor = accentColor,
                            paletteShadowArgb = palette.shadowTextArgb,
                            horizontalSplit = false
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    PreviewDateAndLunarLines(
                        config = config,
                        snapshot = snapshot,
                        tier = tier,
                        dateSp = 12f * previewScale,
                        lunarSp = 10.8f * previewScale,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor,
                        paletteShadowArgb = palette.shadowTextArgb,
                        textAlign = textAlign
                    )
                    if (config.showWeather) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PreviewWeatherCluster(
                            config = config,
                            snapshot = snapshot,
                            iconSizeDp = (20 * previewScale).dp,
                            tempSp = 12.5f * previewScale,
                            condSp = 10.5f * previewScale,
                            showDetails = false,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            palettePrimaryArgb = palette.primaryTextArgb,
                            paletteShadowArgb = palette.shadowTextArgb
                        )
                    }
                    if (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PreviewBatteryCluster(
                            config = config,
                            snapshot = snapshot,
                            tier = tier,
                            textSp = 10.5f * previewScale,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            palettePrimaryArgb = palette.primaryTextArgb,
                            paletteShadowArgb = palette.shadowTextArgb
                        )
                    }
                }
            }

            WidgetSizeTier.ESSENTIAL_4X2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = (14 * previewScale).dp,
                            vertical = (8 * previewScale).dp
                        )
                ) {
                    // Upper 62%: Left Clock+Date | Right Weather+Lunar+Battery (Moved UP)
                    Row(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left 48%: Clock + Gregorian Date
                        Column(
                            modifier = Modifier
                                .weight(0.48f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (config.showClock) {
                                Box(modifier = Modifier.offset(y = config.clockOffsetY.dp)) {
                                    PreviewClockText(
                                        config = config,
                                        snapshot = snapshot,
                                        baseSp = 35f * previewScale,
                                        primaryColor = primaryColor,
                                        accentColor = accentColor,
                                        paletteShadowArgb = palette.shadowTextArgb,
                                        horizontalSplit = true
                                    )
                                }
                            }
                            if (config.showGregorianDate) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(modifier = Modifier.offset(y = config.dateOffsetY.dp)) {
                                    Text(
                                        text = snapshot.gregorianDateText,
                                        style = buildGlassTextStyle(
                                            config = config,
                                            fontSize = (11.8f * previewScale * config.textScale * config.dateScale).sp,
                                            color = secondaryColor,
                                            shadowArgb = palette.shadowTextArgb,
                                            isHeader = false
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Right 52%: Weather + Lunar + Battery
                        Column(
                            modifier = Modifier
                                .weight(0.52f)
                                .padding(start = 6.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (config.showWeather) {
                                Box(modifier = Modifier.offset(y = config.weatherOffsetY.dp)) {
                                    PreviewWeatherCluster(
                                        config = config,
                                        snapshot = snapshot,
                                        iconSizeDp = (26 * previewScale).dp,
                                        tempSp = 15.5f * previewScale,
                                        condSp = 10.8f * previewScale,
                                        showDetails = true,
                                        primaryColor = primaryColor,
                                        secondaryColor = secondaryColor,
                                        palettePrimaryArgb = palette.primaryTextArgb,
                                        paletteShadowArgb = palette.shadowTextArgb
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            if (config.showLunarDate && !config.showWeekCalendarStrip) {
                                Text(
                                    text = snapshot.lunarDateInfo.formatDisplay(
                                        showCanChi = config.showCanChi,
                                        showHolidays = config.showLunarHolidays
                                    ),
                                    style = buildGlassTextStyle(
                                        config = config,
                                        fontSize = (10.8f * previewScale * config.textScale).sp,
                                        color = accentColor,
                                        shadowArgb = palette.shadowTextArgb,
                                        isHeader = false
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            if (config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS) {
                                Spacer(modifier = Modifier.height(3.dp))
                                PreviewTelemetryTrioCluster(
                                    config = config,
                                    snapshot = snapshot,
                                    tier = tier,
                                    previewScale = previewScale
                                )
                            } else {
                                if (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp)) {
                                    Box(modifier = Modifier.offset(y = config.batteryOffsetY.dp)) {
                                        PreviewBatteryCluster(
                                            config = config,
                                            snapshot = snapshot,
                                            tier = tier,
                                            textSp = 11.0f * previewScale,
                                            primaryColor = primaryColor,
                                            secondaryColor = secondaryColor,
                                            palettePrimaryArgb = palette.primaryTextArgb,
                                            paletteShadowArgb = palette.shadowTextArgb
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom 38%: 5-Day or 7-Day Solar & Lunar Calendar Strip
                    if (config.showWeekCalendarStrip) {
                        Box(
                            modifier = Modifier
                                .weight(0.38f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PreviewWeekCalendarStrip(
                                config = config,
                                snapshot = snapshot,
                                previewScale = previewScale,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                paletteShadowArgb = palette.shadowTextArgb
                            )
                        }
                    }
                }
            }

            WidgetSizeTier.PREMIUM_4X3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = (16 * previewScale).dp,
                            vertical = (10 * previewScale).dp
                        )
                ) {
                    // Upper 66%: Left Clock+Date | Right Weather+Lunar+Battery
                    Row(
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.48f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (config.showClock) {
                                Box(modifier = Modifier.offset(y = config.clockOffsetY.dp)) {
                                    PreviewClockText(
                                        config = config,
                                        snapshot = snapshot,
                                        baseSp = 38f * previewScale,
                                        primaryColor = primaryColor,
                                        accentColor = accentColor,
                                        paletteShadowArgb = palette.shadowTextArgb,
                                        horizontalSplit = true
                                    )
                                }
                            }
                            if (config.showGregorianDate) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.offset(y = config.dateOffsetY.dp)) {
                                    Text(
                                        text = snapshot.gregorianDateText,
                                        style = buildGlassTextStyle(
                                            config = config,
                                            fontSize = (13f * previewScale * config.textScale * config.dateScale).sp,
                                            color = secondaryColor,
                                            shadowArgb = palette.shadowTextArgb,
                                            isHeader = false
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.52f)
                                .padding(start = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (config.showWeather) {
                                Box(modifier = Modifier.offset(y = config.weatherOffsetY.dp)) {
                                    PreviewWeatherCluster(
                                        config = config,
                                        snapshot = snapshot,
                                        iconSizeDp = (32 * previewScale).dp,
                                        tempSp = 17f * previewScale,
                                        condSp = 11.5f * previewScale,
                                        showDetails = true,
                                        primaryColor = primaryColor,
                                        secondaryColor = secondaryColor,
                                        palettePrimaryArgb = palette.primaryTextArgb,
                                        paletteShadowArgb = palette.shadowTextArgb
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (config.showLunarDate && !config.showWeekCalendarStrip) {
                                Text(
                                    text = snapshot.lunarDateInfo.formatDisplay(
                                        showCanChi = config.showCanChi,
                                        showHolidays = config.showLunarHolidays
                                    ),
                                    style = buildGlassTextStyle(
                                        config = config,
                                        fontSize = (11.8f * previewScale * config.textScale).sp,
                                        color = accentColor,
                                        shadowArgb = palette.shadowTextArgb,
                                        isHeader = false
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS) {
                                Spacer(modifier = Modifier.height(4.dp))
                                PreviewTelemetryTrioCluster(
                                    config = config,
                                    snapshot = snapshot,
                                    tier = tier,
                                    previewScale = previewScale
                                )
                            } else {
                                if (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp)) {
                                    Box(modifier = Modifier.offset(y = config.batteryOffsetY.dp)) {
                                        PreviewBatteryCluster(
                                            config = config,
                                            snapshot = snapshot,
                                            tier = tier,
                                            textSp = 12.5f * previewScale * config.batteryScale,
                                            primaryColor = primaryColor,
                                            secondaryColor = secondaryColor,
                                            palettePrimaryArgb = palette.primaryTextArgb,
                                            paletteShadowArgb = palette.shadowTextArgb
                                        )
                                    }
                                }
                                if (config.showRamStatus) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(modifier = Modifier.offset(y = config.ramOffsetY.dp)) {
                                        PreviewRamCluster(
                                            config = config,
                                            snapshot = snapshot,
                                            tier = tier,
                                            previewScale = previewScale,
                                            secondaryColor = secondaryColor,
                                            palette = palette
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Lower 34%: 5-Day or 7-Day Solar & Lunar Calendar Strip
                    if (config.showWeekCalendarStrip) {
                        Box(
                            modifier = Modifier
                                .weight(0.34f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PreviewWeekCalendarStrip(
                                config = config,
                                snapshot = snapshot,
                                previewScale = previewScale,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                paletteShadowArgb = palette.shadowTextArgb
                            )
                        }
                    }
                }
            }

            WidgetSizeTier.DASHBOARD_4X4 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = (18 * previewScale).dp,
                            vertical = (12 * previewScale).dp
                        )
                ) {
                    // Zone 1 (34%): Clock + Date + Lunar
                    Column(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = horizontalAlign
                    ) {
                        if (config.showClock) {
                            Box(modifier = Modifier.offset(y = config.clockOffsetY.dp)) {
                                PreviewClockText(
                                    config = config,
                                    snapshot = snapshot,
                                    baseSp = 40f * previewScale,
                                    primaryColor = primaryColor,
                                    accentColor = accentColor,
                                    paletteShadowArgb = palette.shadowTextArgb,
                                    horizontalSplit = true
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.offset(y = config.dateOffsetY.dp)) {
                            PreviewDateAndLunarLines(
                                config = config,
                                snapshot = snapshot,
                                tier = tier,
                                dateSp = 13.5f * previewScale,
                                lunarSp = 11.5f * previewScale,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                paletteShadowArgb = palette.shadowTextArgb,
                                textAlign = textAlign
                            )
                        }
                    }

                    // Zone 2 (34%): Weather Station + Battery Telemetry + RAM
                    Column(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (config.showWeather) {
                            Box(modifier = Modifier.offset(y = config.weatherOffsetY.dp)) {
                                PreviewWeatherCluster(
                                    config = config,
                                    snapshot = snapshot,
                                    iconSizeDp = (34 * previewScale).dp,
                                    tempSp = 19f * previewScale,
                                    condSp = 13f * previewScale,
                                    showDetails = true,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    palettePrimaryArgb = palette.primaryTextArgb,
                                    paletteShadowArgb = palette.shadowTextArgb
                                )
                            }
                        }
                        if (config.telemetryDisplayStyle == TelemetryDisplayStyle.TRIO_GLASS_CARDS) {
                            Spacer(modifier = Modifier.height(5.dp))
                            PreviewTelemetryTrioCluster(
                                config = config,
                                snapshot = snapshot,
                                tier = tier,
                                previewScale = previewScale
                            )
                        } else {
                            if (config.batteryMonitoringEnabled && (config.showBatteryPercent || config.showBatteryTemp)) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(modifier = Modifier.offset(y = config.batteryOffsetY.dp)) {
                                    PreviewBatteryCluster(
                                        config = config,
                                        snapshot = snapshot,
                                        tier = tier,
                                        textSp = 13f * previewScale * config.batteryScale,
                                        primaryColor = primaryColor,
                                        secondaryColor = secondaryColor,
                                        palettePrimaryArgb = palette.primaryTextArgb,
                                        paletteShadowArgb = palette.shadowTextArgb,
                                        expandFullRow = true
                                    )
                                }
                            }
                            if (config.showRamStatus) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.offset(y = config.ramOffsetY.dp)) {
                                    PreviewRamCluster(
                                        config = config,
                                        snapshot = snapshot,
                                        tier = tier,
                                        previewScale = previewScale,
                                        secondaryColor = secondaryColor,
                                        palette = palette
                                    )
                                }
                            }
                        }
                    }

                    // Zone 3 (30%): 5-Day or 7-Day Solar & Lunar Calendar Strip
                    if (config.showWeekCalendarStrip) {
                        Box(
                            modifier = Modifier
                                .weight(0.30f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PreviewWeekCalendarStrip(
                                config = config,
                                snapshot = snapshot,
                                previewScale = previewScale,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                paletteShadowArgb = palette.shadowTextArgb
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewClockText(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    baseSp: Float,
    primaryColor: Color,
    accentColor: Color,
    paletteShadowArgb: Int,
    horizontalSplit: Boolean
) {
    val secFactor = if (config.showSeconds) 0.78f else 1.0f
    val effSp = (baseSp * config.textScale * config.clockScale * secFactor).coerceIn(14f, 72f)

    if (!config.splitClockTwoLines) {
        val display = buildString {
            append(snapshot.previewTimePrimary)
            if (!config.use24HourFormat && snapshot.previewAmPm.isNotBlank()) {
                append(" ")
                append(snapshot.previewAmPm)
            }
        }
        Text(
            text = display,
            style = buildGlassTextStyle(
                config = config,
                fontSize = effSp.sp,
                color = primaryColor,
                shadowArgb = paletteShadowArgb,
                isHeader = true
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    } else if (horizontalSplit) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = snapshot.previewTimeHour,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (effSp * 0.92f).sp,
                    color = primaryColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = true
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = snapshot.previewTimeMinute,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (effSp * 0.92f).sp,
                    color = accentColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = true
                )
            )
        }
    } else {
        Column {
            Text(
                text = snapshot.previewTimeHour,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (effSp * 0.86f).sp,
                    color = primaryColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = true
                )
            )
            Text(
                text = snapshot.previewTimeMinute,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (effSp * 0.86f).sp,
                    color = accentColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = true
                )
            )
        }
    }
}

@Composable
private fun PreviewDateAndLunarLines(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier,
    dateSp: Float,
    lunarSp: Float,
    secondaryColor: Color,
    accentColor: Color,
    paletteShadowArgb: Int,
    textAlign: TextAlign
) {
    val lunarText = when (tier) {
        WidgetSizeTier.MINI_2X1, WidgetSizeTier.COMPACT_2X2 ->
            snapshot.lunarDateInfo.formatCompact(config.showCanChi)
        else ->
            snapshot.lunarDateInfo.formatDisplay(config.showCanChi, config.showLunarHolidays)
    }

    val dateIdx = config.moduleOrder.indexOf(WidgetModuleType.GREGORIAN_DATE)
    val lunarIdx = config.moduleOrder.indexOf(WidgetModuleType.LUNAR_DATE)
    val lunarFirst = lunarIdx in 0 until dateIdx

    if (!lunarFirst) {
        if (config.showGregorianDate) {
            Text(
                text = snapshot.gregorianDateText,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (dateSp * config.textScale * config.dateScale).sp,
                    color = secondaryColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = false
                ),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (config.showLunarDate) {
            Text(
                text = lunarText,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (lunarSp * config.textScale * config.dateScale).sp,
                    color = accentColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = false
                ),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        if (config.showLunarDate) {
            Text(
                text = lunarText,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (dateSp * config.textScale * config.dateScale).sp,
                    color = accentColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = false
                ),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (config.showGregorianDate) {
            Text(
                text = snapshot.gregorianDateText,
                style = buildGlassTextStyle(
                    config = config,
                    fontSize = (lunarSp * config.textScale * config.dateScale).sp,
                    color = secondaryColor,
                    shadowArgb = paletteShadowArgb,
                    isHeader = false
                ),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreviewWeatherCluster(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    iconSizeDp: Dp,
    tempSp: Float,
    condSp: Float,
    showDetails: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    palettePrimaryArgb: Int,
    paletteShadowArgb: Int
) {
    val weather = snapshot.weatherInfo
    val scaledIconDp = (iconSizeDp.value * config.iconScale * config.weatherScale).dp
    val degreeOnly = config.temperatureSymbolStyle == TemperatureSymbolStyle.DEGREE_ONLY
    val isDark = snapshot.isSystemDarkTheme

    when (config.weatherDisplayStyle) {
        WeatherDisplayStyle.MAGIC_CARD -> {
            val cardShape = RoundedCornerShape(14.dp)
            val cardModifier = if (config.showWeatherCardBackground) {
                Modifier
                    .clip(cardShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.40f),
                        cardShape
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.45f else 0.70f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = cardShape
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            } else Modifier

            Box(modifier = cardModifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(scaledIconDp)) {
                        drawIntoCanvas { c ->
                            GlassIconRenderer.drawWeatherIcon(
                                canvas = c.nativeCanvas,
                                sizePx = size.minDimension,
                                category = weather.conditionCategory(),
                                style = config.iconStyle,
                                primaryTextArgb = palettePrimaryArgb
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        // Line 1: Weather Condition ("Nhiều mây") + Current Temp ("15°")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = weather.conditionTextVi,
                                style = buildGlassTextStyle(
                                    config = config,
                                    fontSize = (condSp * 1.25f * config.textScale * config.weatherScale).sp,
                                    color = primaryColor,
                                    shadowArgb = paletteShadowArgb,
                                    isHeader = true
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = weather.formatTemp(weather.currentTempC, config.temperatureUnit, degreeOnly = degreeOnly),
                                style = buildGlassTextStyle(
                                    config = config,
                                    fontSize = (tempSp * 1.05f * config.textScale * config.weatherScale).sp,
                                    color = primaryColor,
                                    shadowArgb = paletteShadowArgb,
                                    isHeader = true
                                )
                            )
                        }
                        // Line 2: Temperature Range & Rain Probability ("14°–22° • Mưa 0%")
                        if (showDetails) {
                            val rangeDetails = weather.formatRangeAndPrecip(config.temperatureUnit, degreeOnly = degreeOnly)
                            Text(
                                text = rangeDetails,
                                style = buildGlassTextStyle(
                                    config = config,
                                    fontSize = (condSp * 0.95f * config.textScale * config.weatherScale).sp,
                                    color = secondaryColor.copy(alpha = 0.88f),
                                    shadowArgb = paletteShadowArgb,
                                    isHeader = false
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        WeatherDisplayStyle.COMPACT_RANGE -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(scaledIconDp)) {
                    drawIntoCanvas { c ->
                        GlassIconRenderer.drawWeatherIcon(
                            canvas = c.nativeCanvas,
                            sizePx = size.minDimension,
                            category = weather.conditionCategory(),
                            style = config.iconStyle,
                            primaryTextArgb = palettePrimaryArgb
                        )
                    }
                }
                Spacer(modifier = Modifier.width(7.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weather.formatTemp(weather.currentTempC, config.temperatureUnit, degreeOnly = degreeOnly),
                            style = buildGlassTextStyle(
                                config = config,
                                fontSize = (tempSp * config.textScale * config.weatherScale).sp,
                                color = primaryColor,
                                shadowArgb = paletteShadowArgb,
                                isHeader = true
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = weather.conditionTextVi,
                            style = buildGlassTextStyle(
                                config = config,
                                fontSize = (condSp * config.textScale * config.weatherScale).sp,
                                color = secondaryColor,
                                shadowArgb = paletteShadowArgb,
                                isHeader = false
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showDetails) {
                        Text(
                            text = weather.formatRangeCompact(config.temperatureUnit, degreeOnly = degreeOnly),
                            style = buildGlassTextStyle(
                                config = config,
                                fontSize = (condSp * 0.90f * config.textScale * config.weatherScale).sp,
                                color = secondaryColor.copy(alpha = 0.85f),
                                shadowArgb = paletteShadowArgb,
                                isHeader = false
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        WeatherDisplayStyle.CLASSIC_DETAILED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(scaledIconDp)) {
                    drawIntoCanvas { c ->
                        GlassIconRenderer.drawWeatherIcon(
                            canvas = c.nativeCanvas,
                            sizePx = size.minDimension,
                            category = weather.conditionCategory(),
                            style = config.iconStyle,
                            primaryTextArgb = palettePrimaryArgb
                        )
                    }
                }
                Spacer(modifier = Modifier.width(7.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weather.formatTemp(weather.currentTempC, config.temperatureUnit, degreeOnly = degreeOnly),
                            style = buildGlassTextStyle(
                                config = config,
                                fontSize = (tempSp * config.textScale * config.weatherScale).sp,
                                color = primaryColor,
                                shadowArgb = paletteShadowArgb,
                                isHeader = true
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (weather.hasData) "${weather.conditionTextVi} • ${weather.cityNameVi}"
                            else "${weather.cityNameVi} • Chưa có dữ liệu",
                            style = buildGlassTextStyle(
                                config = config,
                                fontSize = (condSp * config.textScale * config.weatherScale).sp,
                                color = secondaryColor,
                                shadowArgb = paletteShadowArgb,
                                isHeader = false
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showDetails) {
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
                        } else {
                            detailParts.add("Đang chờ kết nối Open-Meteo")
                        }
                        if (detailParts.isNotEmpty()) {
                            Text(
                                text = detailParts.joinToString(" • "),
                                style = buildGlassTextStyle(
                                    config = config,
                                    fontSize = ((condSp * 0.90f) * config.textScale * config.weatherScale).sp,
                                    color = secondaryColor.copy(alpha = 0.85f),
                                    shadowArgb = paletteShadowArgb,
                                    isHeader = false
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTelemetryTrioCluster(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier,
    previewScale: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val cardHeight = when (tier) {
        WidgetSizeTier.PREMIUM_4X3 -> (66 * previewScale).dp
        WidgetSizeTier.DASHBOARD_4X4 -> (72 * previewScale).dp
        else -> (50 * previewScale).dp
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (config.showBatteryCard && config.batteryMonitoringEnabled) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        GlassIconRenderer.drawBatteryTrioCard(
                            canvas = c.nativeCanvas,
                            w = size.width,
                            h = size.height,
                            battery = snapshot.batteryInfo,
                            config = config,
                            density = density
                        )
                    }
                }
            }
        }
        if (config.showStorageCard) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        GlassIconRenderer.drawStorageTrioCard(
                            canvas = c.nativeCanvas,
                            w = size.width,
                            h = size.height,
                            storage = snapshot.storageInfo,
                            config = config,
                            density = density
                        )
                    }
                }
            }
        }
        if (config.showRamCard && config.showRamStatus) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        GlassIconRenderer.drawRamTrioCard(
                            canvas = c.nativeCanvas,
                            w = size.width,
                            h = size.height,
                            ram = snapshot.ramInfo,
                            config = config,
                            density = density
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBatteryCluster(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier,
    textSp: Float,
    primaryColor: Color,
    secondaryColor: Color,
    palettePrimaryArgb: Int,
    paletteShadowArgb: Int,
    stackVertically: Boolean = false,
    expandFullRow: Boolean = false
) {
    val battery = snapshot.batteryInfo
    val (rawW, rawH) = when (config.batteryStyle) {
        BatteryDisplayStyle.PILL -> when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> 24.dp to 12.dp
            WidgetSizeTier.ESSENTIAL_4X2 -> 28.dp to 13.dp
            WidgetSizeTier.PREMIUM_4X3 -> 32.dp to 15.dp
            WidgetSizeTier.DASHBOARD_4X4 -> 34.dp to 16.dp
            else -> 28.dp to 13.dp
        }
        BatteryDisplayStyle.CIRCLE -> 20.dp to 20.dp
        BatteryDisplayStyle.PROGRESS_BAR -> 42.dp to 12.dp
        BatteryDisplayStyle.ICON_NUMBER -> 16.dp to 18.dp
    }
    val gaugeW = (rawW.value * config.batteryScale).dp
    val gaugeH = (rawH.value * config.batteryScale).dp
    val pctArgb = GlassIconRenderer.resolveBatteryLevelColorArgb(
        levelPercent = battery.levelPercent,
        isCharging = battery.isCharging,
        colorByLevel = config.colorBatteryByLevel,
        fallbackArgb = palettePrimaryArgb
    )
    val tempArgb = GlassIconRenderer.resolveBatteryTempColorArgb(
        tempCelsius = battery.temperatureCelsius,
        warnByColor = config.warnBatteryTempByColor,
        fallbackArgb = palettePrimaryArgb
    )

    val tempFormatted = battery.formatTemperature(config.temperatureUnit)
    val tempText = when (tier) {
        WidgetSizeTier.DASHBOARD_4X4, WidgetSizeTier.PREMIUM_4X3 -> "Nhiệt độ pin: $tempFormatted"
        else -> if (config.showBatteryPercent) "• Pin $tempFormatted" else "Pin: $tempFormatted"
    }

    if (stackVertically) {
        Column(horizontalAlignment = Alignment.End) {
            if (config.showBatteryPercent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(gaugeW, gaugeH)) {
                        drawIntoCanvas { c ->
                            GlassIconRenderer.drawBatteryGauge(
                                canvas = c.nativeCanvas,
                                widthPx = size.width,
                                heightPx = size.height,
                                batteryInfo = battery,
                                style = config.batteryStyle,
                                colorByLevel = config.colorBatteryByLevel,
                                primaryTextArgb = palettePrimaryArgb
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (if (battery.isCharging) "⚡ " else "") + battery.formatPercent(),
                        style = buildGlassTextStyle(
                            config = config,
                            fontSize = (textSp * config.textScale * config.batteryScale).sp,
                            color = Color(pctArgb),
                            shadowArgb = paletteShadowArgb,
                            isHeader = true
                        )
                    )
                }
            }
            if (config.showBatteryTemp) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tempText,
                    style = buildGlassTextStyle(
                        config = config,
                        fontSize = ((textSp * 0.90f) * config.textScale * config.batteryScale).sp,
                        color = if (config.warnBatteryTempByColor) Color(tempArgb) else secondaryColor,
                        shadowArgb = paletteShadowArgb,
                        isHeader = false
                    ),
                    maxLines = 1
                )
            }
        }
    } else {
        Row(
            modifier = if (expandFullRow) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expandFullRow) Arrangement.SpaceBetween else Arrangement.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (config.showBatteryPercent) {
                    Canvas(modifier = Modifier.size(gaugeW, gaugeH)) {
                        drawIntoCanvas { c ->
                            GlassIconRenderer.drawBatteryGauge(
                                canvas = c.nativeCanvas,
                                widthPx = size.width,
                                heightPx = size.height,
                                batteryInfo = battery,
                                style = config.batteryStyle,
                                colorByLevel = config.colorBatteryByLevel,
                                primaryTextArgb = palettePrimaryArgb
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (if (battery.isCharging) "⚡ " else "") + battery.formatPercent(),
                        style = buildGlassTextStyle(
                            config = config,
                            fontSize = (textSp * config.textScale * config.batteryScale).sp,
                            color = Color(pctArgb),
                            shadowArgb = paletteShadowArgb,
                            isHeader = true
                        )
                    )
                }
            }
            if (config.showBatteryTemp) {
                if (!expandFullRow && config.showBatteryPercent) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = tempText,
                    style = buildGlassTextStyle(
                        config = config,
                        fontSize = ((textSp * 0.92f) * config.textScale * config.batteryScale).sp,
                        color = if (config.warnBatteryTempByColor) Color(tempArgb) else secondaryColor,
                        shadowArgb = paletteShadowArgb,
                        isHeader = false
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun buildGlassTextStyle(
    config: WidgetConfig,
    fontSize: TextUnit,
    color: Color,
    shadowArgb: Int,
    isHeader: Boolean
): TextStyle {
    val fontFamily = when (config.fontStyle) {
        ClockFontStyle.ROUNDED_GEOMETRIC -> runCatching {
            FontFamily(android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.NORMAL))
        }.getOrElse { FontFamily.SansSerif }
        ClockFontStyle.MODERN_SANS -> FontFamily.SansSerif
        ClockFontStyle.THIN_ELEGANCE -> FontFamily.SansSerif
        ClockFontStyle.BOLD_GEOMETRIC -> FontFamily.SansSerif
        ClockFontStyle.CLASSIC_SERIF -> FontFamily.Serif
        ClockFontStyle.TECH_MONO -> FontFamily.Monospace
    }

    val fontWeight = when {
        config.fontStyle == ClockFontStyle.THIN_ELEGANCE && isHeader -> FontWeight.Light
        config.fontStyle == ClockFontStyle.BOLD_GEOMETRIC && isHeader -> FontWeight.Black
        config.fontWeight == FontWeightOption.LIGHT -> FontWeight.Light
        config.fontWeight == FontWeightOption.REGULAR -> FontWeight.Normal
        config.fontWeight == FontWeightOption.MEDIUM -> FontWeight.Medium
        config.fontWeight == FontWeightOption.BOLD -> FontWeight.Bold
        else -> FontWeight.Medium
    }

    val shadow = when {
        config.textGlowEnabled && isHeader -> Shadow(
            color = color.copy(alpha = 0.45f),
            offset = Offset(0f, 0f),
            blurRadius = 10f
        )
        config.textShadowEnabled -> Shadow(
            color = Color(shadowArgb),
            offset = Offset(0f, 2f),
            blurRadius = 5f
        )
        else -> null
    }

    return TextStyle(
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        letterSpacing = config.letterSpacingEm.toDouble().em,
        shadow = shadow
    )
}

@Composable
private fun PreviewWeekCalendarStrip(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    previewScale: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    paletteShadowArgb: Int
) {
    val allCols = snapshot.weekCalendarColumns
    if (allCols.size < 7) return
    val visibleCols = if (config.weekCalendarDayCount >= 7) {
        allCols
    } else {
        allCols.subList(1, 6)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleCols.forEach { col ->
            val boxModifier = if (col.isToday) {
                Modifier
                    .weight(1.15f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                accentColor.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 3.dp)
            } else {
                Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            }

            Column(
                modifier = boxModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = col.weekdayShortVi,
                    style = buildGlassTextStyle(
                        config = config,
                        fontSize = (9.5f * previewScale * config.textScale * config.calendarStripScale).sp,
                        color = if (col.isToday) primaryColor else secondaryColor,
                        shadowArgb = paletteShadowArgb,
                        isHeader = col.isToday
                    ),
                    maxLines = 1
                )
                Text(
                    text = col.solarDayString,
                    style = buildGlassTextStyle(
                        config = config,
                        fontSize = ((if (col.isToday) 14.0f else 13.0f) * previewScale * config.textScale * config.calendarStripScale).sp,
                        color = primaryColor,
                        shadowArgb = paletteShadowArgb,
                        isHeader = true
                    ),
                    maxLines = 1
                )
                if (config.showLunarDate) {
                    Text(
                        text = col.lunarDayMonthString,
                        style = buildGlassTextStyle(
                            config = config,
                            fontSize = ((if (col.isToday) 9.5f else 9.0f) * previewScale * config.textScale * config.calendarStripScale).sp,
                            color = if (col.isToday) accentColor else secondaryColor,
                            shadowArgb = paletteShadowArgb,
                            isHeader = col.isToday
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewRamCluster(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier,
    previewScale: Float,
    secondaryColor: Color,
    palette: GlassSurfaceRenderer.EffectiveGlassPalette,
    modifier: Modifier = Modifier
) {
    val ram = snapshot.ramInfo
    val text = if (tier == WidgetSizeTier.DASHBOARD_4X4) ram.formatDetailed() else ram.formatMedium()
    val combinedScale = (previewScale * config.textScale * config.ramScale).coerceIn(0.60f, 1.45f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((14 * previewScale).dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x2600E5FF))
                .border(0.5.dp, Color(0x6600E5FF), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "R",
                fontSize = (7.5f * previewScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0F2FE)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = buildGlassTextStyle(
                config = config,
                fontSize = (11f * combinedScale).sp,
                color = secondaryColor,
                shadowArgb = palette.shadowTextArgb,
                isHeader = false
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveCustomSlotsGridOverlay(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    tier: WidgetSizeTier,
    previewScale: Float,
    interactiveEditMode: Boolean,
    selectedSlotId: String?,
    onSlotClick: ((WidgetSlotDefinition) -> Unit)?
) {
    val slots = WidgetSlotBlueprints.slotsForTier(tier)
    fun defAt(index: Int): WidgetSlotDefinition = slots.getOrElse(index) { slots.first() }
    val outerPad = (if (tier == WidgetSizeTier.MINI_2X1 || tier == WidgetSizeTier.COMPACT_2X2) 10 else 12) * previewScale
    val gap = (5 * previewScale).dp

    @Composable
    fun Cell(def: WidgetSlotDefinition, modifier: Modifier) {
        CustomSlotCellCanvas(
            slotDef = def,
            config = config,
            snapshot = snapshot,
            interactiveEditMode = interactiveEditMode,
            isSelected = selectedSlotId == def.slotId,
            onClick = { onSlotClick?.invoke(def) },
            modifier = modifier
        )
    }

    when (tier) {
        WidgetSizeTier.MINI_2X1 -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPad.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                Cell(defAt(0), Modifier.weight(0.52f).fillMaxHeight())
                Cell(defAt(1), Modifier.weight(0.48f).fillMaxHeight())
            }
        }

        WidgetSizeTier.COMPACT_2X2 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPad.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Cell(defAt(0), Modifier.fillMaxWidth().weight(0.50f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.50f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Cell(defAt(1), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(2), Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        WidgetSizeTier.ESSENTIAL_4X2 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPad.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Cell(defAt(0), Modifier.fillMaxWidth().weight(0.62f))
                        Cell(defAt(1), Modifier.fillMaxWidth().weight(0.38f))
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Cell(defAt(2), Modifier.fillMaxWidth().weight(0.42f))
                        Cell(defAt(3), Modifier.fillMaxWidth().weight(0.58f))
                    }
                }
                Cell(defAt(4), Modifier.fillMaxWidth().weight(0.38f))
            }
        }

        WidgetSizeTier.PREMIUM_4X3 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPad.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Cell(defAt(0), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(1), Modifier.weight(1f).fillMaxHeight())
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Cell(defAt(2), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(3), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(4), Modifier.weight(1f).fillMaxHeight())
                }
                Cell(defAt(5), Modifier.fillMaxWidth().weight(0.30f))
            }
        }

        WidgetSizeTier.DASHBOARD_4X4 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPad.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.26f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Cell(defAt(0), Modifier.weight(0.54f).fillMaxHeight())
                    Cell(defAt(1), Modifier.weight(0.46f).fillMaxHeight())
                }
                Cell(defAt(2), Modifier.fillMaxWidth().weight(0.22f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.28f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Cell(defAt(3), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(4), Modifier.weight(1f).fillMaxHeight())
                    Cell(defAt(5), Modifier.weight(1f).fillMaxHeight())
                }
                Cell(defAt(6), Modifier.fillMaxWidth().weight(0.24f))
            }
        }
    }
}

@Composable
private fun CustomSlotCellCanvas(
    slotDef: WidgetSlotDefinition,
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    interactiveEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentType = WidgetSlotBlueprints.slotContent(config, slotDef.slotId)
    val cellShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .clip(cellShape)
            .then(
                when {
                    isSelected && interactiveEditMode ->
                        Modifier.border(1.8.dp, Color(0xFF38BDF8), cellShape)
                    interactiveEditMode && contentType != SlotContentType.EMPTY ->
                        Modifier.border(0.75.dp, Color.White.copy(alpha = 0.24f), cellShape)
                    else -> Modifier
                }
            )
            .then(
                if (interactiveEditMode) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { c ->
                CustomSlotRenderer.drawSlotContent(
                    canvas = c.nativeCanvas,
                    w = size.width,
                    h = size.height,
                    density = density,
                    contentType = contentType,
                    config = config,
                    snapshot = snapshot,
                    isSystemDarkTheme = snapshot.isSystemDarkTheme,
                    slotHintVi = slotDef.shortHintVi,
                    showEmptyPlaceholder = interactiveEditMode,
                    drawClockTextOnCanvas = true
                )
            }
        }
    }
}
