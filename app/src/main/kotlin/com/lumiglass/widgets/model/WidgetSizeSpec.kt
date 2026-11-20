package com.lumiglass.widgets.model

import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
enum class WidgetSizeTier(
    val id: String,
    val gridLabel: String,
    val titleVi: String,
    val descriptionVi: String,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
    val cellsWidth: Int,
    val cellsHeight: Int
) {
    MINI_2X1(
        id = "2x1",
        gridLabel = "2×1",
        titleVi = "Mini Clock",
        descriptionVi = "Chỉ hiển thị đồng hồ và ngày. Cực kỳ gọn gàng.",
        defaultWidthDp = 160,
        defaultHeightDp = 72,
        cellsWidth = 2,
        cellsHeight = 1
    ),
    COMPACT_2X2(
        id = "2x2",
        gridLabel = "2×2",
        titleVi = "Compact Glass",
        descriptionVi = "Đồng hồ lớn, ngày dương, ngày âm và thời tiết hoặc pin.",
        defaultWidthDp = 160,
        defaultHeightDp = 160,
        cellsWidth = 2,
        cellsHeight = 2
    ),
    ESSENTIAL_4X2(
        id = "4x2",
        gridLabel = "4×2",
        titleVi = "Essential Glass",
        descriptionVi = "Đồng hồ lớn bên trái; thời tiết, âm lịch và pin bên phải.",
        defaultWidthDp = 328,
        defaultHeightDp = 142,
        cellsWidth = 4,
        cellsHeight = 2
    ),
    PREMIUM_4X3(
        id = "4x3",
        gridLabel = "4×3",
        titleVi = "Premium Glass",
        descriptionVi = "Đồng hồ, ngày tháng, âm lịch Can Chi, thời tiết và pin cân đối cao cấp.",
        defaultWidthDp = 328,
        defaultHeightDp = 212,
        cellsWidth = 4,
        cellsHeight = 3
    ),
    DASHBOARD_4X4(
        id = "4x4",
        gridLabel = "4×4",
        titleVi = "Dashboard Glass",
        descriptionVi = "Hiển thị đầy đủ dữ liệu, phân vùng kính tinh tế, tùy biến sâu.",
        defaultWidthDp = 328,
        defaultHeightDp = 284,
        cellsWidth = 4,
        cellsHeight = 4
    )
}

data class ResolvedWidgetMetrics(
    val effectiveTier: WidgetSizeTier,
    val widthDp: Int,
    val heightDp: Int,
    val scaleFactor: Float,
    val contentPaddingDp: Int,
    val isCompactHeight: Boolean
)

object ResponsiveLayoutResolver {

    /**
     * Resolves the optimal layout tier and responsive scaling metrics based on actual launcher
     * dimensions (in dp), preventing text clipping or module overlap on resize.
     */
    fun resolve(
        widthDp: Int,
        heightDp: Int,
        preferredTier: WidgetSizeTier
    ): ResolvedWidgetMetrics {
        val safeW = if (widthDp > 40) widthDp else preferredTier.defaultWidthDp
        val safeH = if (heightDp > 36) heightDp else preferredTier.defaultHeightDp

        val tier = when {
            safeH < 108 -> {
                if (safeW < 235) WidgetSizeTier.MINI_2X1 else WidgetSizeTier.ESSENTIAL_4X2
            }
            safeH < 192 -> {
                if (safeW < 235) WidgetSizeTier.COMPACT_2X2 else WidgetSizeTier.ESSENTIAL_4X2
            }
            safeH < 268 -> {
                if (safeW < 235) WidgetSizeTier.COMPACT_2X2 else WidgetSizeTier.PREMIUM_4X3
            }
            else -> {
                if (safeW < 235) WidgetSizeTier.COMPACT_2X2 else WidgetSizeTier.DASHBOARD_4X4
            }
        }

        val widthRatio = safeW.toFloat() / tier.defaultWidthDp.toFloat()
        val heightRatio = safeH.toFloat() / tier.defaultHeightDp.toFloat()
        val rawScale = min(widthRatio, heightRatio)
        val clampedScale = rawScale.coerceIn(0.76f, 1.24f)

        val paddingDp = when (tier) {
            WidgetSizeTier.MINI_2X1 -> (10 * clampedScale).toInt().coerceIn(6, 14)
            WidgetSizeTier.COMPACT_2X2 -> (14 * clampedScale).toInt().coerceIn(10, 18)
            WidgetSizeTier.ESSENTIAL_4X2 -> (16 * clampedScale).toInt().coerceIn(10, 20)
            WidgetSizeTier.PREMIUM_4X3 -> (18 * clampedScale).toInt().coerceIn(12, 22)
            WidgetSizeTier.DASHBOARD_4X4 -> (20 * clampedScale).toInt().coerceIn(14, 24)
        }

        return ResolvedWidgetMetrics(
            effectiveTier = tier,
            widthDp = safeW,
            heightDp = safeH,
            scaleFactor = clampedScale,
            contentPaddingDp = paddingDp,
            isCompactHeight = safeH < 120
        )
    }
}
