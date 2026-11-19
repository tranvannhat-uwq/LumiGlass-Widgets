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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.widgetConfigDataStore by preferencesDataStore(name = "lumiglass_widget_configs")

@Serializable
data class AppGlobalSettings(
    val appThemeMode: GlassThemeMode = GlassThemeMode.AUTO,
    val defaultCityId: String = "hanoi",
    val defaultUseCoarseLocation: Boolean = false,
    val backgroundRefreshMinutes: Int = 60
)

class WidgetConfigRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
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
     * Gets configuration for a specific [appWidgetId].
     * If not yet saved, initializes from the current Studio Draft (or default preset) tailored to [fallbackTier].
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
                return saved.copy(appWidgetId = appWidgetId)
            }
        }
        val draft = getStudioDraft()
        val initial = draft.copy(
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
    }

    suspend fun saveStudioDraft(config: WidgetConfig) {
        val encoded = json.encodeToString(config)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_STUDIO_DRAFT] = encoded
        }
    }

    suspend fun getStudioDraft(): WidgetConfig {
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
        return newPreset
    }

    suspend fun deleteCustomPreset(presetId: String) {
        val updated = customPresetsFlow.first().filterNot { it.id == presetId }
        val encoded = json.encodeToString(updated)
        context.widgetConfigDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PRESETS] = encoded
        }
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
    }
}
