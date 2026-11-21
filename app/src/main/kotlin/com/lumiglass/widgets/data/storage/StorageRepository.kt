package com.lumiglass.widgets.data.storage

import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Locale

@Serializable
data class StorageInfo(
    val totalBytes: Long = 0L,
    val availBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Int = 0
) {
    val hasData: Boolean get() = totalBytes > 0L

    fun formatUsedGb(): String {
        val gb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f", gb)
    }

    fun formatTotalGb(): String {
        val gb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 99.5) {
            String.format(Locale.US, "%.0f", gb)
        } else {
            String.format(Locale.US, "%.1f", gb)
        }
    }

    fun formatAvailGb(): String {
        val gb = availBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f", gb)
    }

    fun formatUsedAndTotalGb(): String = "${formatUsedGb()} / ${formatTotalGb()} GB"

    fun formatPercent(): String = "$usedPercent%"

    fun formatShort(): String = "Bộ nhớ $usedPercent%"

    fun formatMedium(): String = "Bộ nhớ $usedPercent% • ${formatUsedAndTotalGb()}"

    fun formatDetailed(): String = "Bộ nhớ: ${formatUsedGb()} / ${formatTotalGb()} GB ($usedPercent%)"
}

object StorageRepository {

    fun readCurrentStorage(): StorageInfo {
        return runCatching {
            val dataDir: File = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong

            val total = (totalBlocks * blockSize).coerceAtLeast(1L)
            val avail = (availBlocks * blockSize).coerceAtLeast(0L)
            val used = (total - avail).coerceAtLeast(0L)
            val pct = ((used.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)

            StorageInfo(
                totalBytes = total,
                availBytes = avail,
                usedBytes = used,
                usedPercent = pct
            )
        }.getOrElse { fallbackMockStorage() }
    }

    fun fallbackMockStorage(): StorageInfo {
        val total = 108L * 1024L * 1024L * 1024L
        val used = (74.2 * 1024.0 * 1024.0 * 1024.0).toLong()
        return StorageInfo(
            totalBytes = total,
            availBytes = total - used,
            usedBytes = used,
            usedPercent = 68
        )
    }
}
