package com.lumiglass.widgets.ui.templates

import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiglass.widgets.data.update.AppUpdateUiState
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.components.LiveWidgetGlassCard
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.ui.glass.LiquidGlassButton
import com.lumiglass.widgets.ui.glass.LiquidGlassSlider
import com.lumiglass.widgets.ui.glass.LiquidMaterialChip
import com.lumiglass.widgets.widget.ActiveWidgetSummary
import kotlin.math.roundToInt

@Composable
fun WidgetTemplatesScreen(
    allPresets: List<GlassPreset>,
    snapshot: WidgetDataSnapshot,
    activeWidgets: List<ActiveWidgetSummary>,
    simulatedWallpaper: SimulatedWallpaper,
    appUpdateState: AppUpdateUiState,
    editingAppWidgetId: Int,
    isLauncherConfigureMode: Boolean,
    onWallpaperChange: (SimulatedWallpaper) -> Unit,
    onUsePresetDirectly: (GlassPreset, WidgetSizeTier, Float?) -> Unit,
    onEditPreset: (GlassPreset, WidgetSizeTier, Float?) -> Unit,
    onStartBlankCustomBuilder: (WidgetSizeTier) -> Unit,
    onEditActiveWidget: (ActiveWidgetSummary) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onSyncAllWidgets: () -> Unit,
    onNavigateToUpdateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSizeTier by rememberSaveable { mutableStateOf(WidgetSizeTier.ESSENTIAL_4X2) }
    var selectedCategory by rememberSaveable { mutableStateOf("Tất cả") }
    var showQuickOpacityBar by rememberSaveable { mutableStateOf(false) }
    var useCustomOpacityOverride by rememberSaveable { mutableStateOf(false) }
    var quickOpacityValue by rememberSaveable { mutableFloatStateOf(0.32f) }

    val categories = listOf("Tất cả", "Trong suốt", "Sắc màu", "Tối", "Sương mờ", "Thích ứng", "Cá nhân")
    val filteredPresets = allPresets.filter { preset ->
        selectedCategory == "Tất cả" || preset.categoryTag.equals(selectedCategory, ignoreCase = true)
    }

    val dark = isSystemInDarkTheme()
    val effectiveOverride: Float? = if (useCustomOpacityOverride) quickOpacityValue else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLauncherConfigureMode && editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            "Chọn mẫu Widget #$editingAppWidgetId"
                        } else {
                            "LumiGlass"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (!isLauncherConfigureMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidMaterialChip(
                            text = "LIQUID GLASS",
                            isSelected = true,
                            horizontalPadding = 10.dp,
                            verticalPadding = 3.dp
                        )
                    }
                }
                Text(
                    text = "Chọn nhanh mẫu Widget kính lỏng có sẵn hoặc tự thiết kế ở cuối danh sách",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSyncAllWidgets,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Đồng bộ tất cả widget"
                )
            }
        }

        // 2. GitHub In-App Update Banner (if available)
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

        // 3. Active Widgets Strip (compact if user already has widgets on Home Screen)
        if (activeWidgets.isNotEmpty() && !isLauncherConfigureMode) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Widget đang đặt ngoài màn hình (${activeWidgets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeWidgets.forEach { active ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    0.8.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onEditActiveWidget(active) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = active.providerTier.gridLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column {
                                    Text(
                                        text = active.config.presetName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID #${active.appWidgetId} • Trong ${(100 - (active.config.glassOpacity * 100).roundToInt())}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Size Tier Filter & Style Filter + Quick Glass Opacity Control
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kích thước mẫu chuẩn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LiquidMaterialChip(
                    text = if (useCustomOpacityOverride) {
                        "Độ đục ${(quickOpacityValue * 100).roundToInt()}%"
                    } else {
                        "Chỉnh độ trong kính"
                    },
                    icon = Icons.Default.Opacity,
                    isSelected = showQuickOpacityBar || useCustomOpacityOverride,
                    horizontalPadding = 10.dp,
                    verticalPadding = 4.dp,
                    onClick = { showQuickOpacityBar = !showQuickOpacityBar }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetSizeTier.entries.forEach { tier ->
                    LiquidMaterialChip(
                        text = "${tier.gridLabel} • ${tier.titleVi}",
                        isSelected = selectedSizeTier == tier,
                        onClick = { selectedSizeTier = tier }
                    )
                }
            }

            // Optional Quick Glass Opacity Override Bar for all Templates
            AnimatedVisibility(visible = showQuickOpacityBar) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            0.8.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (useCustomOpacityOverride) {
                                    "Độ đục: ${(quickOpacityValue * 100).roundToInt()}% • Trong suốt: ${100 - (quickOpacityValue * 100).roundToInt()}%"
                                } else {
                                    "Đang dùng độ trong mặc định của từng mẫu"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (useCustomOpacityOverride) {
                                Text(
                                    text = "Mặc định",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { useCustomOpacityOverride = false }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        LiquidGlassSlider(
                            value = quickOpacityValue,
                            onValueChange = { v ->
                                quickOpacityValue = v.coerceIn(0f, 1f)
                                useCustomOpacityOverride = true
                            },
                            valueRange = 0f..1f
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nền thử:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SimulatedWallpaper.entries.forEach { wp ->
                                LiquidMaterialChip(
                                    text = wp.labelVi,
                                    isSelected = simulatedWallpaper == wp,
                                    horizontalPadding = 9.dp,
                                    verticalPadding = 4.dp,
                                    onClick = { onWallpaperChange(wp) }
                                )
                            }
                        }
                    }
                }
            }

            // Style Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    LiquidMaterialChip(
                        text = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        // 5. Ready-to-Use Widget Template List
        if (filteredPresets.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Chưa có mẫu nào trong mục \"$selectedCategory\"",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hãy chọn \"Tất cả\" hoặc tạo mẫu cá nhân của riêng bạn ở khung bên dưới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            filteredPresets.forEach { preset ->
                val previewConfig: WidgetConfig = preset.defaultConfig.copy(
                    preferredSizeTier = selectedSizeTier,
                    glassOpacity = effectiveOverride ?: preset.defaultConfig.glassOpacity
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dark) 0.36f else 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (preset.defaultConfig.useCustomSlots) {
                                                "${preset.codeNumber} • Khung cá nhân"
                                            } else {
                                                "${preset.codeNumber} • ${preset.categoryTag}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = preset.descriptionVi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (preset.isCustomUserPreset) {
                                IconButton(
                                    onClick = { onDeleteCustomPreset(preset.id) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa mẫu cá nhân",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Liquid Glass Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(simulatedWallpaper.gradientColors))
                                .clickable { onUsePresetDirectly(preset, selectedSizeTier, effectiveOverride) }
                                .padding(vertical = 15.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LiveWidgetGlassCard(
                                config = previewConfig,
                                snapshot = snapshot.copy(isSystemDarkTheme = simulatedWallpaper.isDark),
                                tier = selectedSizeTier,
                                previewScale = 0.94f
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = { onUsePresetDirectly(preset, selectedSizeTier, effectiveOverride) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(13.dp),
                                isPrimary = true,
                                minHeight = 40.dp,
                                horizontalPadding = 12.dp,
                                verticalPadding = 6.dp,
                                elevation = 3.dp
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isLauncherConfigureMode) {
                                        "Chọn mẫu này"
                                    } else {
                                        "Dùng mẫu ${selectedSizeTier.gridLabel}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            LiquidGlassButton(
                                onClick = { onEditPreset(preset, selectedSizeTier, effectiveOverride) },
                                modifier = Modifier.weight(0.85f),
                                shape = RoundedCornerShape(13.dp),
                                minHeight = 40.dp,
                                horizontalPadding = 12.dp,
                                verticalPadding = 6.dp,
                                elevation = 2.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Tùy chỉnh",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. DEDICATED PERSONALIZATION CARD AT THE END OF THE LIST (Ở cuối danh sách)
        PersonalizeAtEndOfListCard(
            selectedSizeTier = selectedSizeTier,
            onStartBlankCustomBuilder = { onStartBlankCustomBuilder(selectedSizeTier) }
        )

        Spacer(modifier = Modifier.height(26.dp))
    }
}

@Composable
private fun PersonalizeAtEndOfListCard(
    selectedSizeTier: WidgetSizeTier,
    onStartBlankCustomBuilder: () -> Unit
) {
    val cardShape = RoundedCornerShape(26.dp)
    val heroGradient = Brush.linearGradient(
        listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0E7490)
        )
    )
    val rimGradient = Brush.linearGradient(
        listOf(
            Color(0xFF7DD3FC).copy(alpha = 0.85f),
            Color(0xFFC084FC).copy(alpha = 0.70f),
            Color(0xFF38BDF8).copy(alpha = 0.85f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(heroGradient, cardShape)
            .border(1.4.dp, rimGradient, cardShape)
            .clickable { onStartBlankCustomBuilder() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFF7DD3FC).copy(alpha = 0.75f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DashboardCustomize,
                    contentDescription = null,
                    tint = Color(0xFF7DD3FC),
                    modifier = Modifier.size(25.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cá nhân hóa theo ý thích",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Tự ghép khung thông tin kính lỏng chuẩn ${selectedSizeTier.gridLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFBAE6FD),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Visual Mini Blueprint showing empty (+) slots
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.9.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniPlusSlotPreview("Đồng hồ", Modifier.weight(1f))
                MiniPlusSlotPreview("Thời tiết", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniPlusSlotPreview("Pin + Nhiệt", Modifier.weight(1f))
                MiniPlusSlotPreview("Bộ nhớ", Modifier.weight(1f))
                MiniPlusSlotPreview("RAM", Modifier.weight(1f))
            }
            MiniPlusSlotPreview("Lịch Âm - Dương & Thứ ngày tháng", Modifier.fillMaxWidth())
        }

        Text(
            text = "Khởi đầu từ khung vị trí trống có dấu (+) đặt chuẩn theo từng kích thước widget. Tự thêm Đồng hồ, Lịch Âm Dương, Thời tiết, Pin + Nhiệt độ, Bộ nhớ trong, RAM và chỉnh độ đục / trong của kính lỏng.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                    )
                )
                .clickable { onStartBlankCustomBuilder() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Bắt đầu tự thiết kế Widget (+)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
private fun MiniPlusSlotPreview(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.8.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
