package com.lumiglass.widgets.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lumiglass.widgets.model.WeatherInfo
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Context.weatherCacheDataStore by preferencesDataStore(name = "lumiglass_weather_cache")

class WeatherRepository(
    private val context: Context,
    private val apiClient: OpenMeteoApiClient = OpenMeteoApiClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun cacheKeyFor(locationKey: String) = stringPreferencesKey("weather_cache_$locationKey")

    /**
     * Gets weather for the requested configuration.
     * - Checks persistent cache first. If cache is fresh within [maxAgeMinutes] and [forceRefresh] is false, returns cache.
     * - Otherwise calls Open-Meteo API.
     * - On network error: returns the last cached WeatherInfo marked with [isFromOfflineCache = true],
     *   or an honest empty state (`hasData = false`, `currentTempC = null`) if no cache exists yet.
     *   NEVER returns fake temperatures.
     */
    suspend fun getWeather(
        cityId: String,
        useCoarseLocation: Boolean,
        maxAgeMinutes: Int = 30,
        forceRefresh: Boolean = false
    ): WeatherInfo {
        val targetCoords = resolveCoordinates(cityId, useCoarseLocation)
        val cacheKey = targetCoords.cacheKey
        val cached = readCachedWeather(cacheKey)
        val now = System.currentTimeMillis()
        val maxAgeMillis = maxAgeMinutes.coerceAtLeast(15) * 60_000L

        if (!forceRefresh && cached != null && cached.hasData && (now - cached.updatedAtEpochMillis) < maxAgeMillis) {
            return cached
        }

        val apiResult = apiClient.fetchWeather(
            cityId = targetCoords.cityId,
            cityNameVi = targetCoords.displayNameVi,
            latitude = targetCoords.latitude,
            longitude = targetCoords.longitude
        )

        return apiResult.fold(
            onSuccess = { fresh ->
                saveCachedWeather(cacheKey, fresh)
                fresh
            },
            onFailure = { err ->
                if (cached != null && cached.hasData) {
                    cached.copy(
                        isFromOfflineCache = true,
                        errorMessageVi = "Ngoại tuyến (${formatUpdatedTimeShort(cached.updatedAtEpochMillis)})"
                    )
                } else {
                    WeatherInfo(
                        hasData = false,
                        cityId = targetCoords.cityId,
                        cityNameVi = targetCoords.displayNameVi,
                        currentTempC = null,
                        conditionTextVi = "Chưa có dữ liệu thời tiết",
                        isFromOfflineCache = true,
                        errorMessageVi = err.message ?: "Không thể kết nối Open-Meteo"
                    )
                }
            }
        )
    }

    /**
     * Reads cached weather without triggering a network call (useful for instant synchronous/fast widget redraws).
     */
    suspend fun getCachedWeatherOnly(cityId: String, useCoarseLocation: Boolean): WeatherInfo {
        val targetCoords = resolveCoordinates(cityId, useCoarseLocation)
        val cached = readCachedWeather(targetCoords.cacheKey)
        if (cached != null && cached.hasData) {
            val ageMinutes = (System.currentTimeMillis() - cached.updatedAtEpochMillis) / 60_000L
            return if (ageMinutes > 90) {
                cached.copy(isFromOfflineCache = true)
            } else {
                cached
            }
        }
        return WeatherInfo(
            hasData = false,
            cityId = targetCoords.cityId,
            cityNameVi = targetCoords.displayNameVi,
            currentTempC = null,
            conditionTextVi = "Chạm để cập nhật thời tiết"
        )
    }

    suspend fun clearAllCache() {
        context.weatherCacheDataStore.edit { it.clear() }
    }

    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun resolveCoordinates(cityId: String, useCoarseLocation: Boolean): ResolvedLocationTarget {
        if (useCoarseLocation && hasCoarseLocationPermission()) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            for (provider in providers) {
                val loc = runCatching { locationManager?.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    val nearest = VietnamCityCatalog.findNearestCity(loc.latitude, loc.longitude)
                    return ResolvedLocationTarget(
                        cityId = nearest.id,
                        displayNameVi = "${nearest.nameVi} (Vị trí)",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        cacheKey = "loc_${(loc.latitude * 100).toInt()}_${(loc.longitude * 100).toInt()}"
                    )
                }
            }
        }
        val city = VietnamCityCatalog.findById(cityId)
        return ResolvedLocationTarget(
            cityId = city.id,
            displayNameVi = city.nameVi,
            latitude = city.latitude,
            longitude = city.longitude,
            cacheKey = "city_${city.id}"
        )
    }

    private suspend fun readCachedWeather(key: String): WeatherInfo? {
        return runCatching {
            val prefs = context.weatherCacheDataStore.data.first()
            val raw = prefs[cacheKeyFor(key)] ?: return null
            json.decodeFromString<WeatherInfo>(raw)
        }.getOrNull()
    }

    private suspend fun saveCachedWeather(key: String, info: WeatherInfo) {
        runCatching {
            val raw = json.encodeToString(info)
            context.weatherCacheDataStore.edit { prefs ->
                prefs[cacheKeyFor(key)] = raw
            }
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        fun formatUpdatedTimeShort(epochMillis: Long): String {
            if (epochMillis <= 0L) return "Chưa cập nhật"
            val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            return "Cập nhật ${TIME_FORMATTER.format(zdt)}"
        }
    }
}

private data class ResolvedLocationTarget(
    val cityId: String,
    val displayNameVi: String,
    val latitude: Double,
    val longitude: Double,
    val cacheKey: String
)
