package com.lumiglass.widgets.data.ram

import android.app.ActivityManager
import android.content.Context
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class RamInfo(
    val totalBytes: Long = 0L,
    val availBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Int = 0,
    val isLowMemory: Boolean = false
) {
    fun formatUsedGb(): String = String.format(Locale.US, "%.1f", usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
    fun formatTotalGb(): String = String.format(Locale.US, "%.1f", totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
    fun formatAvailGb(): String = String.format(Locale.US, "%.1f", availBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
    fun formatShort(): String = "RAM $usedPercent%"
    fun formatMedium(): String = "RAM $usedPercent% • ${formatUsedGb()}/${formatTotalGb()}G"
    fun formatCompact(): String = formatMedium()
    fun formatDetailed(): String = "RAM: ${formatUsedGb()} / ${formatTotalGb()} GB ($usedPercent%)"
}

object RamRepository {
    fun readCurrentRam(context: Context): RamInfo {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (am != null) {
                am.getMemoryInfo(memInfo)
                val total = memInfo.totalMem.coerceAtLeast(1L)
                val avail = memInfo.availMem.coerceAtLeast(0L)
                val used = (total - avail).coerceAtLeast(0L)
                val pct = ((used.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
                RamInfo(
                    totalBytes = total,
                    availBytes = avail,
                    usedBytes = used,
                    usedPercent = pct,
                    isLowMemory = memInfo.lowMemory
                )
            } else {
                fallbackMockRam()
            }
        }.getOrElse { fallbackMockRam() }
    }

    private fun fallbackMockRam(): RamInfo {
        val total = 8L * 1024L * 1024L * 1024L
        val used = 4_800_000_000L
        return RamInfo(
            totalBytes = total,
            availBytes = total - used,
            usedBytes = used,
            usedPercent = 56,
            isLowMemory = false
        )
    }
}
