package com.lumiglass.widgets.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.lumiglass.widgets.data.config.WidgetConfigRepository
import com.lumiglass.widgets.model.WidgetSizeTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base [AppWidgetProvider] for all 5 LumiGlass responsive widget sizes.
 * Handles lifecycle events: creation/update, live launcher resizing (onAppWidgetOptionsChanged),
 * and resource cleanup on removal (onDeleted).
 */
abstract class BaseLumiWidgetProvider : AppWidgetProvider() {

    abstract val defaultSizeTier: WidgetSizeTier

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    WidgetUpdateScheduler.updateSingleWidgetNow(
                        context = context,
                        appWidgetId = appWidgetId,
                        fallbackTier = defaultSizeTier,
                        forceWeatherNetwork = false
                    )
                }
                WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                // Fast local re-render using cached weather so launcher resize is instantaneous and smooth
                WidgetUpdateScheduler.updateSingleWidgetNow(
                    context = context,
                    appWidgetId = appWidgetId,
                    fallbackTier = defaultSizeTier,
                    forceWeatherNetwork = false,
                    cacheOnlyForResize = true
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                WidgetConfigRepository(context).deleteWidgetConfigs(appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
