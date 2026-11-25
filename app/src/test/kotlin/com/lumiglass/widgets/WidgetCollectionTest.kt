package com.lumiglass.widgets

import com.lumiglass.widgets.model.CustomSlotBounds
import com.lumiglass.widgets.model.OfficialGlassPresets
import com.lumiglass.widgets.model.SlotBoundsDp
import com.lumiglass.widgets.model.SlotContentType
import com.lumiglass.widgets.model.WidgetRecipe
import com.lumiglass.widgets.model.WidgetSizeTier
import com.lumiglass.widgets.model.WidgetSlotBlueprints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCollectionTest {
    @Test
    fun everyRecipeBuildsValidSlotsForEverySize() {
        val collection = OfficialGlassPresets.ALL.filter { it.recipe != null }
        assertEquals(WidgetRecipe.entries.size, collection.mapNotNull { it.recipe }.distinct().size)
        for (preset in collection) {
            for (size in WidgetSizeTier.entries) {
                val config = preset.configForTier(size)
                val allowedIds = WidgetSlotBlueprints.slotsForTier(size).map { it.slotId }
                assertEquals(size, config.preferredSizeTier)
                assertTrue(config.useCustomSlots)
                assertTrue(config.customSlots.isNotEmpty())
                assertTrue(allowedIds.containsAll(config.customSlots.keys))
                assertTrue(SlotContentType.EMPTY !in config.customSlots.values)
            }
        }
    }

    @Test
    fun actualFourByTwoDimensionsRespectGuttersAndColumns() {
        val slots = CustomSlotBounds.forTier(WidgetSizeTier.ESSENTIAL_4X2, 328, 142)
        assertEquals(listOf(
            SlotBoundsDp(133, 40),
            SlotBoundsDp(133, 24),
            SlotBoundsDp(163, 27),
            SlotBoundsDp(163, 37),
            SlotBoundsDp(304, 43)
        ), slots)
    }

    @Test
    fun tightBoundsKeepEachSlotInsideTheHost() {
        for (tier in WidgetSizeTier.entries) {
            for ((w, h) in listOf(120 to 44, tier.defaultWidthDp to tier.defaultHeightDp, 500 to 190)) {
                val slots = CustomSlotBounds.forTier(tier, w, h)
                assertEquals(WidgetSlotBlueprints.slotsForTier(tier).size, slots.size)
                assertTrue(slots.all { it.width in 1..w && it.height in 1..h })
            }
        }
    }

    @Test
    fun savedCustomPresetKeepsItsExactSlotAssignmentsAtItsOwnSize() {
        val original = OfficialGlassPresets.createBlankCustomConfig(WidgetSizeTier.DASHBOARD_4X4).copy(
            customSlots = mapOf(WidgetSlotBlueprints.SLOT_5 to SlotContentType.WEATHER)
        )
        val saved = OfficialGlassPresets.CRYSTAL_CLEAR.copy(defaultConfig = original, isCustomUserPreset = true)
        assertEquals(original, saved.configForTier(WidgetSizeTier.DASHBOARD_4X4))
        assertTrue(SlotContentType.WEATHER in saved.configForTier(WidgetSizeTier.MINI_2X1).customSlots.values)
    }
}
