package com.lumiglass.widgets.model

import kotlinx.serialization.Serializable

@Serializable
enum class GlassThemeMode(val labelVi: String) {
    AUTO("Tự động (Theo hệ thống)"),
    LIGHT("Kính sáng (Daylight)"),
    DARK("Kính tối (Midnight)")
}

@Serializable
enum class DateFormatStyle(val labelVi: String, val exampleVi: String) {
    FULL_VIETNAMESE("Đầy đủ tiếng Việt", "Thứ Năm, 19 tháng 11 năm 2026"),
    COMPACT_UPPER_DOT("Rút gọn hiện đại", "THỨ NĂM • 19/11"),
    SHORT_WEEKDAY_MONTH("Ngắn gọn tinh tế", "Th 5, 19 Thg 11"),
    WEEKDAY_NUMERIC_SLASH("Thứ kèm ngày/tháng/năm", "Thứ Năm, 19/11/2026"),
    NUMERIC_SLASH("Chỉ ngày/tháng/năm", "19/11/2026")
}

@Serializable
enum class BatteryDisplayStyle(val labelVi: String) {
    PILL("Viên thuốc kính (Pill)"),
    CIRCLE("Vòng tròn (Circle)"),
    PROGRESS_BAR("Thanh tiến trình (Bar)"),
    ICON_NUMBER("Biểu tượng + Số")
}

@Serializable
enum class WidgetModuleType(val labelVi: String) {
    CLOCK("Đồng hồ kỹ thuật số"),
    GREGORIAN_DATE("Thứ, ngày, tháng, năm"),
    LUNAR_DATE("Âm lịch Việt Nam"),
    WEATHER("Thời tiết"),
    BATTERY_PERCENT("Phần trăm pin"),
    BATTERY_TEMP("Nhiệt độ pin")
}

@Serializable
enum class WidgetLayoutVariant(val labelVi: String, val descriptionVi: String) {
    BALANCED_AUTO("Cân đối tự động", "Tự động phân bổ hài hòa theo kích thước widget"),
    SPLIT_HORIZON("Phân tách đôi", "Đồng hồ nổi bật bên trái, thông tin chi tiết bên phải"),
    CENTERED_MINIMAL("Tâm điểm tối giản", "Căn giữa thanh lịch, tập trung vào đồng hồ & lịch"),
    EDITORIAL_STACK("Phân tầng tạp chí", "Sắp xếp theo dòng chảy dọc với đường kẻ kính mảnh")
}

@Serializable
enum class ClockFontStyle(
    val labelVi: String,
    val androidFontFamily: String
) {
    MODERN_SANS("Modern Sans", "sans-serif"),
    THIN_ELEGANCE("Thin Elegance", "sans-serif-light"),
    BOLD_GEOMETRIC("Bold Geometric", "sans-serif-black"),
    CLASSIC_SERIF("Classic Luxury Serif", "serif"),
    TECH_MONO("Precision Mono", "monospace")
}

@Serializable
enum class FontWeightOption(val labelVi: String, val weightValue: Int) {
    LIGHT("Mảnh (Light)", 300),
    REGULAR("Thường (Regular)", 400),
    MEDIUM("Vừa (Medium)", 500),
    BOLD("Đậm (Bold)", 700)
}

@Serializable
enum class GlassIconStyle(val labelVi: String) {
    GLASS("Kính đa lớp (Liquid Glass)"),
    OUTLINE("Đường nét mảnh (Outline)"),
    FILLED("Đặc sắc nét (Filled)")
}

@Serializable
enum class ContentAlignmentOption(val labelVi: String) {
    START("Căn trái"),
    CENTER("Căn giữa"),
    END("Căn phải")
}

@Serializable
enum class TemperatureUnit(val symbol: String, val labelVi: String) {
    CELSIUS("°C", "Độ C (°C)"),
    FAHRENHEIT("°F", "Độ F (°F)")
}

@Serializable
enum class TimezoneMode(val labelVi: String) {
    VIETNAM_UTC7("Giờ Việt Nam (UTC+7)"),
    SYSTEM("Theo múi giờ thiết bị")
}

@Serializable
data class WidgetConfig(
    val appWidgetId: Int = 0,
    val presetId: String = "crystal_clear",
    val presetName: String = "Crystal Clear",
    val preferredSizeTier: WidgetSizeTier = WidgetSizeTier.ESSENTIAL_4X2,

    // 1. Tab Giao diện (Liquid Glass Optical Properties)
    val glassTintColorHex: Long = 0xFFFFFFFFL,
    val glassSecondaryTintHex: Long = 0xFFE0F2FEL,
    val borderColorHex: Long = 0xFFFFFFFFL,
    val glassOpacity: Float = 0.24f,          // 0.0f .. 1.0f (0% - 100%)
    val frostAmount: Float = 0.35f,           // 0.0f .. 1.0f (Simulated satin blur/frost)
    val borderLuminance: Float = 0.78f,       // 0.0f .. 1.0f (Specular rim brightness)
    val cornerRadiusDp: Float = 26f,          // 8f .. 38f
    val shadowIntensity: Float = 0.45f,       // 0.0f .. 1.0f
    val highlightIntensity: Float = 0.65f,    // 0.0f .. 1.0f
    val refractionIntensity: Float = 0.55f,   // 0.0f .. 1.0f (Inner rim optical refraction)
    val themeMode: GlassThemeMode = GlassThemeMode.AUTO,

    // 2. Tab Nội dung (Modules & Formats)
    val showClock: Boolean = true,
    val showGregorianDate: Boolean = true,
    val showLunarDate: Boolean = true,
    val showWeather: Boolean = true,
    val showBatteryPercent: Boolean = true,
    val showBatteryTemp: Boolean = true,

    val use24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val splitClockTwoLines: Boolean = false,

    val dateFormatStyle: DateFormatStyle = DateFormatStyle.FULL_VIETNAMESE,
    val useMonthInWords: Boolean = true,

    val showCanChi: Boolean = true,
    val showLunarHolidays: Boolean = true,

    val showHighLowTemp: Boolean = true,
    val showHumidity: Boolean = true,
    val showRainProbability: Boolean = false,
    val showFeelsLike: Boolean = false,

    val batteryStyle: BatteryDisplayStyle = BatteryDisplayStyle.PILL,
    val colorBatteryByLevel: Boolean = true,
    val warnBatteryTempByColor: Boolean = true,

    val moduleOrder: List<WidgetModuleType> = listOf(
        WidgetModuleType.CLOCK,
        WidgetModuleType.GREGORIAN_DATE,
        WidgetModuleType.LUNAR_DATE,
        WidgetModuleType.WEATHER,
        WidgetModuleType.BATTERY_PERCENT,
        WidgetModuleType.BATTERY_TEMP
    ),
    val layoutVariant: WidgetLayoutVariant = WidgetLayoutVariant.BALANCED_AUTO,

    // 3. Tab Chữ và biểu tượng (Typography & Icons)
    val fontStyle: ClockFontStyle = ClockFontStyle.MODERN_SANS,
    val fontWeight: FontWeightOption = FontWeightOption.MEDIUM,
    val textScale: Float = 1.0f,              // 0.75f .. 1.30f
    val letterSpacingEm: Float = 0.0f,        // -0.02f .. 0.12f
    val textGlowEnabled: Boolean = true,
    val textShadowEnabled: Boolean = true,
    val autoTextColor: Boolean = true,
    val customTextColorHex: Long = 0xFFFFFFFFL,
    val autoContrastEnsure: Boolean = true,
    val iconScale: Float = 1.0f,              // 0.80f .. 1.30f
    val iconStyle: GlassIconStyle = GlassIconStyle.GLASS,
    val contentAlignment: ContentAlignmentOption = ContentAlignmentOption.START,

    // 4. Tab Dữ liệu (Data & Refresh)
    val useCoarseLocation: Boolean = false,
    val cityId: String = "hanoi",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val weatherRefreshMinutes: Int = 60,
    val batteryMonitoringEnabled: Boolean = true,
    val timezoneMode: TimezoneMode = TimezoneMode.VIETNAM_UTC7
)
