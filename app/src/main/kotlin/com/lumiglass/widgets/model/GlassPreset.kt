package com.lumiglass.widgets.model

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
    val defaultConfig: WidgetConfig
)

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

    val ALL: List<GlassPreset> = listOf(
        CRYSTAL_CLEAR,
        AURORA_GLASS,
        MIDNIGHT_GLASS,
        FROSTED_PEARL,
        ADAPTIVE_GLASS,
        AMBER_SUNSET_GLASS
    )

    fun findById(id: String): GlassPreset {
        return ALL.firstOrNull { it.id == id } ?: CRYSTAL_CLEAR
    }
}
