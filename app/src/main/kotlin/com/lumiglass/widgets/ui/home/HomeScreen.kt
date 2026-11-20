package com.lumiglass.widgets.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.data.update.AppUpdateUiState
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.ui.components.WallpaperStageWidgetPreview
import com.lumiglass.widgets.ui.glass.LiquidGlassButton
import com.lumiglass.widgets.ui.glass.LiquidMaterialChip
import com.lumiglass.widgets.widget.ActiveWidgetSummary
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    currentConfig: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    allPresets: List<GlassPreset>,
    activeWidgets: List<ActiveWidgetSummary>,
    simulatedWallpaper: SimulatedWallpaper,
    appUpdateState: AppUpdateUiState,
    onWallpaperChange: (SimulatedWallpaper) -> Unit,
    onOpenStudioForDraft: () -> Unit,
    onOpenStudioForPreset: (GlassPreset, WidgetSizeTier) -> Unit,
    onOpenStudioForActiveWidget: (ActiveWidgetSummary) -> Unit,
    onPinWidgetTier: (WidgetSizeTier) -> Unit,
    onSyncAllWidgets: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToUpdateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header: LumiGlass Title & Live Telemetry Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LumiGlass",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LiquidMaterialChip(
                        text = "LIQUID GLASS",
                        isSelected = true,
                        horizontalPadding = 10.dp,
                        verticalPadding = 3.dp
                    )
                }
                Text(
                    text = "Thiết kế & Tùy biến Widget Kính Lỏng Cao Cấp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSyncAllWidgets,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Đồng bộ tất cả widget"
                )
            }
        }

        // In-App GitHub Update Banner when a new version is available
        val latestRelease = appUpdateState.latestRelease
        if (latestRelease != null && latestRelease.isNewerThanCurrent) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigateToUpdateSettings() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌟 Đã có bản cập nhật mới ${latestRelease.tagName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Chạm để tải và cài đặt trực tiếp trong ứng dụng (${latestRelease.formatFileSizeMb()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Quick Real-time Telemetry Strip (RAM + Lunar Date + Weather + Battery % & Battery Temp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryBadge(
                icon = Icons.Default.AutoAwesome,
                text = "RAM ${snapshot.ramInfo.formatCompact()}"
            )
            TelemetryBadge(
                icon = Icons.Default.CalendarMonth,
                text = snapshot.lunarDateInfo.formatDisplay(showCanChi = true, showHolidays = true)
            )
            TelemetryBadge(
                icon = Icons.Default.Thermostat,
                text = "${snapshot.weatherInfo.cityNameVi}: ${
                    snapshot.weatherInfo.formatTemp(
                        snapshot.weatherInfo.currentTempC,
                        currentConfig.temperatureUnit
                    )
                }"
            )
            TelemetryBadge(
                icon = Icons.Default.BatteryChargingFull,
                text = "Pin ${snapshot.batteryInfo.formatPercent()} • ${
                    snapshot.batteryInfo.formatTemperature(
                        currentConfig.temperatureUnit
                    )
                }"
            )
        }

        // 2. Featured Widget Showcase at Top of Screen
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Widget nổi bật • ${currentConfig.presetName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentConfig.preferredSizeTier.gridLabel} ${currentConfig.preferredSizeTier.titleVi}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            WallpaperStageWidgetPreview(
                config = currentConfig,
                snapshot = snapshot,
                wallpaper = simulatedWallpaper,
                onWallpaperChange = onWallpaperChange,
                onSizeTierChange = null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = onOpenStudioForDraft,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tùy biến trong Studio", fontWeight = FontWeight.Bold)
                }

                LiquidGlassButton(
                    onClick = { onPinWidgetTier(currentConfig.preferredSizeTier) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đặt Widget", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Create New Widget by Responsive Size (5 Sizes: 2x1, 2x2, 4x2, 4x3, 4x4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Tạo Widget mới theo kích thước",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WidgetSizeTier.entries.forEach { tier ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .width(152.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                val preset = allPresets.firstOrNull { it.id == currentConfig.presetId }
                                    ?: allPresets.first()
                                onOpenStudioForPreset(preset, tier)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = tier.gridLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tier.titleVi,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tier.descriptionVi,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Preset Collection Carousel
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bộ sưu tập phong cách kính",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Xem tất cả →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToGallery() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                allPresets.forEach { preset ->
                    PresetQuickCard(
                        preset = preset,
                        onClick = {
                            onOpenStudioForPreset(preset, currentConfig.preferredSizeTier)
                        }
                    )
                }
            }
        }

        // 5. Existing Active Widgets on Home Screen
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Widget đang đặt ngoài màn hình chính (${activeWidgets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeWidgets.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Chưa có widget nào trên màn hình chính",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhấn 'Đặt Widget' ở trên hoặc nhấn giữ khoảng trống ngoài màn hình chính → chọn Widget → LumiGlass Widgets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                activeWidgets.forEach { active ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenStudioForActiveWidget(active) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = active.providerTier.gridLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Widget #${active.appWidgetId} • ${active.config.presetName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${active.providerTier.titleVi} (${active.widthDp}×${active.heightDp} dp) • Độ trong suốt ${(100 - (active.config.glassOpacity * 100).roundToInt())}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Chỉnh sửa widget"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun TelemetryBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    LiquidMaterialChip(
        text = text,
        icon = icon
    )
}

@Composable
private fun PresetQuickCard(
    preset: GlassPreset,
    onClick: () -> Unit
) {
    val c1 = Color(preset.defaultConfig.glassTintColorHex.toInt())
    val c2 = Color(preset.defaultConfig.glassSecondaryTintHex.toInt())

    val cardShape = RoundedCornerShape(20.dp)
    val dark = androidx.compose.foundation.isSystemInDarkTheme()

    val cardBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFF1F5F9).copy(alpha = 0.70f)
            )
        )
    }

    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.55f else 0.85f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = Modifier
            .width(190.dp)
            .clip(cardShape)
            .background(cardBrush, cardShape)
            .border(width = 0.9.dp, brush = rimBrush, shape = cardShape)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${preset.codeNumber} • ${preset.categoryTag}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c1, c2)))
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = preset.subtitleVi,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Mở trong Studio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
