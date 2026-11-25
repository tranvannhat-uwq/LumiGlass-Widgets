package com.lumiglass.widgets.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lumiglass.widgets.data.config.WidgetConfigRepository
import com.lumiglass.widgets.model.WidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Binds the exact draft the user confirmed even if the editor changed while the launcher was open. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdateScheduler.ACTION_WIDGET_PINNED_CALLBACK) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val payload = intent.getStringExtra(EXTRA_CONFIG) ?: return
        val config = runCatching { Json.decodeFromString<WidgetConfig>(payload) }.getOrNull() ?: return
        val tier = config.preferredSizeTier
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetInfo(id)?.provider != WidgetUpdateScheduler.providerComponentForTier(context, tier)) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetConfigRepository(context).saveWidgetConfig(id, config.copy(appWidgetId = id))
                WidgetUpdateScheduler.updateSingleWidgetNow(context, id, tier, cacheOnlyForResize = true)
            } catch (error: Exception) {
                Log.e("WidgetPinnedReceiver", "Could not bind pinned widget", error)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val EXTRA_CONFIG = "com.lumiglass.widgets.PINNED_CONFIG"
    }
}
