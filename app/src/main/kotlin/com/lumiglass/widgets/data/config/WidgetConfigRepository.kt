package com.lumiglass.widgets.data.config

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lumiglass.widgets.model.GlassPreset
import com.lumiglass.widgets.model.GlassThemeMode
import com.lumiglass.widgets.model.OfficialGlassPresets
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetSizeTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val Context.widgetConfigDataStore by preferencesDataStore(name = "lumiglass_widget_configs")

@Serializable
data class AppGlobalSettings(
    val appThemeMode: GlassThemeMode = GlassThemeMode.AUTO,
    val defaultCityId: String = "hanoi",
    val defaultUseCoarseLocation: Boolean = false,
    val backgroundRefreshMinutes: Int = 60
)

/**
 * Master multi-tier persistent backup container.
 * Saved to DataStore, persistent SharedPreferences, internal storage JSON, and external files JSON
 * to ensure user widget configurations and presets are NEVER lost across APK updates or reinstalls.
 */
@Serializable
data class MasterConfigBackup(
    val backupVersion: Int = 1,
    val timestampMillis: Long = System.currentTimeMillis(),
    val studioDraft: WidgetConfig? = null,
    val customPresets: List<GlassPreset> = emptyList(),
    val globalSettings: AppGlobalSettings? = null,
    val widgetConfigs: Map<String, WidgetConfig> = emptyMap()
)

class WidgetConfigRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val BACKUP_PREFS_NAME = "lumiglass_master_backup"
        private const val KEY_PREF_BACKUP_JSON = "master_backup_json"
        private const val BACKUP_FILENAME = "lumiglass_master_backup.json"

        private val KEY_STUDIO_DRAFT = stringPreferencesKey("studio_draft_config")
        private val KEY_CUSTOM_PRESETS = stringPreferencesKey("user_custom_presets_json")
        private val KEY_APP_THEME = stringPreferencesKey("global_app_theme")
        private val KEY_DEFAULT_CITY = stringPreferencesKey("global_default_city")
        private val KEY_DEFAULT_COARSE_LOC = booleanPreferencesKey("global_default_coarse_loc")
        private val KEY_REFRESH_MINUTES = intPreferencesKey("global_refresh_minutes")

        private fun widgetKey(appWidgetId: Int) = stringPreferencesKey("widget_cfg_$appWidgetId")
    }

    val globalSettingsFlow: Flow<AppGlobalSettings> = context.widgetConfigDataStore.data.map { prefs ->
        val themeStr = prefs[KEY_APP_THEME] ?: GlassThemeMode.AUTO.name
        val theme = runCatching { GlassThemeMode.valueOf(themeStr) }.getOrDefault(GlassThemeMode.AUTO)
        AppGlobalSettings(
            appThemeMode = theme,
            defaultCityId = prefs[KEY_DEFAULT_CITY] ?: "hanoi",
            defaultUseCoarseLocation = prefs[KEY_DEFAULT_COARSE_LOC] ?: false,
            backgroundRefreshMinutes = prefs[KEY_REFRESH_MINUTES] ?: 60
        )
    }

    val customPresetsFlow: Flow<List<GlassPreset>> = context.widgetConfigDataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_PRESETS]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<GlassPreset>>(raw) }.getOrDefault(emptyList())
        }
    }

    val studioDraftFlow: Flow<WidgetConfig> = context.widgetConfigDataStore.data.map { prefs ->
        val raw = prefs[KEY_STUDIO_DRAFT]
        if (raw.isNullOrBlank()) {
            OfficialGlassPresets.CRYSTAL_CLEAR.defaultConfig
        } else {
            runCatching { json.decodeFromString<WidgetConfig>(raw) }
                .getOrDefault(OfficialGlassPresets.CRYSTAL_CLEAR.defaultConfig)
        }
    }

    /**
     * Initializes self-healing auto-restoration from backup files if DataStore is empty.
     */
    suspend fun ensureInitializedAndRestored() = withContext(Dispatchers.IO) {
        val prefs = context.widgetConfigDataStore.data.first()
        val hasDraft = prefs.contains(KEY_STUDIO_DRAFT)
        val hasCustoms = prefs.contains(KEY_CUSTOM_PRESETS)
        if (!hasDraft && !hasCustoms) {
            // DataStore is empty (new install / update reset) -> Try auto-restoring from backups
            val backup = loadBestAvailableBackup()
            if (backup != null) {
                applyMasterBackup(backup)
            }
        }
    }

    /**
     * Gets configuration for a specific [appWidgetId].
     * If not yet saved, attempts to restore from master backup, or initializes from the current Studio Draft
     * (or the user's latest configured custom preset) tailored to [fallbackTier].
     */
    suspend fun getWidgetConfig(
        appWidgetId: Int,
        fallbackTier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2
    ): WidgetConfig {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return getStudioDraft().copy(preferredSizeTier = fallbackTier)
        }

        val prefs = context.widgetConfigDataStore.data.first()
        val raw = prefs[widgetKey(appWidgetId)]
        if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<WidgetConfig>(raw) }.getOrNull()?.let { saved ->
                return saved.copy(
                    appWidgetId = appWidgetId,
                    preferredSizeTier = fallbackTier
                )
            }
        }

        // Check if there is a backup for this widget ID or any recently saved widget config
        val backup = loadBestAvailableBackup()
        val savedInBackup = backup?.widgetConfigs?.get(appWidgetId.toString())
        if (savedInBackup != null) {
            val restored = savedInBackup.copy(
                appWidgetId = appWidgetId,
                preferredSizeTier = fallbackTier
            )
            saveWidgetConfig(appWidgetId, restored)
            return restored
        }

        // Inherit styling from either the Studio Draft or any existing customized widget/preset
        val baseDraft = getStudioDraft()
        val initial = baseDraft.copy(
            appWidgetId = appWidgetId,
            preferredSizeTier = fallbackTier
        )
        saveWidgetConfig(appWidgetId, initial)
        return initial
    }

    /**
     * Checks if an explicit configuration already exists for [appWidgetId].
     */
    suspend fun hasSavedConfigForWidget(appWidgetId: Int): Boolean {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val prefs = context.widgetConfigDataStore.data.first()
        return prefs.contains(widgetKey(appWidgetId))
    }

    suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        val normalized = config.copy(appWidgetId = appWidgetId)
        val encoded = json.encodeToString(normalized)
        context.widgetConfigDataStore.edit { prefs ->
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs[widgetKey(appWidgetId)] = encoded
            }
            prefs[KEY_STUDIO_DRAFT] = encoded
        }
        syncToPersistentBackups()
    }

    suspend fun saveStudioDraft(config: WidgetConfig) {
        val encoded = json.encodeToString(config)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_STUDIO_DRAFT] = encoded
        }
        syncToPersistentBackups()
    }

    suspend fun getStudioDraft(): WidgetConfig {
        ensureInitializedAndRestored()
        return studioDraftFlow.first()
    }

    /**
     * Cleans up stored configuration when a widget is deleted from the Home Screen.
     */
    suspend fun deleteWidgetConfigs(appWidgetIds: IntArray) {
        context.widgetConfigDataStore.edit { prefs ->
            for (id in appWidgetIds) {
                prefs.remove(widgetKey(id))
            }
        }
        syncToPersistentBackups()
    }

    /**
     * Saves a user's customized configuration as a reusable personal preset.
     */
    suspend fun saveCustomPreset(name: String, config: WidgetConfig): GlassPreset {
        val currentList = customPresetsFlow.first().toMutableList()
        val cleanName = name.trim().ifEmpty { "Kính Cá Nhân ${currentList.size + 1}" }
        val newId = "custom_${System.currentTimeMillis()}"
        val codeNum = (OfficialGlassPresets.ALL.size + currentList.size + 1).toString().padStart(2, '0')
        val presetConfig = config.copy(
            appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID,
            presetId = newId,
            presetName = cleanName
        )
        val newPreset = GlassPreset(
            id = newId,
            codeNumber = codeNum,
            name = cleanName,
            subtitleVi = "Preset cá nhân tùy chỉnh",
            descriptionVi = "Màu kính, độ trong suốt và bố cục được lưu từ Widget Studio.",
            categoryTag = "Cá nhân",
            isCustomUserPreset = true,
            defaultConfig = presetConfig
        )
        currentList.add(0, newPreset)
        val encoded = json.encodeToString(currentList)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PRESETS] = encoded
        }
        syncToPersistentBackups()
        return newPreset
    }

    suspend fun deleteCustomPreset(presetId: String) {
        val updated = customPresetsFlow.first().filterNot { it.id == presetId }
        val encoded = json.encodeToString(updated)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PRESETS] = encoded
        }
        syncToPersistentBackups()
    }

    suspend fun updateGlobalSettings(transform: (AppGlobalSettings) -> AppGlobalSettings) {
        val current = globalSettingsFlow.first()
        val updated = transform(current)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = updated.appThemeMode.name
            prefs[KEY_DEFAULT_CITY] = updated.defaultCityId
            prefs[KEY_DEFAULT_COARSE_LOC] = updated.defaultUseCoarseLocation
            prefs[KEY_REFRESH_MINUTES] = updated.backgroundRefreshMinutes
        }
        syncToPersistentBackups()
    }

    // =========================================================================
    // Multi-tier Persistent Backup & Self-Healing Restoration Subsystem
    // =========================================================================

    /**
     * Asynchronously creates a comprehensive master backup and writes it across:
     * 1. Persistent SharedPreferences
     * 2. Internal app files directory (`filesDir/lumiglass_master_backup.json`)
     * 3. External app files directory (`getExternalFilesDir(null)/lumiglass_master_backup.json`)
     */
    private suspend fun syncToPersistentBackups() = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.widgetConfigDataStore.data.first()
            val draft = prefs[KEY_STUDIO_DRAFT]?.let {
                runCatching { json.decodeFromString<WidgetConfig>(it) }.getOrNull()
            }
            val customs = prefs[KEY_CUSTOM_PRESETS]?.let {
                runCatching { json.decodeFromString<List<GlassPreset>>(it) }.getOrNull()
            } ?: emptyList()

            val widgetMap = mutableMapOf<String, WidgetConfig>()
            for (key in prefs.asMap().keys) {
                if (key.name.startsWith("widget_cfg_")) {
                    val rawVal = prefs[key] as? String ?: continue
                    runCatching { json.decodeFromString<WidgetConfig>(rawVal) }.getOrNull()?.let { cfg ->
                        val idStr = key.name.removePrefix("widget_cfg_")
                        widgetMap[idStr] = cfg
                    }
                }
            }

            val themeStr = prefs[KEY_APP_THEME] ?: GlassThemeMode.AUTO.name
            val theme = runCatching { GlassThemeMode.valueOf(themeStr) }.getOrDefault(GlassThemeMode.AUTO)
            val settings = AppGlobalSettings(
                appThemeMode = theme,
                defaultCityId = prefs[KEY_DEFAULT_CITY] ?: "hanoi",
                defaultUseCoarseLocation = prefs[KEY_DEFAULT_COARSE_LOC] ?: false,
                backgroundRefreshMinutes = prefs[KEY_REFRESH_MINUTES] ?: 60
            )

            val master = MasterConfigBackup(
                backupVersion = 1,
                timestampMillis = System.currentTimeMillis(),
                studioDraft = draft,
                customPresets = customs,
                globalSettings = settings,
                widgetConfigs = widgetMap
            )
            val encodedJson = json.encodeToString(master)

            // 1. SharedPreferences backup
            val sp = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(KEY_PREF_BACKUP_JSON, encodedJson).apply()

            // 2. Internal files backup
            runCatching {
                File(context.filesDir, BACKUP_FILENAME).writeText(encodedJson)
            }

            // 3. External app files backup (survives app updates & reinstalls if data kept)
            runCatching {
                context.getExternalFilesDir(null)?.let { extDir ->
                    File(extDir, BACKUP_FILENAME).writeText(encodedJson)
                }
            }
        }
    }

    /**
     * Looks across external storage, internal storage, and SharedPreferences to find the freshest backup.
     */
    private fun loadBestAvailableBackup(): MasterConfigBackup? {
        val candidates = mutableListOf<MasterConfigBackup>()

        // 1. External file backup
        runCatching {
            context.getExternalFilesDir(null)?.let { extDir ->
                val f = File(extDir, BACKUP_FILENAME)
                if (f.exists()) {
                    json.decodeFromString<MasterConfigBackup>(f.readText())
                } else null
            }
        }.getOrNull()?.let { candidates.add(it) }

        // 2. Internal file backup
        runCatching {
            val f = File(context.filesDir, BACKUP_FILENAME)
            if (f.exists()) {
                json.decodeFromString<MasterConfigBackup>(f.readText())
            } else null
        }.getOrNull()?.let { candidates.add(it) }

        // 3. SharedPreferences backup
        runCatching {
            val sp = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
            val raw = sp.getString(KEY_PREF_BACKUP_JSON, null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<MasterConfigBackup>(raw)
            } else null
        }.getOrNull()?.let { candidates.add(it) }

        return candidates.maxByOrNull { it.timestampMillis }
    }

    /**
     * Applies a [MasterConfigBackup] into the primary DataStore.
     */
    private suspend fun applyMasterBackup(backup: MasterConfigBackup) {
        context.widgetConfigDataStore.edit { prefs ->
            backup.studioDraft?.let {
                prefs[KEY_STUDIO_DRAFT] = json.encodeToString(it)
            }
            if (backup.customPresets.isNotEmpty()) {
                prefs[KEY_CUSTOM_PRESETS] = json.encodeToString(backup.customPresets)
            }
            backup.globalSettings?.let { gs ->
                prefs[KEY_APP_THEME] = gs.appThemeMode.name
                prefs[KEY_DEFAULT_CITY] = gs.defaultCityId
                prefs[KEY_DEFAULT_COARSE_LOC] = gs.defaultUseCoarseLocation
                prefs[KEY_REFRESH_MINUTES] = gs.backgroundRefreshMinutes
            }
            for ((idStr, cfg) in backup.widgetConfigs) {
                idStr.toIntOrNull()?.let { id ->
                    prefs[widgetKey(id)] = json.encodeToString(cfg)
                }
            }
        }
    }

    /**
     * Exports full user configurations as a JSON string for manual user backup.
     */
    suspend fun exportBackupJsonString(): String = withContext(Dispatchers.IO) {
        val backup = loadBestAvailableBackup() ?: MasterConfigBackup(
            studioDraft = getStudioDraft(),
            customPresets = customPresetsFlow.first(),
            globalSettings = globalSettingsFlow.first()
        )
        json.encodeToString(backup)
    }

    /**
     * Restores user configurations from an exported JSON string.
     */
    suspend fun restoreFromJsonString(rawJson: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val backup = json.decodeFromString<MasterConfigBackup>(rawJson)
            applyMasterBackup(backup)
            syncToPersistentBackups()
            true
        }.getOrDefault(false)
    }

    /**
     * Gets the latest backup timestamp formatted in Vietnamese, or null if none.
     */
    fun getLastBackupTimestamp(): Long? {
        return loadBestAvailableBackup()?.timestampMillis
    }
}
