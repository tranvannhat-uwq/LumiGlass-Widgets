package com.lumiglass.widgets.ui.studio

import android.Manifest
import android.appwidget.AppWidgetManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.data.weather.OpenMeteoApiClient
import com.lumiglass.widgets.data.weather.VietnamCityCatalog
import com.lumiglass.widgets.data.weather.WeatherRepository
import com.lumiglass.widgets.model.BatteryDisplayStyle
import com.lumiglass.widgets.model.ClockFontStyle
import com.lumiglass.widgets.model.ContentAlignmentOption
import com.lumiglass.widgets.model.DateFormatStyle
import com.lumiglass.widgets.model.FontWeightOption
import com.lumiglass.widgets.model.GlassIconStyle
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.GlassThemeMode
import com.lumiglass.widgets.model.TemperatureUnit
import com.lumiglass.widgets.model.TimezoneMode
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetLayoutVariant
import com.lumiglass.widgets.model.WidgetModuleType
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.ui.components.WallpaperStageWidgetPreview
import kotlin.math.roundToInt

private enum class StudioTab(val titleVi: String, val icon: ImageVector) {
    APPEARANCE("Giao diện", Icons.Default.Palette),
    CONTENT("Nội dung", Icons.Default.Layers),
    TYPOGRAPHY("Chữ & Icon", Icons.Default.FormatSize),
    DATA("Dữ liệu", Icons.Default.CloudSync)
}

private data class ColorSwatchOption(
    val nameVi: String,
    val primaryHex: Long,
    val secondaryHex: Long
)

private val GLASS_TINT_SWATCHES = listOf(
    ColorSwatchOption("Pha lê trắng", 0xFFFFFFFFL, 0xFFE2E8F0L),
    ColorSwatchOption("Băng cực quang", 0xFF7DD3FCL, 0xFFC084FCL),
    ColorSwatchOption("Ngọc lục bảo", 0xFF5EEAD4L, 0xFF38BDF8L),
    ColorSwatchOption("Tím thạch anh", 0xFFC084FCL, 0xFFF472B6L),
    ColorSwatchOption("Vàng Champagne", 0xFFFDE68AL, 0xFFFCA5A5L),
    ColorSwatchOption("Xanh đại dương", 0xFF0284C7L, 0xFF1E3A8AL),
    ColorSwatchOption("Khói Obsidian", 0xFF0F172AL, 0xFF1E293BL),
    ColorSwatchOption("Đen Midnight", 0xFF020617L, 0xFF0F172AL)
)

private val BORDER_COLOR_SWATCHES = listOf(
    ColorSwatchOption("Bạc tinh khiết", 0xFFFFFFFFL, 0xFFFFFFFFL),
    ColorSwatchOption("Xanh băng", 0xFF7DD3FCL, 0xFF7DD3FCL),
    ColorSwatchOption("Xanh ngọc", 0xFF5EEAD4L, 0xFF5EEAD4L),
    ColorSwatchOption("Tím Lavender", 0xFFD8B4FEL, 0xFFD8B4FEL),
    ColorSwatchOption("Ánh vàng kim", 0xFFFDE68AL, 0xFFFDE68AL),
    ColorSwatchOption("Thép lạnh", 0xFF94A3B8L, 0xFF94A3B8L)
)

private val TEXT_COLOR_SWATCHES = listOf(
    ColorSwatchOption("Trắng tinh khiết", 0xFFFFFFFFL, 0xFFFFFFFFL),
    ColorSwatchOption("Trắng ngọc trai", 0xFFF8FAFCL, 0xFFF8FAFCL),
    ColorSwatchOption("Xanh băng sáng", 0xFFE0F2FEL, 0xFFE0F2FEL),
    ColorSwatchOption("Vàng kem ấm", 0xFFFEF3C7L, 0xFFFEF3C7L),
    ColorSwatchOption("Xám than đậm", 0xFF0F172AL, 0xFF0F172AL)
)

@Composable
fun WidgetStudioScreen(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    editingAppWidgetId: Int,
    allPresets: List<GlassPreset>,
    simulatedWallpaper: SimulatedWallpaper,
    isRefreshingWeather: Boolean,
    isLauncherConfigureMode: Boolean,
    onWallpaperChange: (SimulatedWallpaper) -> Unit,
    onSelectPreset: (GlassPreset) -> Unit,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onMoveModuleOrder: (WidgetModuleType, Boolean) -> Unit,
    onResetDefault: () -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onRefreshWeatherNow: () -> Unit,
    onSaveAndApply: () -> Unit,
    onPinToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSavePresetDialog by rememberSaveable { mutableStateOf(false) }
    var customPresetNameInput by rememberSaveable { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateConfig { it.copy(useCoarseLocation = granted) }
        if (granted) {
            onRefreshWeatherNow()
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Lưu Preset Kính Cá Nhân") },
            text = {
                Column {
                    Text(
                        text = "Lưu toàn bộ màu kính, độ trong suốt, font chữ và bố cục hiện tại để tái sử dụng cho các widget khác.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customPresetNameInput,
                        onValueChange = { customPresetNameInput = it },
                        label = { Text("Tên mẫu kính (VD: Kính Ngọc Lam)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomPreset(customPresetNameInput)
                        customPresetNameInput = ""
                        showSavePresetDialog = false
                    }
                ) {
                    Text("Lưu Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Bar & Live Preview Stage
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            "Widget Studio • Widget #$editingAppWidgetId"
                        } else {
                            "Widget Studio • ${config.presetName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${config.preferredSizeTier.gridLabel} ${config.preferredSizeTier.titleVi} • Độ trong suốt ${(config.glassOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showSavePresetDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Lưu Preset cá nhân",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onResetDefault,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Khôi phục mặc định"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            WallpaperStageWidgetPreview(
                config = config,
                snapshot = snapshot,
                wallpaper = simulatedWallpaper,
                onWallpaperChange = onWallpaperChange,
                onSizeTierChange = { newTier ->
                    onUpdateConfig { it.copy(preferredSizeTier = newTier) }
                }
            )
        }

        // 2. Editor Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            StudioTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.heightIn(min = 48.dp),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tab.titleVi,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }

        // 3. Scrollable Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (StudioTab.entries[selectedTabIndex]) {
                StudioTab.APPEARANCE -> AppearanceTabContent(
                    config = config,
                    allPresets = allPresets,
                    onSelectPreset = onSelectPreset,
                    onUpdateConfig = onUpdateConfig
                )
                StudioTab.CONTENT -> ContentTabContent(
                    config = config,
                    onUpdateConfig = onUpdateConfig,
                    onMoveModuleOrder = onMoveModuleOrder
                )
                StudioTab.TYPOGRAPHY -> TypographyTabContent(
                    config = config,
                    onUpdateConfig = onUpdateConfig
                )
                StudioTab.DATA -> DataTabContent(
                    config = config,
                    snapshot = snapshot,
                    isRefreshingWeather = isRefreshingWeather,
                    onUpdateConfig = onUpdateConfig,
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onRefreshWeatherNow = onRefreshWeatherNow
                )
            }
        }

        // 4. Persistent Bottom Action Bar (Minimum 48dp touch height)
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSaveAndApply,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLauncherConfigureMode) {
                            "Áp dụng & Đặt Widget"
                        } else if (editingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            "Áp dụng (#$editingAppWidgetId)"
                        } else {
                            "Lưu & Áp dụng"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLauncherConfigureMode) {
                    OutlinedButton(
                        onClick = onPinToHomeScreen,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ghim màn hình",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceTabContent(
    config: WidgetConfig,
    allPresets: List<GlassPreset>,
    onSelectPreset: (GlassPreset) -> Unit,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 5 Glass Presets + Custom Presets
        StudioSectionTitle("Bộ sưu tập phong cách kính (Presets)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allPresets.forEach { preset ->
                val isSelected = config.presetId == preset.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .width(156.dp)
                        .heightIn(min = 76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${preset.codeNumber} • ${preset.categoryTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.defaultConfig.glassTintColorHex.toInt()))
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.subtitleVi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Theme Mode (Light / Dark / Auto)
        StudioSectionTitle("Chế độ thích ứng Sáng / Tối")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = config.themeMode == mode,
                    onClick = { onUpdateConfig { it.copy(themeMode = mode) } },
                    label = { Text(mode.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        // Glass Tint Color Picker
        StudioSectionTitle("Màu bề mặt kính (Glass Tint)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GLASS_TINT_SWATCHES.forEach { swatch ->
                val selected = config.glassTintColorHex == swatch.primaryHex
                SwatchButton(
                    nameVi = swatch.nameVi,
                    color = Color(swatch.primaryHex.toInt()),
                    selected = selected,
                    onClick = {
                        onUpdateConfig {
                            it.copy(
                                glassTintColorHex = swatch.primaryHex,
                                glassSecondaryTintHex = swatch.secondaryHex
                            )
                        }
                    }
                )
            }
        }

        // Border Color Picker
        StudioSectionTitle("Màu viền phản chiếu ánh sáng (Specular Rim)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BORDER_COLOR_SWATCHES.forEach { swatch ->
                val selected = config.borderColorHex == swatch.primaryHex
                SwatchButton(
                    nameVi = swatch.nameVi,
                    color = Color(swatch.primaryHex.toInt()),
                    selected = selected,
                    onClick = {
                        onUpdateConfig { it.copy(borderColorHex = swatch.primaryHex) }
                    }
                )
            }
        }

        // Optical Sliders
        StudioSliderControl(
            title = "Độ đục / Trong suốt kính (Opacity)",
            valueText = "${(config.glassOpacity * 100).roundToInt()}% (Trong suốt ${(100 - (config.glassOpacity * 100).roundToInt())}%)",
            value = config.glassOpacity,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(glassOpacity = v) } }
        )

        StudioSliderControl(
            title = "Mức làm mờ sương mô phỏng (Frosted Satin)",
            valueText = "${(config.frostAmount * 100).roundToInt()}%",
            value = config.frostAmount,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(frostAmount = v) } }
        )

        StudioSliderControl(
            title = "Độ sáng viền phản chiếu (Rim Luminance)",
            valueText = "${(config.borderLuminance * 100).roundToInt()}%",
            value = config.borderLuminance,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(borderLuminance = v) } }
        )

        StudioSliderControl(
            title = "Khúc xạ ánh sáng mép kính (Edge Refraction)",
            valueText = "${(config.refractionIntensity * 100).roundToInt()}%",
            value = config.refractionIntensity,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(refractionIntensity = v) } }
        )

        StudioSliderControl(
            title = "Độ bo góc mềm mại (Corner Radius)",
            valueText = "${config.cornerRadiusDp.roundToInt()} dp",
            value = config.cornerRadiusDp,
            valueRange = 8f..38f,
            onValueChange = { v -> onUpdateConfig { it.copy(cornerRadiusDp = v) } }
        )

        StudioSliderControl(
            title = "Chiều sâu bóng đổ (Ambient Shadow)",
            valueText = "${(config.shadowIntensity * 100).roundToInt()}%",
            value = config.shadowIntensity,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(shadowIntensity = v) } }
        )

        StudioSliderControl(
            title = "Ánh sáng phản xạ mặt kính (Specular Highlight)",
            valueText = "${(config.highlightIntensity * 100).roundToInt()}%",
            value = config.highlightIntensity,
            valueRange = 0.0f..1.0f,
            onValueChange = { v -> onUpdateConfig { it.copy(highlightIntensity = v) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentTabContent(
    config: WidgetConfig,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onMoveModuleOrder: (WidgetModuleType, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioSectionTitle("Bật / Tắt & Sắp xếp thứ tự ưu tiên Module")
        Text(
            text = "Bạn có thể bật/tắt từng thành phần độc lập và dùng mũi tên Lên/Xuống để đổi thứ tự ưu tiên hiển thị.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        config.moduleOrder.forEachIndexed { index, module ->
            val isEnabled = when (module) {
                WidgetModuleType.CLOCK -> config.showClock
                WidgetModuleType.GREGORIAN_DATE -> config.showGregorianDate
                WidgetModuleType.LUNAR_DATE -> config.showLunarDate
                WidgetModuleType.WEATHER -> config.showWeather
                WidgetModuleType.BATTERY_PERCENT -> config.showBatteryPercent
                WidgetModuleType.BATTERY_TEMP -> config.showBatteryTemp
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${module.labelVi}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveModuleOrder(module, true) },
                            enabled = index > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Đưa lên")
                        }
                        IconButton(
                            onClick = { onMoveModuleOrder(module, false) },
                            enabled = index < config.moduleOrder.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Đưa xuống")
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                onUpdateConfig { cfg ->
                                    when (module) {
                                        WidgetModuleType.CLOCK -> cfg.copy(showClock = checked)
                                        WidgetModuleType.GREGORIAN_DATE -> cfg.copy(showGregorianDate = checked)
                                        WidgetModuleType.LUNAR_DATE -> cfg.copy(showLunarDate = checked)
                                        WidgetModuleType.WEATHER -> cfg.copy(showWeather = checked)
                                        WidgetModuleType.BATTERY_PERCENT -> cfg.copy(showBatteryPercent = checked)
                                        WidgetModuleType.BATTERY_TEMP -> cfg.copy(showBatteryTemp = checked)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Digital Clock Options
        StudioSectionTitle("A. Tùy chọn Đồng hồ kỹ thuật số")
        StudioSwitchRow(
            title = "Định dạng 24 giờ (14:35 thay vì 02:35 CH)",
            checked = config.use24HourFormat,
            onCheckedChange = { v -> onUpdateConfig { it.copy(use24HourFormat = v) } }
        )
        StudioSwitchRow(
            title = "Hiển thị giây (Native TextClock)",
            checked = config.showSeconds,
            onCheckedChange = { v -> onUpdateConfig { it.copy(showSeconds = v) } }
        )
        StudioSwitchRow(
            title = "Tách đồng hồ thành hai cụm Giờ & Phút",
            checked = config.splitClockTwoLines,
            onCheckedChange = { v -> onUpdateConfig { it.copy(splitClockTwoLines = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Gregorian Date Options
        StudioSectionTitle("B. Định dạng Thứ, Ngày, Tháng, Năm (Tiếng Việt)")
        StudioSwitchRow(
            title = "Hiển thị tháng dạng chữ ('tháng 11' thay vì '/11')",
            checked = config.useMonthInWords,
            onCheckedChange = { v -> onUpdateConfig { it.copy(useMonthInWords = v) } }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateFormatStyle.entries.forEach { style ->
                val selected = config.dateFormatStyle == style
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onUpdateConfig { it.copy(dateFormatStyle = style) } }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = style.labelVi,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ví dụ: ${style.exampleVi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Vietnamese Lunar Calendar Options
        StudioSectionTitle("C. Tùy chọn Âm lịch Việt Nam (UTC+7)")
        StudioSwitchRow(
            title = "Hiển thị Can Chi năm âm lịch (VD: Bính Ngọ)",
            checked = config.showCanChi,
            onCheckedChange = { v -> onUpdateConfig { it.copy(showCanChi = v) } }
        )
        StudioSwitchRow(
            title = "Hiển thị ngày Rằm, Mùng 1 và Lễ Tết Việt Nam",
            checked = config.showLunarHolidays,
            onCheckedChange = { v -> onUpdateConfig { it.copy(showLunarHolidays = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Weather Sub-details
        StudioSectionTitle("D. Thông tin phụ của Thời tiết")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = config.showHighLowTemp,
                onClick = { onUpdateConfig { it.copy(showHighLowTemp = !it.showHighLowTemp) } },
                label = { Text("Cao / Thấp trong ngày") },
                modifier = Modifier.heightIn(min = 48.dp)
            )
            FilterChip(
                selected = config.showHumidity,
                onClick = { onUpdateConfig { it.copy(showHumidity = !it.showHumidity) } },
                label = { Text("Độ ẩm (%)") },
                modifier = Modifier.heightIn(min = 48.dp)
            )
            FilterChip(
                selected = config.showRainProbability,
                onClick = { onUpdateConfig { it.copy(showRainProbability = !it.showRainProbability) } },
                label = { Text("Khả năng mưa (%)") },
                modifier = Modifier.heightIn(min = 48.dp)
            )
            FilterChip(
                selected = config.showFeelsLike,
                onClick = { onUpdateConfig { it.copy(showFeelsLike = !it.showFeelsLike) } },
                label = { Text("Cảm giác như") },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Battery % and Battery Temp Options
        StudioSectionTitle("E & F. Kiểu hiển thị Pin & Cảnh báo nhiệt độ pin")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BatteryDisplayStyle.entries.forEach { bStyle ->
                FilterChip(
                    selected = config.batteryStyle == bStyle,
                    onClick = { onUpdateConfig { it.copy(batteryStyle = bStyle) } },
                    label = { Text(bStyle.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        StudioSwitchRow(
            title = "Tự động đổi màu theo mức % pin",
            checked = config.colorBatteryByLevel,
            onCheckedChange = { v -> onUpdateConfig { it.copy(colorBatteryByLevel = v) } }
        )
        StudioSwitchRow(
            title = "Cảnh báo màu khi nhiệt độ pin ấm/nóng (≥ 38°C)",
            checked = config.warnBatteryTempByColor,
            onCheckedChange = { v -> onUpdateConfig { it.copy(warnBatteryTempByColor = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Layout Variants
        StudioSectionTitle("Phong cách bố cục (Layout Variant)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WidgetLayoutVariant.entries.forEach { variant ->
                FilterChip(
                    selected = config.layoutVariant == variant,
                    onClick = { onUpdateConfig { it.copy(layoutVariant = variant) } },
                    label = { Text(variant.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypographyTabContent(
    config: WidgetConfig,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Font Family
        StudioSectionTitle("Kiểu chữ & Kiểu số đồng hồ (Font Family)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClockFontStyle.entries.forEach { fStyle ->
                FilterChip(
                    selected = config.fontStyle == fStyle,
                    onClick = { onUpdateConfig { it.copy(fontStyle = fStyle) } },
                    label = { Text(fStyle.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        // Font Weight
        StudioSectionTitle("Độ đậm chữ (Font Weight)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FontWeightOption.entries.forEach { weight ->
                FilterChip(
                    selected = config.fontWeight == weight,
                    onClick = { onUpdateConfig { it.copy(fontWeight = weight) } },
                    label = { Text(weight.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        // Text Scale & Letter Spacing
        StudioSliderControl(
            title = "Tỷ lệ kích thước chữ (Text Scale)",
            valueText = "${(config.textScale * 100).roundToInt()}%",
            value = config.textScale,
            valueRange = 0.75f..1.30f,
            onValueChange = { v -> onUpdateConfig { it.copy(textScale = v) } }
        )

        StudioSliderControl(
            title = "Khoảng cách giữa các ký tự (Letter Spacing)",
            valueText = String.format(java.util.Locale.US, "%.2f em", config.letterSpacingEm),
            value = config.letterSpacingEm,
            valueRange = -0.02f..0.12f,
            onValueChange = { v -> onUpdateConfig { it.copy(letterSpacingEm = v) } }
        )

        // Text Glow & Shadow
        StudioSectionTitle("Hiệu ứng quang học cho chữ trên mặt kính")
        StudioSwitchRow(
            title = "Hiệu ứng chữ phát sáng mềm (Soft Luminous Glow)",
            checked = config.textGlowEnabled,
            onCheckedChange = { v -> onUpdateConfig { it.copy(textGlowEnabled = v) } }
        )
        StudioSwitchRow(
            title = "Đổ bóng chữ tăng độ nổi khối (Crisp Drop Shadow)",
            checked = config.textShadowEnabled,
            onCheckedChange = { v -> onUpdateConfig { it.copy(textShadowEnabled = v) } }
        )
        StudioSwitchRow(
            title = "Tự động bảo đảm độ tương phản dễ đọc",
            checked = config.autoContrastEnsure,
            onCheckedChange = { v -> onUpdateConfig { it.copy(autoContrastEnsure = v) } }
        )

        // Text Color Mode
        StudioSectionTitle("Màu chữ (Tự động hoặc Thủ công)")
        StudioSwitchRow(
            title = "Tự động thích ứng màu chữ theo bề mặt kính",
            checked = config.autoTextColor,
            onCheckedChange = { v -> onUpdateConfig { it.copy(autoTextColor = v) } }
        )

        if (!config.autoTextColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TEXT_COLOR_SWATCHES.forEach { swatch ->
                    val selected = config.customTextColorHex == swatch.primaryHex
                    SwatchButton(
                        nameVi = swatch.nameVi,
                        color = Color(swatch.primaryHex.toInt()),
                        selected = selected,
                        onClick = {
                            onUpdateConfig {
                                it.copy(
                                    autoTextColor = false,
                                    customTextColorHex = swatch.primaryHex
                                )
                            }
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Icon Style & Scale
        StudioSectionTitle("Phong cách & Kích thước Biểu tượng (Icons)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassIconStyle.entries.forEach { iStyle ->
                FilterChip(
                    selected = config.iconStyle == iStyle,
                    onClick = { onUpdateConfig { it.copy(iconStyle = iStyle) } },
                    label = { Text(iStyle.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        StudioSliderControl(
            title = "Kích thước biểu tượng thời tiết & pin",
            valueText = "${(config.iconScale * 100).roundToInt()}%",
            value = config.iconScale,
            valueRange = 0.80f..1.30f,
            onValueChange = { v -> onUpdateConfig { it.copy(iconScale = v) } }
        )

        // Alignment
        StudioSectionTitle("Căn chỉnh nội dung (Alignment)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContentAlignmentOption.entries.forEach { align ->
                FilterChip(
                    selected = config.contentAlignment == align,
                    onClick = { onUpdateConfig { it.copy(contentAlignment = align) } },
                    label = { Text(align.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DataTabContent(
    config: WidgetConfig,
    snapshot: WidgetDataSnapshot,
    isRefreshingWeather: Boolean,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRefreshWeatherNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Weather Source & Location
        StudioSectionTitle("Nguồn thời tiết thực & Vị trí")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${snapshot.weatherInfo.cityNameVi} • ${
                                snapshot.weatherInfo.formatTemp(
                                    snapshot.weatherInfo.currentTempC,
                                    config.temperatureUnit
                                )
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${snapshot.weatherInfo.conditionTextVi} • ${
                                WeatherRepository.formatUpdatedTimeShort(
                                    snapshot.weatherInfo.updatedAtEpochMillis
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = OpenMeteoApiClient.ATTRIBUTION_TEXT,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onRefreshWeatherNow,
                        enabled = !isRefreshingWeather,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        if (isRefreshingWeather) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Làm mới")
                        }
                    }
                }
            }
        }

        StudioSwitchRow(
            title = "Sử dụng vị trí gần đúng của thiết bị (Coarse Location)",
            checked = config.useCoarseLocation,
            onCheckedChange = { wantLocation ->
                if (wantLocation) {
                    onRequestLocationPermission()
                } else {
                    onUpdateConfig { it.copy(useCoarseLocation = false) }
                }
            }
        )

        // Manual City Selection (24 Vietnamese Provinces/Cities)
        StudioSectionTitle("Hoặc chọn Tỉnh / Thành phố thủ công (Không cần GPS)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VietnamCityCatalog.CITIES.forEach { city ->
                val selected = !config.useCoarseLocation && config.cityId == city.id
                FilterChip(
                    selected = selected,
                    onClick = {
                        onUpdateConfig {
                            it.copy(
                                useCoarseLocation = false,
                                cityId = city.id
                            )
                        }
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = { Text(city.nameVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Temperature Unit
        StudioSectionTitle("Đơn vị nhiệt độ")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemperatureUnit.entries.forEach { unit ->
                FilterChip(
                    selected = config.temperatureUnit == unit,
                    onClick = { onUpdateConfig { it.copy(temperatureUnit = unit) } },
                    label = { Text(unit.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        // Refresh Interval
        StudioSectionTitle("Chu kỳ tự động làm mới thời tiết")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(30 to "30 phút", 60 to "60 phút (Khuyên dùng)", 120 to "2 giờ", 180 to "3 giờ").forEach { (mins, label) ->
                FilterChip(
                    selected = config.weatherRefreshMinutes == mins,
                    onClick = { onUpdateConfig { it.copy(weatherRefreshMinutes = mins) } },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Battery Monitoring Toggle
        StudioSectionTitle("Dữ liệu Pin & Nhiệt độ Pin thực tế")
        StudioSwitchRow(
            title = "Đọc % pin và nhiệt độ pin (°C) từ Android BatteryManager",
            checked = config.batteryMonitoringEnabled,
            onCheckedChange = { v -> onUpdateConfig { it.copy(batteryMonitoringEnabled = v) } }
        )
        Text(
            text = "Trạng thái hiện tại: Pin ${snapshot.batteryInfo.formatPercent()} • Nhiệt độ pin thực tế: ${snapshot.batteryInfo.formatTemperature(config.temperatureUnit)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Timezone Mode
        StudioSectionTitle("Ngôn ngữ & Múi giờ lịch âm/dương")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimezoneMode.entries.forEach { tzMode ->
                FilterChip(
                    selected = config.timezoneMode == tzMode,
                    onClick = { onUpdateConfig { it.copy(timezoneMode = tzMode) } },
                    label = { Text(tzMode.labelVi) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StudioSliderControl(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SwatchButton(
    nameVi: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = nameVi,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
