package com.lumiglass.widgets.data.weather

import com.lumiglass.widgets.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Client for Open-Meteo Forecast API (https://open-meteo.com/).
 * Requires no API key, adheres to Open-Meteo attribution & fair-use guidelines.
 * Never returns fabricated/fake temperature data on failure.
 */
class OpenMeteoApiClient(
    private val httpClient: OkHttpClient = defaultOkHttpClient(),
    private val baseUrl: String = "https://api.open-meteo.com/v1/forecast"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchWeather(
        cityId: String,
        cityNameVi: String,
        latitude: Double,
        longitude: Double
    ): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = baseUrl.toHttpUrl().newBuilder()
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter(
                    "current",
                    "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code"
                )
                .addQueryParameter(
                    "daily",
                    "temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                )
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "1")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LumiGlassWidgets/1.0 (Android; Open-Meteo CC-BY-4.0)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Open-Meteo HTTP ${response.code}")
                }
                val bodyStr = response.body?.string()
                    ?: throw IllegalStateException("Phản hồi rỗng từ máy chủ thời tiết")
                parseOpenMeteoJson(
                    rawJson = bodyStr,
                    cityId = cityId,
                    cityNameVi = cityNameVi
                )
            }
        }
    }

    internal fun parseOpenMeteoJson(
        rawJson: String,
        cityId: String,
        cityNameVi: String,
        nowMillis: Long = System.currentTimeMillis()
    ): WeatherInfo {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val current: JsonObject = root["current"]?.jsonObject
            ?: throw IllegalStateException("Thiếu trường 'current' từ Open-Meteo")

        val tempC = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull
            ?: throw IllegalStateException("Không có dữ liệu nhiệt độ hợp lệ")
        val humidity = current["relative_humidity_2m"]?.jsonPrimitive?.intOrNull
        val feelsLike = current["apparent_temperature"]?.jsonPrimitive?.doubleOrNull
        val isDayInt = current["is_day"]?.jsonPrimitive?.intOrNull ?: 1
        val wmoCode = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0

        val daily = root["daily"]?.jsonObject
        val maxTempC = daily?.get("temperature_2m_max")?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
        val minTempC = daily?.get("temperature_2m_min")?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
        val rainProb = daily?.get("precipitation_probability_max")?.jsonArray?.firstOrNull()?.jsonPrimitive?.intOrNull

        val isDay = isDayInt == 1
        val conditionVi = mapWmoCodeToVietnamese(wmoCode, isDay)

        return WeatherInfo(
            hasData = true,
            cityId = cityId,
            cityNameVi = cityNameVi,
            currentTempC = tempC,
            highTempC = maxTempC,
            lowTempC = minTempC,
            feelsLikeC = feelsLike,
            humidityPercent = humidity,
            rainProbabilityPercent = rainProb,
            wmoCode = wmoCode,
            conditionTextVi = conditionVi,
            isDay = isDay,
            updatedAtEpochMillis = nowMillis,
            isFromOfflineCache = false,
            errorMessageVi = null
        )
    }

    companion object {
        const val ATTRIBUTION_TEXT = "Dữ liệu thời tiết: Open-Meteo.com (CC BY 4.0)"

        private fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        fun mapWmoCodeToVietnamese(wmoCode: Int, isDay: Boolean): String {
            return when (wmoCode) {
                0 -> if (isDay) "Trời nắng quang" else "Đêm quang đãng"
                1 -> if (isDay) "Nắng nhẹ, ít mây" else "Đêm ít mây"
                2 -> "Có mây rải rác"
                3 -> "Nhiều mây"
                45, 48 -> "Sương mù"
                51 -> "Mưa phùn nhẹ"
                53 -> "Mưa phùn vừa"
                55, 56, 57 -> "Mưa phùn dày"
                61, 66 -> "Mưa nhỏ"
                63 -> "Mưa vừa"
                65, 67 -> "Mưa to"
                71, 73, 75, 77 -> "Tuyết rơi"
                80 -> "Mưa rào nhẹ"
                81 -> "Mưa rào vừa"
                82 -> "Mưa rào lớn"
                95 -> "Giông"
                96, 99 -> "Giông bão lớn"
                else -> "Có mây"
            }
        }
    }
}
