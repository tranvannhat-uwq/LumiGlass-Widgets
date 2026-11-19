package com.lumiglass.widgets.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lumiglass.widgets.data.battery.BatteryRepository
import com.lumiglass.widgets.data.config.WidgetConfigRepository
import com.lumiglass.widgets.data.datetime.DateTimeRepository
import com.lumiglass.widgets.data.weather.WeatherRepository
import com.lumiglass.widgets.model.WidgetConfig
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.renderer.WidgetRemoteViewsRenderer
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class ActiveWidgetSummary(
    val appWidgetId: Int,
    val providerTier: WidgetSizeTier,
    val widthDp: Int,
    val heightDp: Int,
    val config: WidgetConfig
)

/**
 * Coordinates widget updates, live data snapshot assembly, WorkManager periodic weather refresh,
 * midnight date/lunar rollover scheduling, and launcher widget pinning.
 */
object WidgetUpdateScheduler {

    const val ACTION_MIDNIGHT_ROLLOVER = "com.lumiglass.widgets.ACTION_MIDNIGHT_ROLLOVER"
    const val ACTION_WIDGET_PINNED_CALLBACK = "com.lumiglass.widgets.ACTION_WIDGET_PINNED_CALLBACK"
    private const val WORK_NAME_PERIODIC_SYNC = "lumiglass_periodic_widget_sync"

    fun providerComponentForTier(context: Context, tier: WidgetSizeTier): ComponentName {
        val cls = when (tier) {
            WidgetSizeTier.MINI_2X1 -> LumiWidget2x1Provider::class.java
            WidgetSizeTier.COMPACT_2X2 -> LumiWidget2x2Provider::class.java
            WidgetSizeTier.ESSENTIAL_4X2 -> LumiWidget4x2Provider::class.java
            WidgetSizeTier.PREMIUM_4X3 -> LumiWidget4x3Provider::class.java
            WidgetSizeTier.DASHBOARD_4X4 -> LumiWidget4x4Provider::class.java
        }
        return ComponentName(context, cls)
    }

    fun getAllActiveWidgetIdsByTier(context: Context): Map<WidgetSizeTier, IntArray> {
        val manager = AppWidgetManager.getInstance(context)
        return WidgetSizeTier.entries.associateWith { tier ->
            runCatching {
                manager.getAppWidgetIds(providerComponentForTier(context, tier))
            }.getOrDefault(IntArray(0))
        }
    }

    suspend fun getAllActiveWidgetSummaries(context: Context): List<ActiveWidgetSummary> {
        val manager = AppWidgetManager.getInstance(context)
        val configRepo = WidgetConfigRepository(context)
        val result = mutableListOf<ActiveWidgetSummary>()

        for ((tier, ids) in getAllActiveWidgetIdsByTier(context)) {
            for (id in ids) {
                val options: Bundle? = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
                val (wDp, hDp) = extractBoundsDp(options, tier)
                val cfg = configRepo.getWidgetConfig(id, tier)
                result.add(
                    ActiveWidgetSummary(
                        appWidgetId = id,
                        providerTier = tier,
                        widthDp = wDp,
                        heightDp = hDp,
                        config = cfg
                    )
                )
            }
        }
        return result.sortedBy { it.appWidgetId }
    }

    /**
     * Assembles a real-time [WidgetDataSnapshot] for a given [WidgetConfig].
     */
    suspend fun buildSnapshotForConfig(
        context: Context,
        config: WidgetConfig,
        forceWeatherNetwork: Boolean = false,
        cacheOnlyWeather: Boolean = false
    ): WidgetDataSnapshot {
        val zdt = DateTimeRepository.nowZoned(config.timezoneMode)
        val clockFormatted = DateTimeRepository.formatPreviewClock(zdt, config)
        val gregorianDateText = DateTimeRepository.formatGregorianDate(
            zdt = zdt,
            style = config.dateFormatStyle,
            useMonthInWords = config.useMonthInWords
        )
        val weekdayShort = DateTimeRepository.getVietnameseWeekdayShort(zdt.dayOfWeek)
        val lunarDate = DateTimeRepository.getCurrentLunarDate(zdt)

        val weatherRepo = WeatherRepository(context)
        val weatherInfo = if (!config.showWeather) {
            weatherRepo.getCachedWeatherOnly(config.cityId, config.useCoarseLocation)
        } else if (cacheOnlyWeather) {
            weatherRepo.getCachedWeatherOnly(config.cityId, config.useCoarseLocation)
        } else {
            weatherRepo.getWeather(
                cityId = config.cityId,
                useCoarseLocation = config.useCoarseLocation,
                maxAgeMinutes = config.weatherRefreshMinutes,
                forceRefresh = forceWeatherNetwork
            )
        }

        val batteryInfo = BatteryRepository.readCurrentBattery(
            context = context,
            enabled = config.batteryMonitoringEnabled
        )

        val ramInfo = com.lumiglass.widgets.data.ram.RamRepository.readCurrentRam(context)

        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemDark = uiMode == Configuration.UI_MODE_NIGHT_YES

        return WidgetDataSnapshot(
            previewTimePrimary = clockFormatted.primary,
            previewTimeHour = clockFormatted.hourPart,
            previewTimeMinute = clockFormatted.minutePart,
            previewAmPm = clockFormatted.amPmPart,
            gregorianDateText = gregorianDateText,
            gregorianWeekdayShort = weekdayShort,
            lunarDateInfo = lunarDate,
            weatherInfo = weatherInfo,
            batteryInfo = batteryInfo,
            isSystemDarkTheme = isSystemDark,
            weekCalendarColumns = DateTimeRepository.buildWeekCalendarColumns(zdt),
            ramInfo = ramInfo
        )
    }

    /**
     * Updates a single widget instance by [appWidgetId] on the Home Screen.
     */
    suspend fun updateSingleWidgetNow(
        context: Context,
        appWidgetId: Int,
        fallbackTier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2,
        forceWeatherNetwork: Boolean = false,
        cacheOnlyForResize: Boolean = false
    ) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        val configRepo = WidgetConfigRepository(context)
        val config = configRepo.getWidgetConfig(appWidgetId, fallbackTier)

        val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull()
        val (widthDp, heightDp) = extractBoundsDp(options, config.preferredSizeTier)

        val snapshot = buildSnapshotForConfig(
            context = context,
            config = config,
            forceWeatherNetwork = forceWeatherNetwork,
            cacheOnlyWeather = cacheOnlyForResize
        )

        val remoteViews = WidgetRemoteViewsRenderer.buildRemoteViews(
            context = context,
            appWidgetId = appWidgetId,
            widthDp = widthDp,
            heightDp = heightDp,
            config = config,
            snapshot = snapshot
        )
        runCatching {
            manager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * Updates all active LumiGlass widgets placed on the launcher.
     */
    suspend fun updateAllActiveWidgets(
        context: Context,
        forceWeatherNetwork: Boolean = false
    ) {
        val byTier = getAllActiveWidgetIdsByTier(context)
        for ((tier, ids) in byTier) {
            for (id in ids) {
                updateSingleWidgetNow(
                    context = context,
                    appWidgetId = id,
                    fallbackTier = tier,
                    forceWeatherNetwork = forceWeatherNetwork
                )
            }
        }
    }

    /**
     * Requests the launcher to pin a new widget of [tier] to the Home Screen (Android 8.0+).
     */
    fun requestPinWidgetToHomeScreen(
        context: Context,
        tier: WidgetSizeTier
    ): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            val provider = providerComponentForTier(context, tier)
            val callbackIntent = Intent(context, SystemEventReceiver::class.java).apply {
                action = ACTION_WIDGET_PINNED_CALLBACK
            }
            val successCallback = PendingIntent.getBroadcast(
                context,
                tier.ordinal + 500,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return runCatching {
                manager.requestPinAppWidget(provider, null, successCallback)
            }.getOrDefault(false)
        }
        return false
    }

    /**
     * Schedules periodic WorkManager background sync (30-60 mins) and an inexact midnight alarm
     * so Gregorian & Lunar dates roll over cleanly at 00:00 without continuous background services.
     */
    fun ensurePeriodicAndMidnightScheduled(
        context: Context,
        intervalMinutes: Int = 60
    ) {
        runCatching {
            val safeMinutes = intervalMinutes.coerceIn(30, 180).toLong()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<LumiWidgetRefreshWorker>(
                safeMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC_SYNC,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        scheduleNextMidnightRolloverAlarm(context)
    }

    private fun scheduleNextMidnightRolloverAlarm(context: Context) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val zone = ZoneId.systemDefault()
            val nextMidnightMillis = LocalDate.now(zone)
                .plusDays(1)
                .atStartOfDay(zone)
                .plusSeconds(3)
                .toInstant()
                .toEpochMilli()

            val intent = Intent(context, SystemEventReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_ROLLOVER
            }
            val pi = PendingIntent.getBroadcast(
                context,
                9901,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Use inexact setAndAllowWhileIdle (no SCHEDULE_EXACT_ALARM permission required)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC,
                nextMidnightMillis,
                pi
            )
        }
    }

    private fun extractBoundsDp(options: Bundle?, fallbackTier: WidgetSizeTier): Pair<Int, Int> {
        if (options == null) return fallbackTier.defaultWidthDp to fallbackTier.defaultHeightDp
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val w = if (minW > 40) minW else fallbackTier.defaultWidthDp
        val h = if (minH > 36) minH else fallbackTier.defaultHeightDp
        return w to h
    }
}

class LumiWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        WidgetUpdateScheduler.updateAllActiveWidgets(
            context = applicationContext,
            forceWeatherNetwork = false
        )
        WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(applicationContext)
        return Result.success()
    }
}
