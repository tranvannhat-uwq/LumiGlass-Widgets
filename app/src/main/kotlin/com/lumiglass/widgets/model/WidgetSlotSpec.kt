package com.lumiglass.widgets.model

import kotlinx.serialization.Serializable

/**
 * Available information modules that a user can place into any standardized slot
 * inside the Custom Slot-Based Widget Builder ("Cá nhân hóa").
 */
@Serializable
enum class SlotContentType(
    val labelVi: String,
    val shortLabelVi: String,
    val descriptionVi: String,
    val categoryVi: String
) {
    EMPTY(
        labelVi = "Để trống",
        shortLabelVi = "Trống",
        descriptionVi = "Xóa nội dung khỏi vị trí ô này",
        categoryVi = "Tùy chọn"
    ),
    CLOCK(
        labelVi = "Đồng hồ kỹ thuật số",
        shortLabelVi = "Đồng hồ",
        descriptionVi = "Giờ & phút sắc nét cập nhật thời gian thực, tiết kiệm pin",
        categoryVi = "Thời gian"
    ),
    GREGORIAN_DATE(
        labelVi = "Thứ, ngày, tháng dương lịch",
        shortLabelVi = "Ngày dương",
        descriptionVi = "Hiển thị thứ trong tuần, ngày và tháng dương lịch Việt Nam",
        categoryVi = "Lịch"
    ),
    LUNAR_DATE(
        labelVi = "Lịch âm Việt Nam",
        shortLabelVi = "Âm lịch",
        descriptionVi = "Ngày tháng âm lịch, Can Chi và ngày lễ truyền thống",
        categoryVi = "Lịch"
    ),
    DATE_AND_LUNAR(
        labelVi = "Lịch Âm - Dương đầy đủ",
        shortLabelVi = "Âm & Dương",
        descriptionVi = "Gộp thứ ngày tháng dương lịch và âm lịch gọn gàng trong 1 ô",
        categoryVi = "Lịch"
    ),
    WEEK_CALENDAR(
        labelVi = "Thanh lịch tuần Âm - Dương",
        shortLabelVi = "Lịch tuần",
        descriptionVi = "Dải 5 hoặc 7 ngày liên tiếp kèm âm lịch, nổi bật hôm nay",
        categoryVi = "Lịch"
    ),
    WEATHER(
        labelVi = "Thời tiết trực tuyến",
        shortLabelVi = "Thời tiết",
        descriptionVi = "Biểu tượng kính, nhiệt độ, tình trạng trời, biên độ & xác suất mưa",
        categoryVi = "Môi trường"
    ),
    BATTERY_WITH_TEMP(
        labelVi = "Pin & Nhiệt độ pin",
        shortLabelVi = "Pin + Nhiệt độ",
        descriptionVi = "Vòng đo % pin, tia sét sạc ⚡ và chip nhiệt độ pin cảnh báo màu",
        categoryVi = "Hệ thống"
    ),
    STORAGE(
        labelVi = "Bộ nhớ trong (Storage)",
        shortLabelVi = "Bộ nhớ",
        descriptionVi = "% và dung lượng GB bộ nhớ trong với thanh đo neon cảnh báo",
        categoryVi = "Hệ thống"
    ),
    RAM(
        labelVi = "Bộ nhớ RAM thực tế",
        shortLabelVi = "RAM",
        descriptionVi = "% tải RAM, dung lượng GB đang dùng và màu cảnh báo quá tải",
        categoryVi = "Hệ thống"
    ),
    TELEMETRY_TRIO(
        labelVi = "Cụm 3 thẻ Neon (Pin • Bộ nhớ • RAM)",
        shortLabelVi = "Pin • Lưu trữ • RAM",
        descriptionVi = "Hiển thị đồng thời 3 thẻ kính neon phát sáng với màu cảnh báo",
        categoryVi = "Hệ thống"
    );

    companion object {
        val SELECTABLE_MODULES: List<SlotContentType> = listOf(
            CLOCK,
            GREGORIAN_DATE,
            LUNAR_DATE,
            DATE_AND_LUNAR,
            WEEK_CALENDAR,
            WEATHER,
            BATTERY_WITH_TEMP,
            STORAGE,
            RAM,
            TELEMETRY_TRIO
        )
    }
}

enum class SlotShapeClass {
    COMPACT_CARD,   // Square-ish / vertical card (ideal for Battery+Temp, Storage, RAM, Clock)
    WIDE_BANNER,    // Horizontal wide strip (ideal for Week Calendar, Weather, Date+Lunar, Trio)
    MEDIUM_TILE     // Balanced medium rectangle (ideal for Clock, Weather, Date, Telemetry)
}

data class WidgetSlotDefinition(
    val slotId: String,
    val titleVi: String,
    val shortHintVi: String,
    val shapeClass: SlotShapeClass,
    val recommendedTypes: List<SlotContentType>
)

/**
 * Standardized slot grids tailored strictly to each responsive [WidgetSizeTier].
 * Ensures information boxes maintain harmonious proportions regardless of widget size.
 */
object WidgetSlotBlueprints {

    const val SLOT_0 = "slot_0"
    const val SLOT_1 = "slot_1"
    const val SLOT_2 = "slot_2"
    const val SLOT_3 = "slot_3"
    const val SLOT_4 = "slot_4"
    const val SLOT_5 = "slot_5"
    const val SLOT_6 = "slot_6"

    fun slotsForTier(tier: WidgetSizeTier): List<WidgetSlotDefinition> {
        return when (tier) {
            WidgetSizeTier.MINI_2X1 -> listOf(
                WidgetSlotDefinition(
                    slotId = SLOT_0,
                    titleVi = "Ô bên trái (2×1)",
                    shortHintVi = "Đồng hồ / Pin",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.CLOCK, SlotContentType.WEATHER, SlotContentType.BATTERY_WITH_TEMP)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_1,
                    titleVi = "Ô bên phải (2×1)",
                    shortHintVi = "Lịch / Thời tiết",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.DATE_AND_LUNAR, SlotContentType.WEATHER, SlotContentType.BATTERY_WITH_TEMP, SlotContentType.RAM)
                )
            )

            WidgetSizeTier.COMPACT_2X2 -> listOf(
                WidgetSlotDefinition(
                    slotId = SLOT_0,
                    titleVi = "Ô trên toàn ngang (2×2)",
                    shortHintVi = "Đồng hồ / Thời tiết",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.CLOCK, SlotContentType.WEATHER, SlotContentType.DATE_AND_LUNAR)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_1,
                    titleVi = "Ô dưới trái (2×2)",
                    shortHintVi = "Pin / Thời tiết",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.BATTERY_WITH_TEMP, SlotContentType.WEATHER, SlotContentType.GREGORIAN_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_2,
                    titleVi = "Ô dưới phải (2×2)",
                    shortHintVi = "Bộ nhớ / RAM",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.STORAGE, SlotContentType.RAM, SlotContentType.LUNAR_DATE)
                )
            )

            WidgetSizeTier.ESSENTIAL_4X2 -> listOf(
                WidgetSlotDefinition(
                    slotId = SLOT_0,
                    titleVi = "Ô trên trái (4×2)",
                    shortHintVi = "Đồng hồ",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.CLOCK, SlotContentType.WEATHER)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_1,
                    titleVi = "Ô giữa trái (4×2)",
                    shortHintVi = "Thứ ngày / Âm lịch",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.DATE_AND_LUNAR, SlotContentType.GREGORIAN_DATE, SlotContentType.LUNAR_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_2,
                    titleVi = "Ô trên phải (4×2)",
                    shortHintVi = "Thời tiết",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.WEATHER, SlotContentType.DATE_AND_LUNAR, SlotContentType.LUNAR_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_3,
                    titleVi = "Ô giữa phải (4×2)",
                    shortHintVi = "Pin • Bộ nhớ • RAM",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.TELEMETRY_TRIO, SlotContentType.BATTERY_WITH_TEMP, SlotContentType.STORAGE, SlotContentType.RAM)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_4,
                    titleVi = "Thanh đáy toàn ngang (4×2)",
                    shortHintVi = "Lịch tuần Âm - Dương",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.WEEK_CALENDAR, SlotContentType.TELEMETRY_TRIO, SlotContentType.WEATHER)
                )
            )

            WidgetSizeTier.PREMIUM_4X3 -> listOf(
                WidgetSlotDefinition(
                    slotId = SLOT_0,
                    titleVi = "Ô đỉnh trái (4×3)",
                    shortHintVi = "Đồng hồ",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.CLOCK, SlotContentType.DATE_AND_LUNAR)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_1,
                    titleVi = "Ô đỉnh phải (4×3)",
                    shortHintVi = "Thời tiết / Ngày",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.WEATHER, SlotContentType.DATE_AND_LUNAR, SlotContentType.GREGORIAN_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_2,
                    titleVi = "Thẻ giữa trái (4×3)",
                    shortHintVi = "Pin + Nhiệt độ",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.BATTERY_WITH_TEMP, SlotContentType.WEATHER, SlotContentType.LUNAR_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_3,
                    titleVi = "Thẻ trung tâm (4×3)",
                    shortHintVi = "Bộ nhớ trong",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.STORAGE, SlotContentType.LUNAR_DATE, SlotContentType.GREGORIAN_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_4,
                    titleVi = "Thẻ giữa phải (4×3)",
                    shortHintVi = "RAM thực tế",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.RAM, SlotContentType.BATTERY_WITH_TEMP, SlotContentType.STORAGE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_5,
                    titleVi = "Thanh đáy toàn ngang (4×3)",
                    shortHintVi = "Lịch tuần Âm - Dương",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.WEEK_CALENDAR, SlotContentType.DATE_AND_LUNAR, SlotContentType.TELEMETRY_TRIO)
                )
            )

            WidgetSizeTier.DASHBOARD_4X4 -> listOf(
                WidgetSlotDefinition(
                    slotId = SLOT_0,
                    titleVi = "Ô đỉnh trái (4×4)",
                    shortHintVi = "Đồng hồ",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.CLOCK)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_1,
                    titleVi = "Ô đỉnh phải (4×4)",
                    shortHintVi = "Thứ ngày & Âm lịch",
                    shapeClass = SlotShapeClass.MEDIUM_TILE,
                    recommendedTypes = listOf(SlotContentType.DATE_AND_LUNAR, SlotContentType.GREGORIAN_DATE, SlotContentType.LUNAR_DATE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_2,
                    titleVi = "Băng thông tin giữa (4×4)",
                    shortHintVi = "Thời tiết trực tuyến",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.WEATHER, SlotContentType.LUNAR_DATE, SlotContentType.DATE_AND_LUNAR)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_3,
                    titleVi = "Thẻ hệ thống 1 (4×4)",
                    shortHintVi = "Pin + Nhiệt độ",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.BATTERY_WITH_TEMP)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_4,
                    titleVi = "Thẻ hệ thống 2 (4×4)",
                    shortHintVi = "Bộ nhớ trong",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.STORAGE)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_5,
                    titleVi = "Thẻ hệ thống 3 (4×4)",
                    shortHintVi = "RAM thực tế",
                    shapeClass = SlotShapeClass.COMPACT_CARD,
                    recommendedTypes = listOf(SlotContentType.RAM)
                ),
                WidgetSlotDefinition(
                    slotId = SLOT_6,
                    titleVi = "Thanh đáy toàn ngang (4×4)",
                    shortHintVi = "Lịch tuần Âm - Dương",
                    shapeClass = SlotShapeClass.WIDE_BANNER,
                    recommendedTypes = listOf(SlotContentType.WEEK_CALENDAR, SlotContentType.TELEMETRY_TRIO)
                )
            )
        }
    }

    /**
     * Suggested starter layout for users who tap "Điền gợi ý nhanh" in the Custom Builder.
     */
    fun suggestedSlotsForTier(tier: WidgetSizeTier): Map<String, SlotContentType> {
        return when (tier) {
            WidgetSizeTier.MINI_2X1 -> mapOf(
                SLOT_0 to SlotContentType.CLOCK,
                SLOT_1 to SlotContentType.WEATHER
            )
            WidgetSizeTier.COMPACT_2X2 -> mapOf(
                SLOT_0 to SlotContentType.CLOCK,
                SLOT_1 to SlotContentType.BATTERY_WITH_TEMP,
                SLOT_2 to SlotContentType.WEATHER
            )
            WidgetSizeTier.ESSENTIAL_4X2 -> mapOf(
                SLOT_0 to SlotContentType.CLOCK,
                SLOT_1 to SlotContentType.GREGORIAN_DATE,
                SLOT_2 to SlotContentType.WEATHER,
                SLOT_3 to SlotContentType.TELEMETRY_TRIO,
                SLOT_4 to SlotContentType.WEEK_CALENDAR
            )
            WidgetSizeTier.PREMIUM_4X3 -> mapOf(
                SLOT_0 to SlotContentType.CLOCK,
                SLOT_1 to SlotContentType.WEATHER,
                SLOT_2 to SlotContentType.BATTERY_WITH_TEMP,
                SLOT_3 to SlotContentType.STORAGE,
                SLOT_4 to SlotContentType.RAM,
                SLOT_5 to SlotContentType.WEEK_CALENDAR
            )
            WidgetSizeTier.DASHBOARD_4X4 -> mapOf(
                SLOT_0 to SlotContentType.CLOCK,
                SLOT_1 to SlotContentType.DATE_AND_LUNAR,
                SLOT_2 to SlotContentType.WEATHER,
                SLOT_3 to SlotContentType.BATTERY_WITH_TEMP,
                SLOT_4 to SlotContentType.STORAGE,
                SLOT_5 to SlotContentType.RAM,
                SLOT_6 to SlotContentType.WEEK_CALENDAR
            )
        }
    }

    fun slotContent(config: WidgetConfig, slotId: String): SlotContentType {
        return config.customSlots[slotId] ?: SlotContentType.EMPTY
    }

    fun filledCount(config: WidgetConfig, tier: WidgetSizeTier): Int {
        val validIds = slotsForTier(tier).map { it.slotId }.toSet()
        return config.customSlots.count { (k, v) -> k in validIds && v != SlotContentType.EMPTY }
    }

    /**
     * Intelligently migrates assigned modules when switching between widget size tiers
     * so wide elements (e.g., Week Calendar, Weather) always stay in wide slots and
     * compact telemetry cards expand/collapse cleanly without distortion.
     */
    fun remapSlotsForTier(
        existing: Map<String, SlotContentType>,
        targetTier: WidgetSizeTier
    ): Map<String, SlotContentType> {
        val chosen = existing.entries
            .sortedBy { it.key }
            .map { it.value }
            .filter { it != SlotContentType.EMPTY }
            .toMutableList()
        if (chosen.isEmpty()) return emptyMap()

        // Expand Trio into 3 separate cards when moving to 4x3 or 4x4
        if ((targetTier == WidgetSizeTier.PREMIUM_4X3 || targetTier == WidgetSizeTier.DASHBOARD_4X4) &&
            SlotContentType.TELEMETRY_TRIO in chosen
        ) {
            chosen.remove(SlotContentType.TELEMETRY_TRIO)
            if (SlotContentType.BATTERY_WITH_TEMP !in chosen) chosen.add(SlotContentType.BATTERY_WITH_TEMP)
            if (SlotContentType.STORAGE !in chosen) chosen.add(SlotContentType.STORAGE)
            if (SlotContentType.RAM !in chosen) chosen.add(SlotContentType.RAM)
        }

        // Collapse 3 separate telemetry cards into Trio when moving to 4x2
        if (targetTier == WidgetSizeTier.ESSENTIAL_4X2 &&
            SlotContentType.BATTERY_WITH_TEMP in chosen &&
            SlotContentType.STORAGE in chosen &&
            SlotContentType.RAM in chosen
        ) {
            chosen.remove(SlotContentType.BATTERY_WITH_TEMP)
            chosen.remove(SlotContentType.STORAGE)
            chosen.remove(SlotContentType.RAM)
            chosen.add(SlotContentType.TELEMETRY_TRIO)
        }

        val targetDefs = slotsForTier(targetTier)
        val result = linkedMapOf<String, SlotContentType>()
        val remaining = chosen.toMutableList()

        // Pass 1: Place modules into slots by slot priority order (best fit first)
        for (def in targetDefs) {
            val match = def.recommendedTypes.firstOrNull { it in remaining }
            if (match != null) {
                result[def.slotId] = match
                remaining.remove(match)
            }
        }

        // Pass 2: Place any remaining non-wide modules into remaining open slots
        for (def in targetDefs) {
            if (def.slotId in result) continue
            val fallback = remaining.firstOrNull { mod ->
                if (mod == SlotContentType.WEEK_CALENDAR || mod == SlotContentType.TELEMETRY_TRIO) {
                    def.shapeClass == SlotShapeClass.WIDE_BANNER
                } else {
                    true
                }
            }
            if (fallback != null) {
                result[def.slotId] = fallback
                remaining.remove(fallback)
            }
        }

        return result
    }
}
