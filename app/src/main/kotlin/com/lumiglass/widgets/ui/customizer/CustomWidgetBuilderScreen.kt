package com.lumiglass.widgets.ui.customizer

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.model.GlassThemeMode
import com.lumiglass.widgets.model.SlotContentType
import com.lumiglass.widgets.model.TemperatureSymbolStyle
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints
import com.lumiglass.widgets.model.WidgetSlotDefinition
import com.lumiglass.widgets.ui.components.LiveWidgetGlassCard
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.ui.glass.LiquidGlassButton
import com.lumiglass.widgets.ui.glass.LiquidGlassSlider
import com.lumiglass.widgets.ui.glass.LiquidGlassToggle
import com.lumiglass.widgets.ui.glass.LiquidMaterialChip
import kotlin.math.roundToInt

private data class GlassTintOption(
    val labelVi: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val borderHex: Long,
    val preferredTheme: GlassThemeMode
)

private val GLASS_TINT_SWATCHES = listOf(
    GlassTintOption("Pha lê", 0xFFFFFFFFL, 0xFFE2E8F0L, 0xFFFFFFFFL, GlassThemeMode.LIGHT),
    GlassTintOption("Obsidian", 0xFF0F172AL, 0xFF1E293BL, 0xFF38BDF8L, GlassThemeMode.DARK),
    GlassTintOption("Cực quang", 0xFF38BDF8L, 0xFF818CF8L, 0xFF5EEAD4L, GlassThemeMode.AUTO),
    GlassTintOption("Hổ phách", 0xFFC2410CL, 0xFFD97706L, 0xFFFDE68AL, GlassThemeMode.DARK),
    GlassTintOption("Tím Neon", 0xFF581C87L, 0xFF3B0764L, 0xFFC084FCL, GlassThemeMode.DARK),
    GlassTintOption("Ngọc bích", 0xFF0F766EL, 0xFF115E59L, 0xFF5EEAD4L, GlassThemeMode.DARK),
    GlassTintOption("Ngọc trai", 0xFFF8FAFCL, 0xFFCBD5E1L, 0xFFFFFFFFL, GlassThemeMode.LIGHT)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomWidgetBuilderScreen(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    editingAppWidgetId: Int,
    simulatedWallpaper: SimulatedWallpaper,
    isLauncherConfigureMode: Boolean,
    onWallpaperChange: (SimulatedWallpaper) -> Unit,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onResetEmptySlots: () -> Unit,
    onFillSuggestedSlots: () -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onSaveAndApply: () -> Unit,
    onPinToHomeScreen: () -> Unit,
    onOpenAdvancedStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTier = config.preferredSizeTier
    val tierSlots = remember(activeTier) { WidgetSlotBlueprints.slotsForTier(activeTier) }
    val filledCount = WidgetSlotBlueprints.filledCount(config, activeTier)

    var selectedSlotForPicker by remember { mutableStateOf<WidgetSlotDefinition?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var customTemplateName by remember { mutableStateOf("") }

    val dark = isSystemInDarkTheme()
    val panelShape = RoundedCornerShape(22.dp)
    val panelBg = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.78f)
    val panelBorder = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Compact Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = if (editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        "Cá nhân hóa • Widget #$editingAppWidgetId"
                    } else {
                        "Cá nhân hóa Widget"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Chạm ô dấu (+) để xếp thông tin & chỉnh độ trong kính lỏng",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$filledCount/${tierSlots.size} ô",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        // 2. Unified Standardized Size Bar + Circular Wallpaper Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetSizeTier.entries.forEach { tier ->
                    val isSelected = activeTier == tier
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                            .clickable {
                                if (tier != activeTier) {
                                    onUpdateConfig {
                                        it.copy(
                                            preferredSizeTier = tier,
                                            useCustomSlots = true,
                                            customSlots = WidgetSlotBlueprints.remapSlotsForTier(it.customSlots, tier)
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tier.gridLabel,
                            style = MaterialTheme.typography.labelSmall,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimulatedWallpaper.entries.forEach { wp ->
                    val selected = wp == simulatedWallpaper
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

        // 3. Interactive Liquid Glass Canvas Stage
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(simulatedWallpaper.gradientColors))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive Standardized Slot Widget Canvas
            LiveWidgetGlassCard(
                config = config.copy(useCustomSlots = true),
                snapshot = snapshot.copy(isSystemDarkTheme = simulatedWallpaper.isDark),
                tier = activeTier,
                previewScale = 0.94f,
                interactiveSlotEditMode = true,
                selectedSlotId = selectedSlotForPicker?.slotId,
                onSlotClick = { slotDef ->
                    selectedSlotForPicker = slotDef
                }
            )

            // Quick Canvas Utility Actions: Suggest / Reset Blank (strictly single-line equal height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.34f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .clickable { onFillSuggestedSlots() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7DD3FC),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Gợi ý chuẩn",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.34f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .clickable { onResetEmptySlots() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Làm trống (+)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Helpful guidance hint
        Text(
            text = "💡 Chạm vào bất kỳ ô dấu (+) nào ở trên để thêm Đồng hồ, Ngày dương, Lịch Âm Dương, Thời tiết, Pin + Nhiệt độ, Bộ nhớ trong, RAM…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 4. Liquid Glass Opacity / Transparency & Frost Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(panelBg, panelShape)
                .border(1.dp, panelBorder, panelShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "2. Chỉnh độ đục / trong của kính lỏng",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Glass Opacity (Độ đục / Độ trong suốt)
            val opacityPct = (config.glassOpacity * 100f).roundToInt().coerceIn(0, 100)
            val transparencyPct = 100 - opacityPct
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Độ đục kính: $opacityPct%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Độ trong suốt: $transparencyPct%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LiquidGlassSlider(
                    value = config.glassOpacity,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(glassOpacity = v.coerceIn(0f, 1f), useCustomSlots = true) }
                    },
                    valueRange = 0f..1f
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickOpacities = listOf(
                        "Siêu trong (8%)" to 0.08f,
                        "Kính lỏng (28%)" to 0.28f,
                        "Bán mờ (52%)" to 0.52f,
                        "Đục sang (80%)" to 0.80f
                    )
                    quickOpacities.forEach { (label, targetOpacity) ->
                        val isNear = kotlin.math.abs(config.glassOpacity - targetOpacity) < 0.05f
                        LiquidMaterialChip(
                            text = label,
                            isSelected = isNear,
                            horizontalPadding = 10.dp,
                            verticalPadding = 5.dp,
                            onClick = {
                                onUpdateConfig { it.copy(glassOpacity = targetOpacity, useCustomSlots = true) }
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = panelBorder)

            // Frost Amount (Độ mờ sương kính)
            val frostPct = (config.frostAmount * 100f).roundToInt().coerceIn(0, 100)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Độ mờ sương (Frost)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$frostPct%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                LiquidGlassSlider(
                    value = config.frostAmount,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(frostAmount = v.coerceIn(0f, 1f), useCustomSlots = true) }
                    },
                    valueRange = 0f..1f
                )
            }

            // Border Luminance + Corner Radius compact row
            val borderPct = (config.borderLuminance * 100f).roundToInt().coerceIn(0, 100)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Độ sáng viền phản chiếu",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$borderPct%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                LiquidGlassSlider(
                    value = config.borderLuminance,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(borderLuminance = v.coerceIn(0f, 1f), useCustomSlots = true) }
                    },
                    valueRange = 0f..1f
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Độ bo góc kính",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${config.cornerRadiusDp.roundToInt()} dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                LiquidGlassSlider(
                    value = config.cornerRadiusDp,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(cornerRadiusDp = v.coerceIn(8f, 38f), useCustomSlots = true) }
                    },
                    valueRange = 8f..38f
                )
            }

            HorizontalDivider(color = panelBorder)

            // Glass Tint Swatches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sắc thái kính lỏng",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GLASS_TINT_SWATCHES.forEach { swatch ->
                        val isSelected = config.glassTintColorHex == swatch.primaryHex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateConfig {
                                        it.copy(
                                            glassTintColorHex = swatch.primaryHex,
                                            glassSecondaryTintHex = swatch.secondaryHex,
                                            borderColorHex = swatch.borderHex,
                                            themeMode = swatch.preferredTheme,
                                            useCustomSlots = true
                                        )
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(swatch.primaryHex.toInt()),
                                                Color(swatch.secondaryHex.toInt())
                                            )
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 2.2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.65f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (swatch.preferredTheme == GlassThemeMode.LIGHT) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = swatch.labelVi,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Glass Theme Mode (Auto / Light / Dark)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassThemeMode.entries.forEach { mode ->
                    LiquidMaterialChip(
                        text = mode.labelVi,
                        isSelected = config.themeMode == mode,
                        onClick = {
                            onUpdateConfig { it.copy(themeMode = mode, useCustomSlots = true) }
                        }
                    )
                }
            }

            // Smart Alert Colors Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Màu cảnh báo thông minh",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tự đổi cam/đỏ khi pin yếu, pin nóng, bộ nhớ hoặc RAM gần đầy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LiquidGlassToggle(
                    checked = config.dynamicAlertColors,
                    onCheckedChange = { checked ->
                        onUpdateConfig { it.copy(dynamicAlertColors = checked, useCustomSlots = true) }
                    }
                )
            }
        }

        // 5. Primary Action Bar (strictly single-line uniform height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassButton(
                onClick = {
                    customTemplateName = "Mẫu ${activeTier.gridLabel} của tôi"
                    showSaveDialog = true
                },
                shape = RoundedCornerShape(13.dp),
                minHeight = 42.dp,
                horizontalPadding = 14.dp,
                verticalPadding = 6.dp
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Lưu mẫu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            LiquidGlassButton(
                onClick = {
                    if (isLauncherConfigureMode || editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        onSaveAndApply()
                    } else {
                        onPinToHomeScreen()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(13.dp),
                isPrimary = true,
                minHeight = 42.dp,
                horizontalPadding = 14.dp,
                verticalPadding = 6.dp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLauncherConfigureMode || editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        "Áp dụng Widget"
                    } else {
                        "Ghim ${activeTier.gridLabel} ra màn hình"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Link to Advanced Studio for Fonts, Weather City, 12h/24h, 5/7 days
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenAdvancedStudio() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Tinh chỉnh nâng cao trong Studio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Đổi kiểu chữ, chọn thành phố thời tiết, định dạng 12/24h, lịch tuần 5 hoặc 7 ngày…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Mở →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Module Picker Modal Bottom Sheet when any slot is tapped
    val activePickerSlot = selectedSlotForPicker
    if (activePickerSlot != null) {
        val currentAssigned = WidgetSlotBlueprints.slotContent(config, activePickerSlot.slotId)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedSlotForPicker = null },
            sheetState = sheetState
        ) {
            SlotModulePickerSheetContent(
                slotDef = activePickerSlot,
                currentType = currentAssigned,
                config = config,
                snapshot = snapshot,
                onSelectType = { chosen ->
                    onUpdateConfig { cfg ->
                        val nextMap = cfg.customSlots.toMutableMap()
                        if (chosen == SlotContentType.EMPTY) {
                            nextMap.remove(activePickerSlot.slotId)
                        } else {
                            nextMap[activePickerSlot.slotId] = chosen
                        }
                        cfg.copy(
                            useCustomSlots = true,
                            customSlots = nextMap
                        )
                    }
                    selectedSlotForPicker = null
                }
            )
        }
    }

    // Save Custom Template Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text("Lưu vào danh sách Mẫu Widget", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mẫu cá nhân này sẽ xuất hiện trong danh sách Mẫu Widget để bạn sử dụng lại bất cứ lúc nào.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = customTemplateName,
                        onValueChange = { customTemplateName = it },
                        label = { Text("Tên mẫu widget") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = customTemplateName.trim().ifBlank { "Mẫu ${activeTier.gridLabel} cá nhân" }
                        onSaveCustomPreset(finalName)
                        showSaveDialog = false
                    }
                ) {
                    Text("Lưu ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SlotModulePickerSheetContent(
    slotDef: WidgetSlotDefinition,
    currentType: SlotContentType,
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    onSelectType: (SlotContentType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotDef.titleVi,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Chọn thông tin hiển thị trong ô này",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (currentType != SlotContentType.EMPTY) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectType(SlotContentType.EMPTY) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Xóa ô",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Order recommended modules first for this specific slot shape
        val recommended = slotDef.recommendedTypes
        val orderedModules = recommended + SlotContentType.SELECTABLE_MODULES.filterNot { it in recommended }

        orderedModules.forEach { moduleType ->
            val isRecommended = moduleType in recommended
            val isSelected = moduleType == currentType
            val livePreviewText = buildLiveModulePreviewText(moduleType, config, snapshot)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 0.7.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectType(moduleType) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForSlotContent(moduleType),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = moduleType.labelVi,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isRecommended) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Phù hợp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = livePreviewText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = moduleType.descriptionVi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun iconForSlotContent(type: SlotContentType): ImageVector {
    return when (type) {
        SlotContentType.EMPTY -> Icons.Default.Add
        SlotContentType.CLOCK -> Icons.Default.AccessTime
        SlotContentType.GREGORIAN_DATE -> Icons.Default.Today
        SlotContentType.LUNAR_DATE -> Icons.Default.NightsStay
        SlotContentType.DATE_AND_LUNAR -> Icons.Default.CalendarMonth
        SlotContentType.WEEK_CALENDAR -> Icons.Default.CalendarMonth
        SlotContentType.WEATHER -> Icons.Default.Cloud
        SlotContentType.BATTERY_WITH_TEMP -> Icons.Default.BatteryChargingFull
        SlotContentType.STORAGE -> Icons.Default.SdStorage
        SlotContentType.RAM -> Icons.Default.Memory
        SlotContentType.TELEMETRY_TRIO -> Icons.Default.Widgets
    }
}

private fun buildLiveModulePreviewText(
    type: SlotContentType,
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot
): String {
    val degreeOnly = config.temperatureSymbolStyle == TemperatureSymbolStyle.DEGREE_ONLY
    return when (type) {
        SlotContentType.EMPTY -> "Ô trống"
        SlotContentType.CLOCK -> "Hiện tại: ${snapshot.previewTimePrimary}"
        SlotContentType.GREGORIAN_DATE -> snapshot.gregorianDateText
        SlotContentType.LUNAR_DATE -> snapshot.lunarDateInfo.formatDisplay(config.showCanChi, config.showLunarHolidays)
        SlotContentType.DATE_AND_LUNAR -> "${snapshot.gregorianDateText} • ${snapshot.lunarDateInfo.formatLunarShort()} ÂL"
        SlotContentType.WEEK_CALENDAR -> "Dải ${config.weekCalendarDayCount} ngày Âm - Dương quanh hôm nay"
        SlotContentType.WEATHER -> {
            val w = snapshot.weatherInfo
            "${w.cityNameVi}: ${w.formatTemp(w.currentTempC, config.temperatureUnit, degreeOnly)} • ${w.conditionTextVi}"
        }
        SlotContentType.BATTERY_WITH_TEMP -> {
            val b = snapshot.batteryInfo
            "Pin ${b.formatPercent()} • ${b.formatTemperature(config.temperatureUnit)}"
        }
        SlotContentType.STORAGE -> snapshot.storageInfo.formatMedium()
        SlotContentType.RAM -> snapshot.ramInfo.formatMedium()
        SlotContentType.TELEMETRY_TRIO -> {
            "Pin ${snapshot.batteryInfo.formatPercent()} • Bộ nhớ ${snapshot.storageInfo.formatPercent()} • RAM ${snapshot.ramInfo.usedPercent}%"
        }
    }
}
