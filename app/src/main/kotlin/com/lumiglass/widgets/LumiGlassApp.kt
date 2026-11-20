package com.lumiglass.widgets

import android.app.Application
import com.lumiglass.widgets.data.config.WidgetConfigRepository
import com.lumiglass.widgets.widget.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LumiGlassApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    WidgetConfigRepository(this@LumiGlassApp).ensureInitializedAndRestored()
                }
            }
            WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(this)
        }
    }
}
