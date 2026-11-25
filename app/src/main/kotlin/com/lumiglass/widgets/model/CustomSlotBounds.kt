package com.lumiglass.widgets.model

import kotlin.math.roundToInt

data class SlotBoundsDp(val width: Int, val height: Int)

/** Mirrors the padding, gaps and weights in the widget_custom_* layouts. */
object CustomSlotBounds {
    fun forTier(tier: WidgetSizeTier, widthDp: Int, heightDp: Int): List<SlotBoundsDp> {
        val compact = tier == WidgetSizeTier.MINI_2X1 || tier == WidgetSizeTier.COMPACT_2X2
        val inset = if (compact) 20f else 24f
        val w = widthDp - inset
        val h = heightDp - inset
        fun slot(w: Float, h: Float) =
            SlotBoundsDp(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1))

        return when (tier) {
            WidgetSizeTier.MINI_2X1 -> listOf(slot((w - 8) * .52f, h), slot((w - 8) * .48f, h))
            WidgetSizeTier.COMPACT_2X2 -> {
                val row = (h - 8) * .5f
                listOf(slot(w, row), slot((w - 8) / 2, row), slot((w - 8) / 2, row))
            }
            WidgetSizeTier.ESSENTIAL_4X2 -> {
                val top = (h - 6) * .62f - 5
                val left = (w - 8) * .45f
                val right = (w - 8) * .55f
                listOf(
                    slot(left, top * .62f), slot(left, top * .38f),
                    slot(right, top * .42f), slot(right, top * .58f),
                    slot(w, (h - 6) * .38f)
                )
            }
            WidgetSizeTier.PREMIUM_4X3 -> {
                val rows = h - 10
                List(2) { slot((w - 8) / 2, rows * .35f) } +
                    List(3) { slot((w - 12) / 3, rows * .35f) } +
                    slot(w, rows * .30f)
            }
            WidgetSizeTier.DASHBOARD_4X4 -> {
                val rows = h - 15
                listOf(slot((w - 8) * .54f, rows * .26f), slot((w - 8) * .46f, rows * .26f),
                    slot(w, rows * .22f)) +
                    List(3) { slot((w - 12) / 3, rows * .28f) } +
                    slot(w, rows * .24f)
            }
        }
    }
}
