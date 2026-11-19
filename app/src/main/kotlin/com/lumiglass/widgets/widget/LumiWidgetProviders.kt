package com.lumiglass.widgets.widget

import com.lumiglass.widgets.model.WidgetSizeTier

/** 2×1 – Mini Clock */
class LumiWidget2x1Provider : BaseLumiWidgetProvider() {
    override val defaultSizeTier: WidgetSizeTier = WidgetSizeTier.MINI_2X1
}

/** 2×2 – Compact Glass */
class LumiWidget2x2Provider : BaseLumiWidgetProvider() {
    override val defaultSizeTier: WidgetSizeTier = WidgetSizeTier.COMPACT_2X2
}

/** 4×2 – Essential Glass */
class LumiWidget4x2Provider : BaseLumiWidgetProvider() {
    override val defaultSizeTier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2
}

/** 4×3 – Premium Glass */
class LumiWidget4x3Provider : BaseLumiWidgetProvider() {
    override val defaultSizeTier: WidgetSizeTier = WidgetSizeTier.PREMIUM_4X3
}

/** 4×4 – Dashboard Glass */
class LumiWidget4x4Provider : BaseLumiWidgetProvider() {
    override val defaultSizeTier: WidgetSizeTier = WidgetSizeTier.DASHBOARD_4X4
}
