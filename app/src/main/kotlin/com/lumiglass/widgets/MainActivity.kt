package com.lumiglass.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.ui.customizer.CustomWidgetBuilderScreen
import com.lumiglass.widgets.ui.settings.SettingsScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioViewModel
import com.lumiglass.widgets.ui.templates.WidgetTemplatesScreen
import com.lumiglass.widgets.ui.theme.LumiGlassTheme
import com.lumiglass.widgets.widget.WidgetUpdateScheduler

private enum class MainDestination(val labelVi: String, val icon: ImageVector) {
    TEMPLATES("Khám phá", Icons.Default.Widgets),
    CUSTOMIZER("Thiết kế", Icons.Default.DashboardCustomize),
    STUDIO("Studio", Icons.Default.Tune),
    SETTINGS("Cài đặt", Icons.Default.Settings)
}

private class WidgetEntry(val id: Int, val tier: WidgetSizeTier, val isConfigure: Boolean)

class MainActivity : ComponentActivity() {

    private val studioViewModel: WidgetStudioViewModel by viewModels()
    private var widgetEntry by mutableStateOf<WidgetEntry?>(null)

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
                    widgetEntry = widgetEntry,
                    onFinishWidgetConfigure = { configuredWidgetId ->
                        if (widgetEntry?.isConfigure == true && configuredWidgetId == widgetEntry?.id) {
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
        studioViewModel.refreshRamTelemetry()
        studioViewModel.refreshStorageTelemetry()
        studioViewModel.refreshActiveWidgetsList()
    }

    private fun parseWidgetIntent(intent: Intent?) {
        val extras = intent?.extras
        val widgetId = extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val configuring = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        if (configuring) setResult(Activity.RESULT_CANCELED)
        val manager = AppWidgetManager.getInstance(this)
        val provider = manager.getAppWidgetInfo(widgetId)?.provider
        val tier = WidgetSizeTier.entries.firstOrNull {
            WidgetUpdateScheduler.providerComponentForTier(this, it) == provider
        }
        if (tier == null) {
            widgetEntry = null
            if (configuring) finish()
            return
        }
        widgetEntry = WidgetEntry(widgetId, tier, configuring)
    }


}

@Composable
private fun LumiGlassMainApp(
    viewModel: WidgetStudioViewModel,
    widgetEntry: WidgetEntry?,
    onFinishWidgetConfigure: (Int) -> Unit
) {
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

    val isLauncherConfigureMode = widgetEntry?.isConfigure == true
    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.TEMPLATES) }
    var editorParent by rememberSaveable { mutableStateOf(MainDestination.TEMPLATES) }
    var loadingWidget by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = currentDestination != MainDestination.TEMPLATES) {
        currentDestination = if (currentDestination == MainDestination.STUDIO) editorParent else MainDestination.TEMPLATES
    }

    LaunchedEffect(widgetEntry) {
        val entry = widgetEntry ?: return@LaunchedEffect
        currentDestination = MainDestination.TEMPLATES
        editorParent = MainDestination.TEMPLATES
        loadingWidget = true
        viewModel.loadWidgetForEditing(entry.id, entry.tier) { loaded ->
            loadingWidget = false
            if (!entry.isConfigure) {
                currentDestination = if (loaded.useCustomSlots) MainDestination.CUSTOMIZER else MainDestination.STUDIO
            }
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        },
        bottomBar = {
            if (!isLauncherConfigureMode) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    MainDestination.entries.filter { it != MainDestination.STUDIO }.forEach { dest ->
                        NavigationBarItem(
                            selected = dest == currentDestination ||
                                (dest == MainDestination.CUSTOMIZER && currentDestination == MainDestination.STUDIO),
                            onClick = {
                                if (dest == MainDestination.CUSTOMIZER) {
                                    editorParent = MainDestination.TEMPLATES
                                    currentDestination = if (activeConfig.useCustomSlots) {
                                        MainDestination.CUSTOMIZER
                                    } else MainDestination.STUDIO
                                } else currentDestination = dest
                            },
                            enabled = !loadingWidget,
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.labelVi) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (loadingWidget) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
                return@Box
            }
            when (currentDestination) {
                MainDestination.TEMPLATES -> WidgetTemplatesScreen(
                    allPresets = allPresets,
                    snapshot = snapshot,
                    activeWidgets = activeWidgets,
                    simulatedWallpaper = simulatedWallpaper,
                    appUpdateState = appUpdateState,
                    lockedSize = widgetEntry?.tier?.takeIf { isLauncherConfigureMode },
                    isLauncherConfigureMode = isLauncherConfigureMode,
                    onWallpaperChange = viewModel::setSimulatedWallpaper,
                    onUsePresetDirectly = { preset, tier, opacityOverride ->
                        viewModel.usePresetDirectly(
                            preset = preset,
                            sizeTier = tier,
                            opacityOverride = opacityOverride,
                            isLauncherConfigureMode = isLauncherConfigureMode,
                            onAppliedComplete = { appliedId ->
                                onFinishWidgetConfigure(appliedId)
                            }
                        )
                    },
                    onEditPreset = { preset, tier, opacityOverride ->
                        editorParent = MainDestination.TEMPLATES
                        viewModel.startNewWidgetFromPresetWithOpacity(
                            preset = preset,
                            sizeTier = tier,
                            opacityOverride = opacityOverride,
                            preserveWidgetId = isLauncherConfigureMode
                        )
                        currentDestination = if (preset.defaultConfig.useCustomSlots) {
                            MainDestination.CUSTOMIZER
                        } else {
                            MainDestination.STUDIO
                        }
                    },
                    onStartBlankCustomBuilder = { tier ->
                        viewModel.startBlankCustomBuilder(
                            sizeTier = tier,
                            preserveWidgetId = isLauncherConfigureMode
                        )
                        currentDestination = MainDestination.CUSTOMIZER
                    },
                    onEditActiveWidget = { summary ->
                        loadingWidget = true
                        editorParent = MainDestination.TEMPLATES
                        viewModel.loadWidgetForEditing(summary.appWidgetId, summary.providerTier) { loaded ->
                            loadingWidget = false
                            currentDestination = if (loaded.useCustomSlots) MainDestination.CUSTOMIZER else MainDestination.STUDIO
                        }
                    },
                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                    onSyncAllWidgets = viewModel::syncAllWidgetsNow,
                    onNavigateToUpdateSettings = {
                        currentDestination = MainDestination.SETTINGS
                    }
                )

                MainDestination.CUSTOMIZER -> CustomWidgetBuilderScreen(
                    config = activeConfig,
                    snapshot = snapshot,
                    editingAppWidgetId = editingWidgetId,
                    simulatedWallpaper = simulatedWallpaper,
                    isLauncherConfigureMode = isLauncherConfigureMode,
                    onWallpaperChange = viewModel::setSimulatedWallpaper,
                    onUpdateConfig = viewModel::updateConfig,
                    onResetEmptySlots = viewModel::resetCustomSlotsToBlank,
                    onFillSuggestedSlots = viewModel::fillSuggestedCustomSlots,
                    onSaveCustomPreset = viewModel::saveCurrentAsCustomPreset,
                    onSaveAndApply = {
                        viewModel.saveAndApplyToWidget { appliedId ->
                            onFinishWidgetConfigure(appliedId)
                        }
                    },
                    onPinToHomeScreen = {
                        viewModel.requestPinToHomeScreen(activeConfig.preferredSizeTier)
                    },
                    onOpenAdvancedStudio = {
                        editorParent = MainDestination.CUSTOMIZER
                        currentDestination = MainDestination.STUDIO
                    }
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
