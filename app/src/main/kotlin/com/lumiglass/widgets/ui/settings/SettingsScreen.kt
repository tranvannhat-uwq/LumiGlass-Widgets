package com.lumiglass.widgets.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.data.config.AppGlobalSettings
import com.lumiglass.widgets.data.update.AppUpdateManager
import com.lumiglass.widgets.data.update.AppUpdateUiState
import com.lumiglass.widgets.data.weather.OpenMeteoApiClient
import com.lumiglass.widgets.data.weather.VietnamCityCatalog
import com.lumiglass.widgets.model.GlassThemeMode
import com.lumiglass.widgets.model.WidgetDataSnapshot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    globalSettings: AppGlobalSettings,
    snapshot: WidgetDataSnapshot,
    activeWidgetCount: Int,
    appUpdateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onDownloadAndInstallUpdate: () -> Unit,
    onInstallDownloadedApk: () -> Unit,
    onUpdateGlobalSettings: ((AppGlobalSettings) -> AppGlobalSettings) -> Unit,
    onSyncAllWidgetsNow: () -> Unit,
    onManualBackup: () -> Unit = {},
    onManualRestore: () -> Unit = {},
    lastBackupTime: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateGlobalSettings { it.copy(defaultUseCoarseLocation = granted) }
        if (granted) onSyncAllWidgetsNow()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = "Cài đặt & Cập nhật",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Quản lý cập nhật trực tiếp từ GitHub, giao diện ứng dụng, nguồn thời tiết Open-Meteo và tối ưu hiệu năng widget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. In-App Update via GitHub Releases Card
        SettingsCard(
            icon = Icons.Default.SystemUpdate,
            title = "Cập nhật ứng dụng trực tiếp (GitHub Releases)"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phiên bản hiện tại: v${appUpdateState.currentVersionName} (Build ${appUpdateState.currentVersionCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nguồn: github.com/${appUpdateState.githubOwner}/${appUpdateState.githubRepo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(AppUpdateManager.DEFAULT_REPO_URL)
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val latest = appUpdateState.latestRelease
            if (latest != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (latest.isNewerThanCurrent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (latest.isNewerThanCurrent) {
                                    "🌟 Bản cập nhật mới: ${latest.tagName}"
                                } else {
                                    "✓ Bản phát hành mới nhất: ${latest.tagName}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (latest.isNewerThanCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = latest.formatFileSizeMb(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (latest.publishedAt.isNotBlank()) {
                            Text(
                                text = "${latest.releaseTitle} • Ngày phát hành: ${latest.publishedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = latest.releaseNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (appUpdateState.isDownloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dlMb = appUpdateState.downloadedBytes.toDouble() / (1024.0 * 1024.0)
                    val totalMb = appUpdateState.totalBytes.toDouble() / (1024.0 * 1024.0)
                    Text(
                        text = String.format(
                            java.util.Locale.US,
                            "Đang tải gói APK trong ứng dụng: %d%% (%.2f / %.2f MB)",
                            appUpdateState.downloadProgressPercent,
                            dlMb,
                            totalMb.coerceAtLeast(dlMb)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { (appUpdateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!appUpdateState.statusTextVi.isNullOrBlank()) {
                Text(
                    text = appUpdateState.statusTextVi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (!appUpdateState.errorTextVi.isNullOrBlank()) {
                Text(
                    text = appUpdateState.errorTextVi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCheckForUpdate,
                    enabled = !appUpdateState.isChecking && !appUpdateState.isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    if (appUpdateState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang kiểm tra...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kiểm tra cập nhật")
                    }
                }

                if (latest != null && !latest.apkDownloadUrl.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = onDownloadAndInstallUpdate,
                        enabled = !appUpdateState.isDownloading,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (latest.isNewerThanCurrent) {
                                "Cập nhật ${latest.tagName}"
                            } else {
                                "Tải & Cài đặt lại"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (appUpdateState.downloadedApkFile?.exists() == true && !appUpdateState.isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onInstallDownloadedApk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mở trình cài đặt APK đã tải (${appUpdateState.downloadedApkFile.name})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Multi-tier Data Protection & Backup / Restore
        SettingsCard(
            icon = Icons.Default.Security,
            title = "Bảo vệ & Sao lưu cấu hình Widget"
        ) {
            Text(
                text = "Hệ thống tự động sao lưu đa tầng (Multi-tier Persistent Backup) đã được kích hoạt. Toàn bộ cấu hình màu kính, độ trong suốt, vị trí và preset cá nhân được bảo vệ an toàn qua các bản cập nhật.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trạng thái sao lưu: $lastBackupTime",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onManualBackup,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sao lưu ngay")
                }

                FilledTonalButton(
                    onClick = onManualRestore,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Khôi phục")
                }
            }
        }

        // 3. App Appearance (Light / Dark / System)
        SettingsCard(
            icon = Icons.Default.Palette,
            title = "Giao diện ứng dụng LumiGlass"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = globalSettings.appThemeMode == mode,
                        onClick = { onUpdateGlobalSettings { it.copy(appThemeMode = mode) } },
                        label = { Text(mode.labelVi) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // 3. Weather Source & Location Permission
        SettingsCard(
            icon = Icons.Default.Cloud,
            title = "Nguồn thời tiết & Quyền vị trí"
        ) {
            Text(
                text = OpenMeteoApiClient.ATTRIBUTION_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LumiGlass sử dụng dữ liệu khí tượng thực từ Open-Meteo API với cơ chế lưu đệm (cache) ngoại tuyến thông minh. Khi ngoại tuyến hoặc lỗi mạng, widget giữ dữ liệu cập nhật gần nhất và tuyệt đối không hiển thị nhiệt độ giả.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cấp quyền Vị trí gần đúng")
                }

                Button(
                    onClick = onSyncAllWidgetsNow,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đồng bộ ($activeWidgetCount)")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Thành phố mặc định cho widget mới: ${VietnamCityCatalog.findById(globalSettings.defaultCityId).nameVi}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("hanoi", "hcmc", "danang", "haiphong", "cantho", "hue", "dalat", "nhatrang").forEach { cityId ->
                    val city = VietnamCityCatalog.findById(cityId)
                    FilterChip(
                        selected = globalSettings.defaultCityId == cityId,
                        onClick = { onUpdateGlobalSettings { it.copy(defaultCityId = cityId) } },
                        label = { Text(city.nameVi) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // 4. Battery & Thermal Telemetry Status
        SettingsCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Thông tin Pin & Nhiệt độ Pin thực tế"
        ) {
            Text(
                text = "Dung lượng pin hiện tại: ${snapshot.batteryInfo.formatPercent()} ${if (snapshot.batteryInfo.isCharging) "(Đang sạc ⚡)" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Nhiệt độ pin hiện tại (EXTRA_TEMPERATURE): ${snapshot.batteryInfo.formatTemperature()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dữ liệu nhiệt độ pin được đọc từ cảm biến nhiệt độ pin qua Android BatteryManager (đơn vị 1/10 °C chuyển đổi sang °C), hoàn toàn tách biệt với nhiệt độ thời tiết và không dùng dịch vụ nền chạy liên tục.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 5. Architecture, Android Widget Limits & Usage Guide
        SettingsCard(
            icon = Icons.Default.Info,
            title = "Hướng dẫn đặt Widget & Giải thích Kỹ thuật"
        ) {
            Text(
                text = "1. Cách đặt và đổi kích thước Widget trên màn hình chính:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Nhấn nút 'Ghim màn hình' trong Widget Studio, HOẶC nhấn giữ khoảng trống trên màn hình chính → chọn 'Tiện ích / Widgets' → tìm 'LumiGlass Widgets'.\n" +
                    "• Chọn một trong 5 kích thước chuẩn: 2×1 Mini Clock, 2×2 Compact Glass, 4×2 Essential Glass, 4×3 Premium Glass, hoặc 4×4 Dashboard Glass.\n" +
                    "• Nhấn giữ widget đã đặt trên màn hình chính và kéo khung viền để thay đổi kích thước: hệ thống Responsive Layout Engine sẽ tự động sắp xếp lại bố cục và cỡ chữ phù hợp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "2. Công nghệ Liquid Glass & Giới hạn Android AppWidget:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Android RemoteViews không cho phép widget tự đọc hoặc làm mờ (real-time blur/shader refraction) trực tiếp ảnh hình nền của Launcher do giới hạn bảo mật sandbox.\n" +
                    "• LumiGlass giải quyết bằng Procedural Optical Glass Engine: kết hợp lớp kính bán trong suốt đa tầng, tán xạ sương mờ satin (Frosted Diffusion), ánh sáng phản xạ mặt kính (Caustic Sheen), khúc xạ Fresnel tại mép kính (Edge Refraction Rim) và viền phản chiếu ánh sáng (Sweep Specular Rim).\n" +
                    "• Đồng hồ sử dụng TextClock native của hệ thống giúp thời gian luôn chính xác từng phút/giây mà không tốn pin chạy ngầm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
