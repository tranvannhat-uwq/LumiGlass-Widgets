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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.MaterialTheme
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
import com.lumiglass.widgets.ui.glass.LiquidGlassBottomBar
import com.lumiglass.widgets.ui.glass.LiquidNavDestinationItem
import com.lumiglass.widgets.ui.settings.SettingsScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioScreen
import com.lumiglass.widgets.ui.studio.WidgetStudioViewModel
import com.lumiglass.widgets.ui.templates.WidgetTemplatesScreen
import com.lumiglass.widgets.ui.theme.LumiGlassTheme
import com.lumiglass.widgets.widget.WidgetUpdateScheduler

private enum class MainDestination(val labelVi: String, val icon: ImageVector) {
    TEMPLATES("Mẫu Widget", Icons.Default.Widgets),
    CUSTOMIZER("Cá nhân hóa", Icons.Default.DashboardCustomize),
    STUDIO("Studio", Icons.Default.Tune),
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
                    initialOpenDirectEdit = openStudioRequested && !isLauncherConfigureIntent,
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

        isLauncherConfigureIntent = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        openStudioRequested = intent?.getBooleanExtra(EXTRA_OPEN_STUDIO_DIRECTLY, false) == true ||
            widgetId != AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            incomingAppWidgetId = widgetId
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
    initialOpenDirectEdit: Boolean,
    isLauncherConfigureMode: Boolean,
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

    var currentDestination by rememberSaveable {
        mutableStateOf(
            if (initialOpenDirectEdit) {
                if (activeConfig.useCustomSlots) MainDestination.CUSTOMIZER else MainDestination.STUDIO
            } else {
                MainDestination.TEMPLATES
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = currentDestination != MainDestination.TEMPLATES && !initialOpenDirectEdit) {
        currentDestination = MainDestination.TEMPLATES
    }

    LaunchedEffect(initialOpenDirectEdit, activeConfig.appWidgetId, activeConfig.useCustomSlots) {
        if (initialOpenDirectEdit && activeConfig.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            currentDestination = if (activeConfig.useCustomSlots) {
                MainDestination.CUSTOMIZER
            } else {
                MainDestination.STUDIO
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
                LiquidGlassBottomBar(
                    items = MainDestination.entries.map {
                        LiquidNavDestinationItem(it, it.labelVi, it.icon)
                    },
                    selectedDestination = currentDestination,
                    onSelectDestination = { dest ->
                        if (dest == MainDestination.CUSTOMIZER) {
                            viewModel.ensureCustomBuilderModeIfEmpty()
                        }
                        currentDestination = dest
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
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
            when (currentDestination) {
                MainDestination.TEMPLATES -> WidgetTemplatesScreen(
                    allPresets = allPresets,
                    snapshot = snapshot,
                    activeWidgets = activeWidgets,
                    simulatedWallpaper = simulatedWallpaper,
                    appUpdateState = appUpdateState,
                    editingAppWidgetId = editingWidgetId,
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
                        viewModel.loadWidgetForEditing(summary.appWidgetId, summary.providerTier)
                        currentDestination = if (summary.config.useCustomSlots) {
                            MainDestination.CUSTOMIZER
                        } else {
                            MainDestination.STUDIO
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
