package com.lumiglass.widgets.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives system events (boot completed, date/time/timezone change, midnight rollover,
 * power connected/disconnected) to refresh active widgets accurately without draining battery.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_LOCALE_CHANGED,
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED,
                    Intent.ACTION_BATTERY_LOW,
                    Intent.ACTION_BATTERY_OKAY,
                    WidgetUpdateScheduler.ACTION_MIDNIGHT_ROLLOVER,
                    WidgetUpdateScheduler.ACTION_WIDGET_PINNED_CALLBACK -> {
                        WidgetUpdateScheduler.updateAllActiveWidgets(
                            context = context,
                            forceWeatherNetwork = false
                        )
                        WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
