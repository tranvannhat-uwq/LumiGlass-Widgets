package com.lumiglass.widgets.model

import com.lumiglass.widgets.model.SlotContentType.*
import kotlinx.serialization.Serializable

@Serializable
data class GlassPreset(
    val id: String,
    val codeNumber: String,
    val name: String,
    val subtitleVi: String,
    val descriptionVi: String,
    val categoryTag: String,
    val isCustomUserPreset: Boolean = false,
    val defaultConfig: WidgetConfig,
    val recipe: WidgetRecipe? = null
) {
    fun configForTier(tier: WidgetSizeTier): WidgetConfig = defaultConfig.copy(
        preferredSizeTier = tier,
        customSlots = when {
            recipe != null -> recipe.slots(tier)
            defaultConfig.useCustomSlots && tier != defaultConfig.preferredSizeTier ->
                WidgetSlotBlueprints.remapSlotsForTier(defaultConfig.customSlots, tier)
            else -> defaultConfig.customSlots
        }
    )
}

@Serializable
enum class WidgetRecipe {
    FOCUS, ALMANAC, FORECAST, VITALS, DAILY, WEEK;

    fun slots(tier: WidgetSizeTier): Map<String, SlotContentType> {
        val slots = when (this) {
                FOCUS -> when (tier) {
                    WidgetSizeTier.MINI_2X1 -> listOf(CLOCK, GREGORIAN_DATE)
                    WidgetSizeTier.COMPACT_2X2 -> listOf(CLOCK, GREGORIAN_DATE, LUNAR_DATE)
                    WidgetSizeTier.ESSENTIAL_4X2 -> listOf(CLOCK, GREGORIAN_DATE, DATE_AND_LUNAR, EMPTY, WEEK_CALENDAR)
                    WidgetSizeTier.PREMIUM_4X3 -> listOf(CLOCK, DATE_AND_LUNAR, EMPTY, EMPTY, EMPTY, WEEK_CALENDAR)
                    WidgetSizeTier.DASHBOARD_4X4 -> listOf(CLOCK, DATE_AND_LUNAR, GREGORIAN_DATE, EMPTY, EMPTY, EMPTY, WEEK_CALENDAR)
                }
                ALMANAC -> when (tier) {
                    WidgetSizeTier.MINI_2X1 -> listOf(GREGORIAN_DATE, LUNAR_DATE)
                    WidgetSizeTier.COMPACT_2X2 -> listOf(WEEK_CALENDAR, GREGORIAN_DATE, LUNAR_DATE)
                    WidgetSizeTier.ESSENTIAL_4X2 -> listOf(GREGORIAN_DATE, LUNAR_DATE, EMPTY, DATE_AND_LUNAR, WEEK_CALENDAR)
                    WidgetSizeTier.PREMIUM_4X3 -> listOf(CLOCK, DATE_AND_LUNAR, GREGORIAN_DATE, LUNAR_DATE, EMPTY, WEEK_CALENDAR)
                    WidgetSizeTier.DASHBOARD_4X4 -> listOf(CLOCK, LUNAR_DATE, GREGORIAN_DATE, EMPTY, EMPTY, EMPTY, WEEK_CALENDAR)
                }
                FORECAST -> when (tier) {
                    WidgetSizeTier.MINI_2X1 -> listOf(WEATHER, CLOCK)
                    WidgetSizeTier.COMPACT_2X2 -> listOf(WEATHER, CLOCK, GREGORIAN_DATE)
                    WidgetSizeTier.ESSENTIAL_4X2 -> listOf(CLOCK, GREGORIAN_DATE, EMPTY, DATE_AND_LUNAR, WEATHER)
                    WidgetSizeTier.PREMIUM_4X3 -> listOf(WEATHER, CLOCK, GREGORIAN_DATE, LUNAR_DATE, EMPTY, WEEK_CALENDAR)
                    WidgetSizeTier.DASHBOARD_4X4 -> listOf(CLOCK, DATE_AND_LUNAR, WEATHER, EMPTY, EMPTY, EMPTY, WEEK_CALENDAR)
                }
                VITALS -> when (tier) {
                    WidgetSizeTier.MINI_2X1 -> listOf(BATTERY_WITH_TEMP, STORAGE)
                    WidgetSizeTier.COMPACT_2X2 -> listOf(BATTERY_WITH_TEMP, STORAGE, RAM)
                    WidgetSizeTier.ESSENTIAL_4X2 -> listOf(CLOCK, GREGORIAN_DATE, BATTERY_WITH_TEMP, STORAGE, TELEMETRY_TRIO)
                    WidgetSizeTier.PREMIUM_4X3 -> listOf(CLOCK, GREGORIAN_DATE, BATTERY_WITH_TEMP, STORAGE, RAM, DATE_AND_LUNAR)
                    WidgetSizeTier.DASHBOARD_4X4 -> listOf(CLOCK, DATE_AND_LUNAR, GREGORIAN_DATE, BATTERY_WITH_TEMP, STORAGE, RAM, WEEK_CALENDAR)
                }
                DAILY -> return WidgetSlotBlueprints.suggestedSlotsForTier(tier)
                WEEK -> when (tier) {
                    WidgetSizeTier.MINI_2X1 -> listOf(CLOCK, WEEK_CALENDAR)
                    WidgetSizeTier.COMPACT_2X2 -> listOf(WEEK_CALENDAR, CLOCK, WEATHER)
                    WidgetSizeTier.ESSENTIAL_4X2 -> listOf(CLOCK, LUNAR_DATE, GREGORIAN_DATE, WEATHER, WEEK_CALENDAR)
                    WidgetSizeTier.PREMIUM_4X3 -> listOf(CLOCK, DATE_AND_LUNAR, WEATHER, BATTERY_WITH_TEMP, STORAGE, WEEK_CALENDAR)
                    WidgetSizeTier.DASHBOARD_4X4 -> listOf(CLOCK, DATE_AND_LUNAR, WEATHER, BATTERY_WITH_TEMP, STORAGE, RAM, WEEK_CALENDAR)
                }
        }
        return WidgetSlotBlueprints.slotsForTier(tier).zip(slots)
            .filter { (_, content) -> content != SlotContentType.EMPTY }
            .associate { (slot, content) -> slot.slotId to content }
    }
}

object OfficialGlassPresets {

    val CRYSTAL_CLEAR = GlassPreset(
        id = "crystal_clear",
        codeNumber = "01",
        name = "Crystal Clear",
        subtitleVi = "Pha lê trong suốt tinh khiết",
        descriptionVi = "Kính trắng trong suốt, ánh sáng tự nhiên, viền bạc mảnh sắc nét, khúc xạ mép kính và chữ tự thích ứng.",
        categoryTag = "Trong suốt",
        defaultConfig = WidgetConfig(
            presetId = "crystal_clear",
            presetName = "Crystal Clear",
            glassTintColorHex = 0xFFFFFFFFL,
            glassSecondaryTintHex = 0xFFE2E8F0L,
            borderColorHex = 0xFFFFFFFFL,
            glassOpacity = 0.22f,
            frostAmount = 0.25f,
            borderLuminance = 0.88f,
            cornerRadiusDp = 28f,
            shadowIntensity = 0.42f,
            highlightIntensity = 0.78f,
            refractionIntensity = 0.70f,
            themeMode = GlassThemeMode.LIGHT,
            fontStyle = ClockFontStyle.THIN_ELEGANCE,
            fontWeight = FontWeightOption.MEDIUM,
            autoTextColor = false,
            customTextColorHex = 0xFFFFFFFFL,
            textShadowEnabled = true,
            textGlowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            batteryStyle = BatteryDisplayStyle.PILL
        )
    )

    val AURORA_GLASS = GlassPreset(
        id = "aurora_glass",
        codeNumber = "02",
        name = "Aurora Glass",
        subtitleVi = "Cực quang pastel đa sắc",
        descriptionVi = "Kính pastel xanh lam băng, tím nhạt và xanh ngọc với hiệu ứng ánh sáng mềm và gradient khúc xạ tinh tế.",
        categoryTag = "Sắc màu",
        defaultConfig = WidgetConfig(
            presetId = "aurora_glass",
            presetName = "Aurora Glass",
            glassTintColorHex = 0xFF7DD3FCL,
            glassSecondaryTintHex = 0xFFC084FCL,
            borderColorHex = 0xFF5EEAD4L,
            glassOpacity = 0.34f,
            frostAmount = 0.42f,
            borderLuminance = 0.85f,
            cornerRadiusDp = 28f,
            shadowIntensity = 0.50f,
            highlightIntensity = 0.82f,
            refractionIntensity = 0.75f,
            themeMode = GlassThemeMode.LIGHT,
            fontStyle = ClockFontStyle.MODERN_SANS,
            fontWeight = FontWeightOption.BOLD,
            autoTextColor = false,
            customTextColorHex = 0xFFFFFFFFL,
            textGlowEnabled = true,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            batteryStyle = BatteryDisplayStyle.CIRCLE
        )
    )

    val MIDNIGHT_GLASS = GlassPreset(
        id = "midnight_glass",
        codeNumber = "03",
        name = "Midnight Glass",
        subtitleVi = "Kính khói đêm huyền bí",
        descriptionVi = "Kính khói tối sâu, viền sáng lạnh ánh bạc-lam, chữ trắng nổi rõ trên bề mặt obsidian trong suốt.",
        categoryTag = "Tối",
        defaultConfig = WidgetConfig(
            presetId = "midnight_glass",
            presetName = "Midnight Glass",
            glassTintColorHex = 0xFF0F172AL,
            glassSecondaryTintHex = 0xFF1E293BL,
            borderColorHex = 0xFF94A3B8L,
            glassOpacity = 0.62f,
            frostAmount = 0.48f,
            borderLuminance = 0.74f,
            cornerRadiusDp = 26f,
            shadowIntensity = 0.68f,
            highlightIntensity = 0.55f,
            refractionIntensity = 0.58f,
            themeMode = GlassThemeMode.DARK,
            fontStyle = ClockFontStyle.BOLD_GEOMETRIC,
            fontWeight = FontWeightOption.BOLD,
            autoTextColor = false,
            customTextColorHex = 0xFFF8FAFCL,
            textGlowEnabled = false,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            batteryStyle = BatteryDisplayStyle.PROGRESS_BAR
        )
    )

    val FROSTED_PEARL = GlassPreset(
        id = "frosted_pearl",
        codeNumber = "04",
        name = "Frosted Pearl",
        subtitleVi = "Sương mờ ngọc trai thanh lịch",
        descriptionVi = "Kính trắng sương mờ cao cấp, bề mặt satin mịn màng, phản chiếu ánh ngọc ấm nhẹ nhàng.",
        categoryTag = "Sương mờ",
        defaultConfig = WidgetConfig(
            presetId = "frosted_pearl",
            presetName = "Frosted Pearl",
            glassTintColorHex = 0xFFF8FAFCL,
            glassSecondaryTintHex = 0xFFE2E8F0L,
            borderColorHex = 0xFFFFFFFFL,
            glassOpacity = 0.48f,
            frostAmount = 0.82f,
            borderLuminance = 0.90f,
            cornerRadiusDp = 30f,
            shadowIntensity = 0.38f,
            highlightIntensity = 0.72f,
            refractionIntensity = 0.64f,
            themeMode = GlassThemeMode.LIGHT,
            fontStyle = ClockFontStyle.CLASSIC_SERIF,
            fontWeight = FontWeightOption.MEDIUM,
            autoTextColor = true,
            customTextColorHex = 0xFF0F172AL,
            textGlowEnabled = false,
            textShadowEnabled = false,
            iconStyle = GlassIconStyle.OUTLINE,
            batteryStyle = BatteryDisplayStyle.PILL,
            layoutVariant = WidgetLayoutVariant.CENTERED_MINIMAL,
            contentAlignment = ContentAlignmentOption.CENTER
        )
    )

    val ADAPTIVE_GLASS = GlassPreset(
        id = "adaptive_glass",
        codeNumber = "05",
        name = "Adaptive Glass",
        subtitleVi = "Thích ứng thông minh Ngày & Đêm",
        descriptionVi = "Tự động chuyển đổi sắc thái kính, độ sâu bóng đổ và màu chữ theo chế độ Sáng/Tối và bảng màu hệ thống.",
        categoryTag = "Thích ứng",
        defaultConfig = WidgetConfig(
            presetId = "adaptive_glass",
            presetName = "Adaptive Glass",
            glassTintColorHex = 0xFF38BDF8L,
            glassSecondaryTintHex = 0xFF818CF8L,
            borderColorHex = 0xFFE0F2FEL,
            glassOpacity = 0.38f,
            frostAmount = 0.55f,
            borderLuminance = 0.80f,
            cornerRadiusDp = 26f,
            shadowIntensity = 0.52f,
            highlightIntensity = 0.68f,
            refractionIntensity = 0.62f,
            themeMode = GlassThemeMode.AUTO,
            fontStyle = ClockFontStyle.ROUNDED_GEOMETRIC,
            fontWeight = FontWeightOption.MEDIUM,
            autoTextColor = true,
            customTextColorHex = 0xFFFFFFFFL,
            textGlowEnabled = true,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            batteryStyle = BatteryDisplayStyle.PILL
        )
    )

    val AMBER_SUNSET_GLASS = GlassPreset(
        id = "amber_sunset_glass",
        codeNumber = "06",
        name = "Amber Sunset Glass",
        subtitleVi = "Kính hổ phách hoàng hôn (Honor/Magic)",
        descriptionVi = "Sắc cam ấm hổ phách sang trọng, font bo tròn hình học hiện đại, thẻ kính thời tiết MagicOS với trạng thái to rõ và dải nhiệt tinh tế.",
        categoryTag = "Sắc màu",
        defaultConfig = WidgetConfig(
            presetId = "amber_sunset_glass",
            presetName = "Amber Sunset Glass",
            glassTintColorHex = 0xFFC2410CL,
            glassSecondaryTintHex = 0xFFD97706L,
            borderColorHex = 0xFFFDE68AL,
            glassOpacity = 0.36f,
            frostAmount = 0.45f,
            borderLuminance = 0.86f,
            cornerRadiusDp = 28f,
            shadowIntensity = 0.48f,
            highlightIntensity = 0.80f,
            refractionIntensity = 0.72f,
            themeMode = GlassThemeMode.DARK,
            fontStyle = ClockFontStyle.ROUNDED_GEOMETRIC,
            fontWeight = FontWeightOption.BOLD,
            autoTextColor = false,
            customTextColorHex = 0xFFFFFFFFL,
            textGlowEnabled = true,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            batteryStyle = BatteryDisplayStyle.PILL,
            weatherDisplayStyle = WeatherDisplayStyle.MAGIC_CARD,
            temperatureSymbolStyle = TemperatureSymbolStyle.DEGREE_ONLY,
            showWeatherCardBackground = true,
            showLocationPin = true,
            deviceTagText = "Honor GT",
            showDeviceTag = true
        )
    )

    val NEON_CYBER_OBSIDIAN = GlassPreset(
        id = "neon_cyber_obsidian",
        codeNumber = "07",
        name = "Neon Cyber Obsidian",
        subtitleVi = "Obsidian đen neon đa sắc (như ảnh)",
        descriptionVi = "Bề mặt kính obsidian tối sâu, viền neon xanh lam phát sáng, bộ 3 thẻ kính Pin (vòng đo & nhiệt độ), Bộ nhớ trong (Storage) và RAM với thanh đo phát sáng và màu cảnh báo thông minh.",
        categoryTag = "Sắc màu",
        defaultConfig = WidgetConfig(
            presetId = "neon_cyber_obsidian",
            presetName = "Neon Cyber Obsidian",
            glassTintColorHex = 0xFF0B132BL,
            glassSecondaryTintHex = 0xFF1C2541L,
            borderColorHex = 0xFF38BDF8L,
            glassOpacity = 0.58f,
            frostAmount = 0.52f,
            borderLuminance = 0.92f,
            cornerRadiusDp = 30f,
            shadowIntensity = 0.65f,
            highlightIntensity = 0.88f,
            refractionIntensity = 0.78f,
            themeMode = GlassThemeMode.DARK,
            fontStyle = ClockFontStyle.ROUNDED_GEOMETRIC,
            fontWeight = FontWeightOption.BOLD,
            autoTextColor = false,
            customTextColorHex = 0xFFFFFFFFL,
            textGlowEnabled = true,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            weatherDisplayStyle = WeatherDisplayStyle.MAGIC_CARD,
            temperatureSymbolStyle = TemperatureSymbolStyle.DEGREE_ONLY,
            showWeatherCardBackground = true,
            telemetryDisplayStyle = TelemetryDisplayStyle.TRIO_GLASS_CARDS,
            dynamicAlertColors = true,
            showBatteryCard = true,
            showStorageCard = true,
            showRamCard = true,
            showBatteryPercent = true,
            showBatteryTemp = true,
            showRamStatus = true
        )
    )

    private fun collectionPreset(
        recipe: WidgetRecipe,
        name: String,
        category: String,
        description: String,
        tint: Long,
        shade: Long,
        accent: Long,
        theme: GlassThemeMode
    ): GlassPreset {
        val id = "collection_${recipe.name.lowercase()}"
        val tier = WidgetSizeTier.ESSENTIAL_4X2
        return GlassPreset(
            id = id,
            codeNumber = (recipe.ordinal + 8).toString().padStart(2, '0'),
            name = name,
            subtitleVi = category,
            descriptionVi = description,
            categoryTag = category,
            recipe = recipe,
            defaultConfig = createBlankCustomConfig(tier).copy(
                presetId = id,
                presetName = name,
                themeMode = theme,
                glassTintColorHex = tint,
                glassSecondaryTintHex = shade,
                borderColorHex = accent,
                glassOpacity = .84f,
                frostAmount = .12f,
                borderLuminance = .3f,
                highlightIntensity = .2f,
                refractionIntensity = .15f,
                shadowIntensity = .15f,
                textGlowEnabled = false,
                textShadowEnabled = false,
                showLunarDate = true,
                customSlots = recipe.slots(tier)
            )
        )
    }

    private val collections = listOf(
        collectionPreset(WidgetRecipe.FOCUS, "Đồng hồ Sương", "Đồng hồ",
            "Giờ và ngày trên nền ngà dịu mắt.", 0xFFF6F4ECL, 0xFFDADFDAL, 0xFF90A99BL, GlassThemeMode.LIGHT),
        collectionPreset(WidgetRecipe.FORECAST, "Bầu trời Lam", "Thời tiết",
            "Xem nhiệt độ, tình trạng trời và dự báo mưa.", 0xFF12324AL, 0xFF0B1B34L, 0xFF83DFFFL, GlassThemeMode.DARK),
        collectionPreset(WidgetRecipe.ALMANAC, "Lịch Ngọc", "Lịch",
            "Ngày âm, ngày dương và lịch tuần Việt Nam.", 0xFFE1F3E8L, 0xFFB9DCD1L, 0xFF3E9E82L, GlassThemeMode.LIGHT),
        collectionPreset(WidgetRecipe.VITALS, "Mạch Năng Lượng", "Thiết bị",
            "Theo dõi pin, nhiệt độ, lưu trữ và RAM.", 0xFF201B33L, 0xFF110F21L, 0xFFBAA6FFL, GlassThemeMode.DARK),
        collectionPreset(WidgetRecipe.DAILY, "Bento Hằng Ngày", "Tổng hợp",
            "Gom giờ, thời tiết và thông tin thiết bị vào các ô dễ đọc.", 0xFF223139L, 0xFF16262BL, 0xFFA9EDC7L, GlassThemeMode.DARK),
        collectionPreset(WidgetRecipe.WEEK, "Tuần Hổ Phách", "Lịch",
            "Lịch tuần, giờ và thời tiết trong bảng màu ấm.", 0xFFFFECD2L, 0xFFFFD8BAL, 0xFFB36540L, GlassThemeMode.LIGHT)
    )

    val ALL: List<GlassPreset> = collections + listOf(
        CRYSTAL_CLEAR,
        AURORA_GLASS,
        MIDNIGHT_GLASS,
        FROSTED_PEARL,
        ADAPTIVE_GLASS,
        AMBER_SUNSET_GLASS,
        NEON_CYBER_OBSIDIAN
    )

    fun findById(id: String): GlassPreset {
        return ALL.firstOrNull { it.id == id } ?: CRYSTAL_CLEAR
    }

    /**
     * Creates a clean blank Liquid Glass canvas with standardized empty slots (+)
     * ready for the user to personalize in the Custom Slot Builder.
     */
    fun createBlankCustomConfig(tier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2): WidgetConfig {
        return WidgetConfig(
            presetId = "custom_slots_canvas",
            presetName = "Cá nhân hóa kính lỏng",
            preferredSizeTier = tier,
            glassTintColorHex = 0xFF0F172AL,
            glassSecondaryTintHex = 0xFF1E293BL,
            borderColorHex = 0xFF7DD3FCL,
            glassOpacity = 0.34f,
            frostAmount = 0.44f,
            borderLuminance = 0.84f,
            cornerRadiusDp = 28f,
            shadowIntensity = 0.50f,
            highlightIntensity = 0.78f,
            refractionIntensity = 0.70f,
            themeMode = GlassThemeMode.AUTO,
            fontStyle = ClockFontStyle.ROUNDED_GEOMETRIC,
            fontWeight = FontWeightOption.BOLD,
            autoTextColor = true,
            customTextColorHex = 0xFFFFFFFFL,
            textGlowEnabled = true,
            textShadowEnabled = true,
            iconStyle = GlassIconStyle.GLASS,
            weatherDisplayStyle = WeatherDisplayStyle.MAGIC_CARD,
            temperatureSymbolStyle = TemperatureSymbolStyle.DEGREE_ONLY,
            dynamicAlertColors = true,
            showBatteryPercent = true,
            showBatteryTemp = true,
            showRamStatus = true,
            showStorageStatus = true,
            useCustomSlots = true,
            customSlots = emptyMap()
        )
    }
}
