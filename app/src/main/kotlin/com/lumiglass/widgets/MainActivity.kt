package com.lumiglass.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.gallery.GalleryScreen
import com.lumiglass.widgets.ui.home.HomeScreen
import com.lumiglass.widgets.ui.settings.SettingsScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioViewModel
import com.lumiglass.widgets.ui.theme.LumiGlassTheme
import com.lumiglass.widgets.widget.WidgetUpdateScheduler

private enum class MainDestination(val labelVi: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Default.Home),
    GALLERY("Bộ sưu tập", Icons.Default.Collections),
    STUDIO("Widget Studio", Icons.Default.Tune),
    SETTINGS("Cài đặt", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val studioViewModel: WidgetStudioViewModel by viewModels()
    private var incomingAppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isLauncherConfigureIntent: Boolean = false
    private var openStudioRequested: Boolean = false

    companion object {
        const val EXTRA_OPEN_STUDIO_DIRECTLY = "com.lumiglass.widgets.EXTRA_OPEN_STUDIO_DIRECTLY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseWidgetIntent(intent)

        setContent {
            val globalSettings by studioViewModel.globalSettings.collectAsState()
            LumiGlassTheme(themeMode = globalSettings.appThemeMode) {
                LumiGlassMainApp(
                    viewModel = studioViewModel,
                    initialOpenStudio = isLauncherConfigureIntent || openStudioRequested,
                    isLauncherConfigureMode = isLauncherConfigureIntent,
                    onFinishWidgetConfigure = { configuredWidgetId ->
                        if (isLauncherConfigureIntent && configuredWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, configuredWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        studioViewModel.refreshBatteryTelemetry()
        studioViewModel.refreshActiveWidgetsList()
    }

    private fun parseWidgetIntent(intent: Intent?) {
        val extras = intent?.extras
        val widgetId = extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        isLauncherConfigureIntent = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        openStudioRequested = intent?.getBooleanExtra(EXTRA_OPEN_STUDIO_DIRECTLY, false) == true ||
            widgetId != AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            incomingAppWidgetId = widgetId
            // Set default RESULT_OK so pressing Back after previewing does not cancel widget placement
            val defaultResult = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            setResult(Activity.RESULT_OK, defaultResult)

            val tier = detectProviderTierForWidgetId(widgetId)
            studioViewModel.loadWidgetForEditing(widgetId, tier)
        }
    }

    private fun detectProviderTierForWidgetId(appWidgetId: Int): WidgetSizeTier {
        val byTier = WidgetUpdateScheduler.getAllActiveWidgetIdsByTier(this)
        for ((tier, ids) in byTier) {
            if (ids.contains(appWidgetId)) return tier
        }
        return WidgetSizeTier.ESSENTIAL_4X2
    }
}

@Composable
private fun LumiGlassMainApp(
    viewModel: WidgetStudioViewModel,
    initialOpenStudio: Boolean,
    isLauncherConfigureMode: Boolean,
    onFinishWidgetConfigure: (Int) -> Unit
) {
    var currentDestination by rememberSaveable {
        mutableStateOf(if (initialOpenStudio) MainDestination.STUDIO else MainDestination.HOME)
    }

    val activeConfig by viewModel.activeConfig.collectAsState()
    val snapshot by viewModel.liveSnapshot.collectAsState()
    val editingWidgetId by viewModel.editingAppWidgetId.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val activeWidgets by viewModel.activeWidgets.collectAsState()
    val simulatedWallpaper by viewModel.simulatedWallpaper.collectAsState()
    val isRefreshingWeather by viewModel.isRefreshingWeather.collectAsState()
    val globalSettings by viewModel.globalSettings.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialOpenStudio, editingWidgetId) {
        if (initialOpenStudio || editingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            currentDestination = MainDestination.STUDIO
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { dest ->
                    val selected = currentDestination == dest
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.labelVi
                            )
                        },
                        label = {
                            Text(
                                text = dest.labelVi,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                MainDestination.HOME -> HomeScreen(
                    currentConfig = activeConfig,
                    snapshot = snapshot,
                    allPresets = allPresets,
                    activeWidgets = activeWidgets,
                    simulatedWallpaper = simulatedWallpaper,
                    appUpdateState = appUpdateState,
                    onWallpaperChange = viewModel::setSimulatedWallpaper,
                    onOpenStudioForDraft = {
                        currentDestination = MainDestination.STUDIO
                    },
                    onOpenStudioForPreset = { preset, tier ->
                        viewModel.startNewWidgetFromPreset(preset, tier)
                        currentDestination = MainDestination.STUDIO
                    },
                    onOpenStudioForActiveWidget = { summary ->
                        viewModel.loadWidgetForEditing(summary.appWidgetId, summary.providerTier)
                        currentDestination = MainDestination.STUDIO
                    },
                    onPinWidgetTier = { tier ->
                        viewModel.requestPinToHomeScreen(tier)
                    },
                    onSyncAllWidgets = viewModel::syncAllWidgetsNow,
                    onNavigateToGallery = {
                        currentDestination = MainDestination.GALLERY
                    },
                    onNavigateToUpdateSettings = {
                        currentDestination = MainDestination.SETTINGS
                    }
                )

                MainDestination.GALLERY -> GalleryScreen(
                    allPresets = allPresets,
                    snapshot = snapshot,
                    simulatedWallpaper = simulatedWallpaper,
                    onSelectPresetForStudio = { preset, tier ->
                        viewModel.startNewWidgetFromPreset(preset, tier)
                        currentDestination = MainDestination.STUDIO
                    },
                    onPinPresetDirectly = { preset, tier ->
                        viewModel.startNewWidgetFromPreset(preset, tier)
                        viewModel.requestPinToHomeScreen(tier)
                    },
                    onDeleteCustomPreset = viewModel::deleteCustomPreset
                )

                MainDestination.STUDIO -> WidgetStudioScreen(
                    config = activeConfig,
                    snapshot = snapshot,
                    editingAppWidgetId = editingWidgetId,
                    allPresets = allPresets,
                    simulatedWallpaper = simulatedWallpaper,
                    isRefreshingWeather = isRefreshingWeather,
                    isLauncherConfigureMode = isLauncherConfigureMode,
                    onWallpaperChange = viewModel::setSimulatedWallpaper,
                    onSelectPreset = viewModel::applyPresetToCurrentSession,
                    onUpdateConfig = viewModel::updateConfig,
                    onMoveModuleOrder = viewModel::moveModuleOrder,
                    onResetDefault = viewModel::resetToPresetDefault,
                    onSaveCustomPreset = viewModel::saveCurrentAsCustomPreset,
                    onRefreshWeatherNow = { viewModel.fetchWeatherForCurrentConfig(forceNetwork = true) },
                    onSaveAndApply = {
                        viewModel.saveAndApplyToWidget { appliedId ->
                            onFinishWidgetConfigure(appliedId)
                        }
                    },
                    onPinToHomeScreen = {
                        viewModel.requestPinToHomeScreen(activeConfig.preferredSizeTier)
                    }
                )

                MainDestination.SETTINGS -> SettingsScreen(
                    globalSettings = globalSettings,
                    snapshot = snapshot,
                    activeWidgetCount = activeWidgets.size,
                    appUpdateState = appUpdateState,
                    onCheckForUpdate = { viewModel.checkForAppUpdate(silent = false) },
                    onDownloadAndInstallUpdate = { viewModel.downloadAndInstallAppUpdate() },
                    onInstallDownloadedApk = { viewModel.installAlreadyDownloadedApk() },
                    onUpdateGlobalSettings = viewModel::updateGlobalSettings,
                    onSyncAllWidgetsNow = viewModel::syncAllWidgetsNow,
                    onManualBackup = { viewModel.performManualBackup {} },
                    onManualRestore = { viewModel.performRestoreFromBackup { _, _ -> } },
                    lastBackupTime = viewModel.getLastBackupTimeFormatted()
                )
            }
        }
    }
}
