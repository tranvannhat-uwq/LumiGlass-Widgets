package com.lumiglass.widgets

import android.app.Application
import com.lumiglass.widgets.widget.WidgetUpdateScheduler

class LumiGlassApp : Application() {

    override fun onCreate() {
        super.onCreate()
        WidgetUpdateScheduler.ensurePeriodicAndMidnightScheduled(this)
    }
}
