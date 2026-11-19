package com.lumiglass.widgets.ui.gallery

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.components.LiveWidgetGlassCard
import com.lumiglass.widgets.ui.components.SimulatedWallpaper

@Composable
fun GalleryScreen(
    allPresets: List<GlassPreset>,
    snapshot: WidgetDataSnapshot,
    simulatedWallpaper: SimulatedWallpaper,
    onSelectPresetForStudio: (GlassPreset, WidgetSizeTier) -> Unit,
    onPinPresetDirectly: (GlassPreset, WidgetSizeTier) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSizeTier by rememberSaveable { mutableStateOf(WidgetSizeTier.ESSENTIAL_4X2) }
    var selectedCategory by rememberSaveable { mutableStateOf("Tất cả") }

    val categories = listOf("Tất cả", "Trong suốt", "Sắc màu", "Tối", "Sương mờ", "Thích ứng", "Cá nhân")

    val filteredPresets = allPresets.filter { preset ->
        selectedCategory == "Tất cả" || preset.categoryTag.equals(selectedCategory, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Bộ sưu tập Liquid Glass",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Phân loại theo kích thước responsive và phong cách quang học kính lỏng. Chạm vào mẫu bất kỳ để tùy biến trong Widget Studio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Filter by Widget Size (2x1, 2x2, 4x2, 4x3, 4x4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Kích thước xem trước:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetSizeTier.entries.forEach { tier ->
                    FilterChip(
                        selected = selectedSizeTier == tier,
                        onClick = { selectedSizeTier = tier },
                        label = { Text("${tier.gridLabel} • ${tier.titleVi}") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // 2. Filter by Glass Style Category
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Phong cách kính:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // 3. Preset Cards with Live Liquid Glass Previews
        filteredPresets.forEach { preset ->
            val previewConfig = preset.defaultConfig.copy(preferredSizeTier = selectedSizeTier)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                        text = "${preset.codeNumber} • ${preset.categoryTag}",
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
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.descriptionVi,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (preset.isCustomUserPreset) {
                            IconButton(
                                onClick = { onDeleteCustomPreset(preset.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa preset cá nhân",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Glass Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(simulatedWallpaper.gradientColors))
                            .clickable { onSelectPresetForStudio(preset, selectedSizeTier) }
                            .padding(vertical = 16.dp, horizontal = 10.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSelectPresetForStudio(preset, selectedSizeTier) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mở trong Widget Studio", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onPinPresetDirectly(preset, selectedSizeTier) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ghim ${selectedSizeTier.gridLabel}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
