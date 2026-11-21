package com.lumiglass.widgets.ui.studio

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumiglass.widgets.data.battery.BatteryRepository
import com.lumiglass.widgets.data.config.AppGlobalSettings
import com.lumiglass.widgets.data.config.WidgetConfigRepository
import com.lumiglass.widgets.data.datetime.DateTimeRepository
import com.lumiglass.widgets.data.ram.RamInfo
import com.lumiglass.widgets.data.ram.RamRepository
import com.lumiglass.widgets.data.update.AppUpdateManager
import com.lumiglass.widgets.data.update.AppUpdateUiState
import com.lumiglass.widgets.data.update.GitHubReleaseInfo
import com.lumiglass.widgets.data.update.InstallLaunchResult
import com.lumiglass.widgets.data.weather.VietnamCityCatalog
import com.lumiglass.widgets.data.weather.WeatherRepository
import com.lumiglass.widgets.model.BatteryInfo
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.OfficialGlassPresets
import com.lumiglass.widgets.model.WeatherInfo
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetModuleType
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints
import com.lumiglass.widgets.ui.components.SimulatedWallpaper
import com.lumiglass.widgets.widget.ActiveWidgetSummary
import com.lumiglass.widgets.widget.WidgetUpdateScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WidgetStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val configRepo = WidgetConfigRepository(appContext)
    private val weatherRepo = WeatherRepository(appContext)
    private val updateManager = AppUpdateManager()

    private val _appUpdateState = MutableStateFlow(AppUpdateUiState())
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    private val _editingAppWidgetId = MutableStateFlow(AppWidgetManager.INVALID_APPWIDGET_ID)
    val editingAppWidgetId: StateFlow<Int> = _editingAppWidgetId.asStateFlow()

    private val _activeConfig = MutableStateFlow(OfficialGlassPresets.CRYSTAL_CLEAR.defaultConfig)
    val activeConfig: StateFlow<WidgetConfig> = _activeConfig.asStateFlow()

    private val _simulatedWallpaper = MutableStateFlow(SimulatedWallpaper.AURORA_COLORFUL)
    val simulatedWallpaper: StateFlow<SimulatedWallpaper> = _simulatedWallpaper.asStateFlow()

    private val _currentWeather = MutableStateFlow(
        WeatherInfo(
            hasData = false,
            cityId = "hanoi",
            cityNameVi = "Hà Nội",
            conditionTextVi = "Đang tải thời tiết..."
        )
    )
    val currentWeather: StateFlow<WeatherInfo> = _currentWeather.asStateFlow()

    private val _isRefreshingWeather = MutableStateFlow(false)
    val isRefreshingWeather: StateFlow<Boolean> = _isRefreshingWeather.asStateFlow()

    private val _currentBattery = MutableStateFlow(BatteryInfo())
    private val _currentRam = MutableStateFlow(RamRepository.readCurrentRam(appContext))
    val currentRam: StateFlow<RamInfo> = _currentRam.asStateFlow()
    private val _currentStorage = MutableStateFlow(com.lumiglass.widgets.data.storage.StorageRepository.readCurrentStorage())
    val currentStorage: StateFlow<com.lumiglass.widgets.data.storage.StorageInfo> = _currentStorage.asStateFlow()

    private val _clockTickMillis = MutableStateFlow(System.currentTimeMillis())

    private val _activeWidgets = MutableStateFlow<List<ActiveWidgetSummary>>(emptyList())
    val activeWidgets: StateFlow<List<ActiveWidgetSummary>> = _activeWidgets.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val globalSettings: StateFlow<AppGlobalSettings> = configRepo.globalSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppGlobalSettings())

    val customPresets: StateFlow<List<GlassPreset>> = configRepo.customPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<GlassPreset>> = combine(customPresets) { (customs) ->
        OfficialGlassPresets.ALL + customs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfficialGlassPresets.ALL)

    val liveSnapshot: StateFlow<WidgetDataSnapshot> = combine(
        _activeConfig,
        _currentWeather,
        _currentBattery,
        _currentRam,
        _clockTickMillis
    ) { cfg, weather, battery, ram, tickMillis ->
        val zdt = DateTimeRepository.nowZoned(cfg.timezoneMode, tickMillis)
        val clockFormatted = DateTimeRepository.formatPreviewClock(zdt, cfg)
        val gregorianText = DateTimeRepository.formatGregorianDate(
            zdt = zdt,
            style = cfg.dateFormatStyle,
            useMonthInWords = cfg.useMonthInWords
        )
        val weekdayShort = DateTimeRepository.getVietnameseWeekdayShort(zdt.dayOfWeek)
        val lunarInfo = DateTimeRepository.getCurrentLunarDate(zdt)
        val uiMode = appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = uiMode == Configuration.UI_MODE_NIGHT_YES

        WidgetDataSnapshot(
            previewTimePrimary = clockFormatted.primary,
            previewTimeHour = clockFormatted.hourPart,
            previewTimeMinute = clockFormatted.minutePart,
            previewAmPm = clockFormatted.amPmPart,
            gregorianDateText = gregorianText,
            gregorianWeekdayShort = weekdayShort,
            lunarDateInfo = lunarInfo,
            weatherInfo = weather,
            batteryInfo = if (cfg.batteryMonitoringEnabled) battery else BatteryInfo(),
            isSystemDarkTheme = isDark,
            weekCalendarColumns = DateTimeRepository.buildWeekCalendarColumns(zdt),
            ramInfo = ram,
            storageInfo = _currentStorage.value
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        createInitialSnapshot()
    )

    init {
        viewModelScope.launch {
            val draft = configRepo.getStudioDraft()
            if (_editingAppWidgetId.value == AppWidgetManager.INVALID_APPWIDGET_ID) {
                _activeConfig.value = draft
            }
            refreshBatteryTelemetry()
            refreshRamTelemetry()
            refreshStorageTelemetry()
            refreshActiveWidgetsList()
            fetchWeatherForCurrentConfig(forceNetwork = false)
            checkForAppUpdate(silent = true)
        }

        // Real-time 1-second clock & periodic battery/RAM/storage refresh while UI is active
        viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(1000L)
                _clockTickMillis.value = System.currentTimeMillis()
                tickCount++
                if (tickCount % 5 == 0) {
                    refreshBatteryTelemetry()
                    refreshRamTelemetry()
                    refreshStorageTelemetry()
                }
            }
        }
    }

    fun refreshRamTelemetry() {
        _currentRam.value = RamRepository.readCurrentRam(appContext)
    }

    fun refreshStorageTelemetry() {
        _currentStorage.value = com.lumiglass.widgets.data.storage.StorageRepository.readCurrentStorage()
    }

    private fun createInitialSnapshot(): WidgetDataSnapshot {
        val cfg = _activeConfig.value
        val zdt = DateTimeRepository.nowZoned(cfg.timezoneMode)
        val clock = DateTimeRepository.formatPreviewClock(zdt, cfg)
        return WidgetDataSnapshot(
            previewTimePrimary = clock.primary,
            previewTimeHour = clock.hourPart,
            previewTimeMinute = clock.minutePart,
            previewAmPm = clock.amPmPart,
            gregorianDateText = DateTimeRepository.formatGregorianDate(
                zdt,
                cfg.dateFormatStyle,
                cfg.useMonthInWords
            ),
            gregorianWeekdayShort = DateTimeRepository.getVietnameseWeekdayShort(zdt.dayOfWeek),
            lunarDateInfo = DateTimeRepository.getCurrentLunarDate(zdt),
            weatherInfo = _currentWeather.value,
            batteryInfo = BatteryRepository.readCurrentBattery(appContext, true),
            isSystemDarkTheme = true,
            weekCalendarColumns = DateTimeRepository.buildWeekCalendarColumns(zdt),
            ramInfo = RamRepository.readCurrentRam(appContext),
            storageInfo = com.lumiglass.widgets.data.storage.StorageRepository.readCurrentStorage()
        )
    }

    fun setSimulatedWallpaper(wallpaper: SimulatedWallpaper) {
        _simulatedWallpaper.value = wallpaper
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshBatteryTelemetry() {
        _currentBattery.value = BatteryRepository.readCurrentBattery(
            context = appContext,
            enabled = _activeConfig.value.batteryMonitoringEnabled
        )
    }

    fun refreshActiveWidgetsList() {
        viewModelScope.launch {
            _activeWidgets.value = WidgetUpdateScheduler.getAllActiveWidgetSummaries(appContext)
        }
    }

    fun loadWidgetForEditing(
        appWidgetId: Int,
        fallbackTier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2
    ) {
        _editingAppWidgetId.value = appWidgetId
        viewModelScope.launch {
            val loaded = configRepo.getWidgetConfig(appWidgetId, fallbackTier)
            _activeConfig.value = loaded
            fetchWeatherForCurrentConfig(forceNetwork = false)
        }
    }

    fun startNewWidgetFromPreset(
        preset: GlassPreset,
        sizeTier: WidgetSizeTier = _activeConfig.value.preferredSizeTier
    ) {
        startNewWidgetFromPresetWithOpacity(preset, sizeTier, opacityOverride = null, preserveWidgetId = false)
    }

    fun startNewWidgetFromPresetWithOpacity(
        preset: GlassPreset,
        sizeTier: WidgetSizeTier = _activeConfig.value.preferredSizeTier,
        opacityOverride: Float? = null,
        preserveWidgetId: Boolean = false
    ) {
        val targetId = if (preserveWidgetId) _editingAppWidgetId.value else AppWidgetManager.INVALID_APPWIDGET_ID
        if (!preserveWidgetId) {
            _editingAppWidgetId.value = AppWidgetManager.INVALID_APPWIDGET_ID
        }
        val current = _activeConfig.value
        _activeConfig.value = preset.defaultConfig.copy(
            appWidgetId = targetId,
            preferredSizeTier = sizeTier,
            glassOpacity = opacityOverride ?: preset.defaultConfig.glassOpacity,
            cityId = current.cityId,
            useCoarseLocation = current.useCoarseLocation,
            temperatureUnit = current.temperatureUnit,
            weatherRefreshMinutes = current.weatherRefreshMinutes
        )
        viewModelScope.launch {
            configRepo.saveStudioDraft(_activeConfig.value)
        }
    }

    fun usePresetDirectly(
        preset: GlassPreset,
        sizeTier: WidgetSizeTier,
        opacityOverride: Float? = null,
        isLauncherConfigureMode: Boolean = false,
        onAppliedComplete: ((Int) -> Unit)? = null
    ) {
        startNewWidgetFromPresetWithOpacity(
            preset = preset,
            sizeTier = sizeTier,
            opacityOverride = opacityOverride,
            preserveWidgetId = isLauncherConfigureMode
        )
        if (isLauncherConfigureMode && _editingAppWidgetId.value != AppWidgetManager.INVALID_APPWIDGET_ID) {
            saveAndApplyToWidget(onAppliedComplete)
        } else {
            requestPinToHomeScreen(sizeTier)
        }
    }

    fun startBlankCustomBuilder(
        sizeTier: WidgetSizeTier = _activeConfig.value.preferredSizeTier,
        preserveWidgetId: Boolean = false
    ) {
        val targetId = if (preserveWidgetId) _editingAppWidgetId.value else AppWidgetManager.INVALID_APPWIDGET_ID
        if (!preserveWidgetId) {
            _editingAppWidgetId.value = AppWidgetManager.INVALID_APPWIDGET_ID
        }
        val current = _activeConfig.value
        _activeConfig.value = OfficialGlassPresets.createBlankCustomConfig(sizeTier).copy(
            appWidgetId = targetId,
            cityId = current.cityId,
            useCoarseLocation = current.useCoarseLocation,
            temperatureUnit = current.temperatureUnit,
            weatherRefreshMinutes = current.weatherRefreshMinutes
        )
        _statusMessage.value = "Khung kính trống ${sizeTier.gridLabel} đã sẵn sàng — Chạm dấu (+) để thêm thông tin"
    }

    fun ensureCustomBuilderModeIfEmpty() {
        val current = _activeConfig.value
        if (!current.useCustomSlots) {
            _activeConfig.value = OfficialGlassPresets.createBlankCustomConfig(current.preferredSizeTier).copy(
                appWidgetId = current.appWidgetId,
                cityId = current.cityId,
                useCoarseLocation = current.useCoarseLocation,
                temperatureUnit = current.temperatureUnit,
                weatherRefreshMinutes = current.weatherRefreshMinutes
            )
        }
    }

    fun resetCustomSlotsToBlank() {
        updateConfig { cfg ->
            cfg.copy(
                useCustomSlots = true,
                customSlots = emptyMap()
            )
        }
        _statusMessage.value = "Đã làm trống các vị trí khung — Chạm (+) để thêm lại thông tin"
    }

    fun fillSuggestedCustomSlots() {
        val tier = _activeConfig.value.preferredSizeTier
        val suggested = WidgetSlotBlueprints.suggestedSlotsForTier(tier)
        updateConfig { cfg ->
            cfg.copy(
                useCustomSlots = true,
                customSlots = suggested
            )
        }
        _statusMessage.value = "Đã điền gợi ý chuẩn cho kích thước ${tier.gridLabel}"
    }

    fun applyPresetToCurrentSession(preset: GlassPreset) {
        val current = _activeConfig.value
        val updated = preset.defaultConfig.copy(
            appWidgetId = current.appWidgetId,
            preferredSizeTier = current.preferredSizeTier,
            cityId = current.cityId,
            useCoarseLocation = current.useCoarseLocation,
            temperatureUnit = current.temperatureUnit,
            weatherRefreshMinutes = current.weatherRefreshMinutes
        )
        _activeConfig.value = updated
        _statusMessage.value = "Đã chọn mẫu kính ${preset.name}"
    }

    fun updateConfig(transform: (WidgetConfig) -> WidgetConfig) {
        val prev = _activeConfig.value
        val next = transform(prev)
        _activeConfig.value = next

        if (prev.cityId != next.cityId || prev.useCoarseLocation != next.useCoarseLocation) {
            fetchWeatherForCurrentConfig(forceNetwork = false)
        }
        if (prev.batteryMonitoringEnabled != next.batteryMonitoringEnabled) {
            refreshBatteryTelemetry()
        }
    }

    fun moveModuleOrder(module: WidgetModuleType, moveUp: Boolean) {
        val list = _activeConfig.value.moduleOrder.toMutableList()
        val idx = list.indexOf(module)
        if (idx < 0) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in list.indices) return
        val temp = list[idx]
        list[idx] = list[targetIdx]
        list[targetIdx] = temp
        updateConfig { it.copy(moduleOrder = list) }
    }

    fun resetToPresetDefault() {
        val current = _activeConfig.value
        val preset = allPresets.value.firstOrNull { it.id == current.presetId }
            ?: OfficialGlassPresets.CRYSTAL_CLEAR
        _activeConfig.value = preset.defaultConfig.copy(
            appWidgetId = current.appWidgetId,
            preferredSizeTier = current.preferredSizeTier
        )
        _statusMessage.value = "Đã khôi phục mặc định mẫu ${preset.name}"
    }

    fun fetchWeatherForCurrentConfig(forceNetwork: Boolean) {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            try {
                val cfg = _activeConfig.value
                val city = VietnamCityCatalog.findById(cfg.cityId)
                val info = weatherRepo.getWeather(
                    cityId = city.id,
                    useCoarseLocation = cfg.useCoarseLocation,
                    maxAgeMinutes = cfg.weatherRefreshMinutes,
                    forceRefresh = forceNetwork
                )
                _currentWeather.value = info
                if (forceNetwork) {
                    _statusMessage.value = if (info.hasData && !info.isFromOfflineCache) {
                        "Đã cập nhật thời tiết ${info.cityNameVi}: ${info.formatTemp(info.currentTempC, cfg.temperatureUnit)}"
                    } else {
                        info.errorMessageVi ?: "Đang dùng dữ liệu ngoại tuyến"
                    }
                    WidgetUpdateScheduler.updateAllActiveWidgets(appContext, forceWeatherNetwork = false)
                }
            } finally {
                _isRefreshingWeather.value = false
            }
        }
    }

    fun saveAndApplyToWidget(onAppliedComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val targetId = _editingAppWidgetId.value
            val cfg = _activeConfig.value.copy(appWidgetId = targetId)

            if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                configRepo.saveWidgetConfig(targetId, cfg)
                WidgetUpdateScheduler.updateSingleWidgetNow(
                    context = appContext,
                    appWidgetId = targetId,
                    fallbackTier = cfg.preferredSizeTier,
                    forceWeatherNetwork = false
                )
                _statusMessage.value = "Đã lưu & áp dụng lên Widget #$targetId"
            } else {
                configRepo.saveStudioDraft(cfg)
                val activeList = WidgetUpdateScheduler.getAllActiveWidgetSummaries(appContext)
                for (w in activeList) {
                    configRepo.saveWidgetConfig(
                        w.appWidgetId,
                        cfg.copy(appWidgetId = w.appWidgetId, preferredSizeTier = w.providerTier)
                    )
                }
                WidgetUpdateScheduler.updateAllActiveWidgets(appContext, forceWeatherNetwork = false)
                _statusMessage.value = if (activeList.isNotEmpty()) {
                    "Đã lưu & áp dụng cho ${activeList.size} widget trên màn hình chính"
                } else {
                    "Đã lưu cấu hình mẫu. Nhấn 'Thêm vào màn hình chính' để đặt widget."
                }
            }
            refreshActiveWidgetsList()
            onAppliedComplete?.invoke(targetId)
        }
    }

    fun requestPinToHomeScreen(tier: WidgetSizeTier = _activeConfig.value.preferredSizeTier) {
        viewModelScope.launch {
            val cfg = _activeConfig.value.copy(preferredSizeTier = tier)
            configRepo.saveStudioDraft(cfg)
            val pinned = WidgetUpdateScheduler.requestPinWidgetToHomeScreen(appContext, tier)
            _statusMessage.value = if (pinned) {
                "Đã gửi yêu cầu ghim Widget ${tier.gridLabel} (${tier.titleVi}) ra màn hình chính!"
            } else {
                "Đã lưu mẫu! Hãy nhấn giữ màn hình chính → chọn mục Widget → chọn LumiGlass ${tier.gridLabel}."
            }
        }
    }

    fun saveCurrentAsCustomPreset(customName: String) {
        viewModelScope.launch {
            val preset = configRepo.saveCustomPreset(customName, _activeConfig.value)
            _activeConfig.value = _activeConfig.value.copy(
                presetId = preset.id,
                presetName = preset.name
            )
            _statusMessage.value = "Đã lưu preset cá nhân: ${preset.name}"
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            configRepo.deleteCustomPreset(presetId)
            _statusMessage.value = "Đã xóa preset cá nhân"
        }
    }

    fun updateGlobalSettings(transform: (AppGlobalSettings) -> AppGlobalSettings) {
        viewModelScope.launch {
            configRepo.updateGlobalSettings(transform)
            val latest = globalSettings.value
            WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(
                appContext,
                latest.backgroundRefreshMinutes
            )
        }
    }

    fun syncAllWidgetsNow() {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            try {
                WidgetUpdateScheduler.updateAllActiveWidgets(appContext, forceWeatherNetwork = true)
                refreshActiveWidgetsList()
                fetchWeatherForCurrentConfig(forceNetwork = false)
                _statusMessage.value = "Đã đồng bộ tất cả widget ngoài màn hình chính"
            } finally {
                _isRefreshingWeather.value = false
            }
        }
    }

    fun checkForAppUpdate(silent: Boolean = false) {
        if (_appUpdateState.value.isChecking) return
        viewModelScope.launch {
            _appUpdateState.value = _appUpdateState.value.copy(
                isChecking = true,
                errorTextVi = null,
                statusTextVi = if (silent) null else "Đang kiểm tra bản cập nhật trên GitHub..."
            )
            val result = updateManager.checkForLatestRelease(
                owner = _appUpdateState.value.githubOwner,
                repo = _appUpdateState.value.githubRepo
            )
            result.fold(
                onSuccess = { release ->
                    val statusMsg = if (release.isNewerThanCurrent) {
                        "Đã có bản cập nhật mới ${release.tagName} (${release.formatFileSizeMb()})!"
                    } else {
                        "Bạn đang dùng phiên bản mới nhất (${release.tagName})"
                    }
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isChecking = false,
                        latestRelease = release,
                        statusTextVi = statusMsg,
                        errorTextVi = null
                    )
                    if (!silent || release.isNewerThanCurrent) {
                        _statusMessage.value = statusMsg
                    }
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Không thể kiểm tra cập nhật từ GitHub"
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isChecking = false,
                        statusTextVi = null,
                        errorTextVi = errMsg
                    )
                    if (!silent) {
                        _statusMessage.value = errMsg
                    }
                }
            )
        }
    }

    fun downloadAndInstallAppUpdate(release: GitHubReleaseInfo? = _appUpdateState.value.latestRelease) {
        val targetRelease = release ?: return
        if (_appUpdateState.value.isDownloading) return

        viewModelScope.launch {
            _appUpdateState.value = _appUpdateState.value.copy(
                isDownloading = true,
                downloadProgressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = targetRelease.apkSizeBytes,
                errorTextVi = null,
                statusTextVi = "Đang tải gói cập nhật ${targetRelease.tagName}..."
            )

            val downloadResult = updateManager.downloadReleaseApk(
                context = appContext,
                release = targetRelease
            ) { pct, downloaded, total ->
                _appUpdateState.value = _appUpdateState.value.copy(
                    downloadProgressPercent = pct,
                    downloadedBytes = downloaded,
                    totalBytes = total
                )
            }

            downloadResult.fold(
                onSuccess = { apkFile ->
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isDownloading = false,
                        downloadProgressPercent = 100,
                        downloadedApkFile = apkFile,
                        statusTextVi = "Tải hoàn tất (${apkFile.name}). Đang mở trình cài đặt..."
                    )
                    installAlreadyDownloadedApk()
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Lỗi khi tải file APK cập nhật"
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isDownloading = false,
                        errorTextVi = errMsg,
                        statusTextVi = null
                    )
                    _statusMessage.value = errMsg
                }
            )
        }
    }

    fun installAlreadyDownloadedApk() {
        val file = _appUpdateState.value.downloadedApkFile ?: return
        when (updateManager.triggerInstallApk(appContext, file)) {
            InstallLaunchResult.INSTALLER_LAUNCHED -> {
                _appUpdateState.value = _appUpdateState.value.copy(
                    statusTextVi = "Đã mở trình cài đặt Android cho bản cập nhật mới."
                )
            }
            InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                val msg = "Vui lòng bật 'Cho phép cài đặt ứng dụng từ nguồn này' rồi nhấn 'Cài đặt ngay'."
                _appUpdateState.value = _appUpdateState.value.copy(
                    statusTextVi = msg
                )
                _statusMessage.value = msg
            }
            InstallLaunchResult.ERROR -> {
                val msg = "Không thể mở trình cài đặt gói APK."
                _appUpdateState.value = _appUpdateState.value.copy(
                    errorTextVi = msg
                )
                _statusMessage.value = msg
            }
        }
    }

    fun performManualBackup(onDone: (String) -> Unit) {
        viewModelScope.launch {
            configRepo.saveStudioDraft(_activeConfig.value)
            val msg = "Đã sao lưu an toàn toàn bộ cấu hình widget và preset cá nhân!"
            _statusMessage.value = msg
            onDone(msg)
        }
    }

    fun performRestoreFromBackup(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            configRepo.ensureInitializedAndRestored()
            val draft = configRepo.getStudioDraft()
            _activeConfig.value = draft
            refreshActiveWidgetsList()
            WidgetUpdateScheduler.updateAllActiveWidgets(appContext, forceWeatherNetwork = false)
            val msg = "Đã khôi phục thành công cấu hình widget từ bản sao lưu dự phòng!"
            _statusMessage.value = msg
            onDone(true, msg)
        }
    }

    fun getLastBackupTimeFormatted(): String {
        val ts = configRepo.getLastBackupTimestamp() ?: return "Đã sao lưu gần đây"
        val sdf = java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }
}
