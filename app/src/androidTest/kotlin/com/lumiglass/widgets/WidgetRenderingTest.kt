package com.lumiglass.widgets

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lumiglass.widgets.data.datetime.DateTimeRepository
import com.lumiglass.widgets.model.BatteryInfo
import com.lumiglass.widgets.model.OfficialGlassPresets
import com.lumiglass.widgets.model.TimezoneMode
import com.lumiglass.widgets.model.WeatherInfo
import com.lumiglass.widgets.model.WidgetDataSnapshot
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.renderer.WidgetRemoteViewsRenderer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WidgetRenderingTest {
    @Test
    fun samplesInflateAndDrawAtLauncherSizesWithLargeSystemText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val context = app.createConfigurationContext(Configuration(app.resources.configuration).apply {
            fontScale = 1.5f
        })
        val date = DateTimeRepository.nowZoned(TimezoneMode.VIETNAM_UTC7)
        val snapshot = WidgetDataSnapshot(
            previewTimePrimary = "12:58:59",
            previewTimeHour = "12",
            previewTimeMinute = "58",
            previewAmPm = "CH",
            gregorianDateText = "Thứ Tư, 25 tháng Mười một, 2026",
            gregorianWeekdayShort = "T4",
            lunarDateInfo = DateTimeRepository.getCurrentLunarDate(date),
            weatherInfo = WeatherInfo(cityNameVi = "Thành phố Hồ Chí Minh", conditionTextVi = "Mưa rào và giông rải rác"),
            batteryInfo = BatteryInfo(),
            isSystemDarkTheme = true,
            weekCalendarColumns = DateTimeRepository.buildWeekCalendarColumns(date)
        )
        val density = context.resources.displayMetrics.density

        instrumentation.runOnMainSync {
            for (preset in OfficialGlassPresets.ALL) {
                for (tier in WidgetSizeTier.entries) {
                    val w = (tier.defaultWidthDp * .86f).roundToInt()
                    val h = (tier.defaultHeightDp * .86f).roundToInt()
                    val pxW = (w * density).roundToInt()
                    val pxH = (h * density).roundToInt()
                    val config = preset.configForTier(tier).copy(
                        use24HourFormat = false, showSeconds = true, textScale = 1.3f
                    )
                    val remote = WidgetRemoteViewsRenderer.buildRemoteViews(
                        context, 0, w, h, config, snapshot, preview = true
                    )
                    val host = FrameLayout(context)
                    host.addView(remote.apply(context, host), FrameLayout.LayoutParams(-1, -1))
                    host.measure(
                        View.MeasureSpec.makeMeasureSpec(pxW, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(pxH, View.MeasureSpec.EXACTLY)
                    )
                    host.layout(0, 0, pxW, pxH)
                    assertTrue(host.getChildAt(0) is ViewGroup)
                    assertTrue(host.getChildAt(0).width == pxW && host.getChildAt(0).height == pxH)
                    val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
                    try {
                        host.draw(Canvas(bitmap))
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }
}
