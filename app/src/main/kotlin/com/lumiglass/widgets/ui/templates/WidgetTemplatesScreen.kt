package com.lumiglass.widgets.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.data.update.AppUpdateUiState
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.components.LiveWidgetGlassCard
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.widget.ActiveWidgetSummary
import kotlin.math.roundToInt

@Composable
fun WidgetTemplatesScreen(
    allPresets: List<GlassPreset>,
    snapshot: WidgetDataSnapshot,
    activeWidgets: List<ActiveWidgetSummary>,
    simulatedWallpaper: SimulatedWallpaper,
    appUpdateState: AppUpdateUiState,
    lockedSize: WidgetSizeTier?,
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
    var selectedSize by rememberSaveable { mutableStateOf(WidgetSizeTier.ESSENTIAL_4X2) }
    var category by rememberSaveable(lockedSize) { mutableStateOf("Tất cả") }
    var showPreviewSettings by rememberSaveable { mutableStateOf(false) }
    var useOpacity by rememberSaveable { mutableStateOf(false) }
    var opacity by rememberSaveable { mutableFloatStateOf(.65f) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val tier = lockedSize ?: selectedSize
    val override = opacity.takeIf { useOpacity }
    val categories = listOf("Tất cả", "Đồng hồ", "Lịch", "Thời tiết", "Thiết bị", "Tổng hợp", "Kính cổ điển", "Mẫu đã lưu")
    val shown = allPresets.filter { preset ->
        when (category) {
            "Tất cả" -> true
            "Kính cổ điển" -> preset.recipe == null && !preset.isCustomUserPreset
            "Mẫu đã lưu" -> preset.isCustomUserPreset
            else -> !preset.isCustomUserPreset && preset.categoryTag == category
        }
    }

    if (showPreviewSettings) {
        AlertDialog(
            onDismissRequest = { showPreviewSettings = false },
            title = { Text("Tùy chọn xem trước") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterRow(SimulatedWallpaper.entries.map { it.labelVi }, simulatedWallpaper.labelVi) { selected ->
                        onWallpaperChange(SimulatedWallpaper.entries.first { it.labelVi == selected })
                    }
                    FilterChip(
                        selected = useOpacity,
                        onClick = { useOpacity = !useOpacity },
                        label = { Text("Áp dụng độ đậm riêng khi dùng mẫu") }
                    )
                    if (useOpacity) {
                        Text("Độ đậm ${(opacity * 100).roundToInt()}%")
                        Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0f..1f)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPreviewSettings = false }) { Text("Xong") } }
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Xóa mẫu đã lưu?") },
            text = { Text("Widget đã ghim từ mẫu này vẫn dùng cấu hình hiện tại.") },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Hủy") } },
            confirmButton = {
                TextButton(onClick = { onDeleteCustomPreset(id); deleteId = null }) { Text("Xóa mẫu") }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp
        )
    ) {
        item("header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isLauncherConfigureMode) "Chọn mẫu widget" else "LumiGlass",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (lockedSize != null) "Kích thước ${lockedSize.gridLabel}" else "Thiết kế màn hình chính",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(onClick = { onStartBlankCustomBuilder(tier) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Tạo widget", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                if (lockedSize == null) {
                    Text("Kích thước", style = MaterialTheme.typography.titleSmall)
                    FilterRow(WidgetSizeTier.entries.map { it.gridLabel }, tier.gridLabel) { label ->
                        selectedSize = WidgetSizeTier.entries.first { it.gridLabel == label }
                    }
                }
            }
        }

        val update = appUpdateState.latestRelease
        if (update?.isNewerThanCurrent == true && !isLauncherConfigureMode) {
            item("update") {
                OutlinedButton(onClick = onNavigateToUpdateSettings) { Text("Có bản cập nhật ${update.tagName}") }
            }
        }

        if (activeWidgets.isNotEmpty() && !isLauncherConfigureMode) {
            item("placed") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Widget đang dùng", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = onSyncAllWidgets) {
                            Icon(Icons.Default.Refresh, contentDescription = "Làm mới widget đã ghim")
                        }
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeWidgets.forEach { widget ->
                            OutlinedButton(onClick = { onEditActiveWidget(widget) }) {
                                Text(
                                    "${widget.providerTier.gridLabel} · ${widget.config.presetName} #${widget.appWidgetId}",
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        item("filters") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bộ sưu tập", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showPreviewSettings = true }) { Text("Nền và độ đậm") }
                }
                FilterRow(categories, category) { category = it }
            }
        }

        if (shown.isEmpty()) {
            item("empty") {
                Text("Mẫu bạn lưu trong trình chỉnh sửa sẽ xuất hiện ở đây.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(shown, key = { it.id }) { preset ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val preview = preset.configForTier(tier)
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(19.dp))
                            .background(Brush.linearGradient(simulatedWallpaper.gradientColors))
                            .padding(10.dp).heightIn(min = 88.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveWidgetGlassCard(
                            config = preview.copy(glassOpacity = override ?: preview.glassOpacity),
                            snapshot = snapshot.copy(isSystemDarkTheme = simulatedWallpaper.isDark),
                            tier = tier,
                            previewScale = if (tier == WidgetSizeTier.DASHBOARD_4X4) .82f else .94f
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                preset.descriptionVi, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (preset.isCustomUserPreset) {
                            IconButton(onClick = { deleteId = preset.id }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa mẫu ${preset.name}")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onUsePresetDirectly(preset, tier, override) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.AddToHomeScreen, contentDescription = null)
                            Text(if (isLauncherConfigureMode) "Chọn mẫu" else "Ghim widget", Modifier.padding(start = 6.dp))
                        }
                        OutlinedIconButton(onClick = { onEditPreset(preset, tier, override) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Chỉnh mẫu ${preset.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(value, maxLines = 1) }
            )
        }
    }
}
