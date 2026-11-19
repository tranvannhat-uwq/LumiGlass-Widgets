package com.lumiglass.widgets.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.lumiglass.widgets.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val htmlReleaseUrl: String,
    val isNewerThanCurrent: Boolean
) {
    fun formatFileSizeMb(): String {
        if (apkSizeBytes <= 0L) return "APK"
        val mb = apkSizeBytes.toDouble() / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.2f MB", mb)
    }
}

data class AppUpdateUiState(
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val githubOwner: String = AppUpdateManager.DEFAULT_GITHUB_OWNER,
    val githubRepo: String = AppUpdateManager.DEFAULT_GITHUB_REPO,
    val isChecking: Boolean = false,
    val latestRelease: GitHubReleaseInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedApkFile: File? = null,
    val statusTextVi: String? = null,
    val errorTextVi: String? = null
)

enum class InstallLaunchResult {
    INSTALLER_LAUNCHED,
    NEEDS_UNKNOWN_SOURCES_PERMISSION,
    ERROR
}

/**
 * In-App Update Engine powered by GitHub Releases API.
 * Checks for new versions, downloads the APK with live progress inside the app,
 * and triggers the Android Package Installer via FileProvider.
 */
class AppUpdateManager(
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_GITHUB_OWNER = "tranvannhat-uwq"
        const val DEFAULT_GITHUB_REPO = "LumiGlass-Widgets"
        const val DEFAULT_REPO_URL = "https://github.com/$DEFAULT_GITHUB_OWNER/$DEFAULT_GITHUB_REPO"

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        /**
         * Compares two semantic version strings (e.g. "1.0.0" vs "1.1.0" or "v1.0.1").
         * Returns true if [remoteVersion] is strictly newer than [currentVersion].
         */
        fun isRemoteVersionNewer(currentVersion: String, remoteVersion: String): Boolean {
            val cleanCurrent = cleanVersionParts(currentVersion)
            val cleanRemote = cleanVersionParts(remoteVersion)
            val maxLen = maxOf(cleanCurrent.size, cleanRemote.size)
            for (i in 0 until maxLen) {
                val c = cleanCurrent.getOrElse(i) { 0 }
                val r = cleanRemote.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        internal fun cleanVersionParts(raw: String): List<Int> {
            val stripped = raw.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
            return stripped.split(".")
                .mapNotNull { it.trim().toIntOrNull() }
                .ifEmpty { listOf(0) }
        }
    }

    /**
     * Queries GitHub Releases API for the latest release of [owner]/[repo].
     */
    suspend fun checkForLatestRelease(
        owner: String = DEFAULT_GITHUB_OWNER,
        repo: String = DEFAULT_GITHUB_REPO,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val latestUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(latestUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LumiGlassWidgets/${BuildConfig.VERSION_NAME} (Android)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                        ?: throw IllegalStateException("Phản hồi rỗng từ GitHub Releases")
                    val obj = json.parseToJsonElement(bodyStr).jsonObject
                    return@runCatching parseReleaseJsonObject(obj, currentVersionName, owner, repo)
                }

                // Fallback: check /releases list if /releases/latest returned 404 (e.g. pre-release)
                if (response.code == 404) {
                    val listUrl = "https://api.github.com/repos/$owner/$repo/releases"
                    val listReq = Request.Builder()
                        .url(listUrl)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "LumiGlassWidgets/${BuildConfig.VERSION_NAME} (Android)")
                        .get()
                        .build()
                    httpClient.newCall(listReq).execute().use { listResp ->
                        if (!listResp.isSuccessful) {
                            throw IllegalStateException("Không tìm thấy bản phát hành trên GitHub (HTTP ${listResp.code})")
                        }
                        val listBody = listResp.body?.string().orEmpty()
                        val arr: JsonArray = json.parseToJsonElement(listBody).jsonArray
                        val firstObj = arr.firstOrNull()?.jsonObject
                            ?: throw IllegalStateException("Kho lưu trữ GitHub chưa có bản phát hành (Release) nào")
                        return@runCatching parseReleaseJsonObject(firstObj, currentVersionName, owner, repo)
                    }
                }

                throw IllegalStateException("Lỗi kết nối GitHub API (HTTP ${response.code})")
            }
        }
    }

    internal fun parseReleaseJsonObject(
        obj: JsonObject,
        currentVersionName: String,
        owner: String = DEFAULT_GITHUB_OWNER,
        repo: String = DEFAULT_GITHUB_REPO
    ): GitHubReleaseInfo {
        val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: "v1.0.0"
        val cleanVersion = tagName.removePrefix("v").removePrefix("V")
        val title = obj["name"]?.jsonPrimitive?.content?.ifBlank { tagName } ?: tagName
        val notes = obj["body"]?.jsonPrimitive?.content?.ifBlank {
            "Bản cập nhật LumiGlass Widgets từ GitHub Releases."
        } ?: "Bản cập nhật LumiGlass Widgets từ GitHub Releases."
        val publishedAt = obj["published_at"]?.jsonPrimitive?.content?.take(10) ?: ""
        val htmlUrl = obj["html_url"]?.jsonPrimitive?.content
            ?: "https://github.com/$owner/$repo/releases"

        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize = 0L

        val assets = obj["assets"]?.jsonArray
        if (assets != null) {
            val apkCandidates = mutableListOf<Triple<String, String, Long>>()
            for (element in assets) {
                val assetObj = element.jsonObject
                val name = assetObj["name"]?.jsonPrimitive?.content.orEmpty()
                val downloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                val size = assetObj["size"]?.jsonPrimitive?.longOrNull ?: 0L
                if (name.endsWith(".apk", ignoreCase = true) && !downloadUrl.isNullOrBlank()) {
                    apkCandidates.add(Triple(name, downloadUrl, size))
                }
            }
            // Prioritize named releases (e.g. LumiGlass-Widgets-*.apk or *-release.apk) over generic debug
            val bestCandidate = apkCandidates.firstOrNull { (name, _, _) ->
                name.contains("LumiGlass", ignoreCase = true) && !name.contains("debug", ignoreCase = true)
            } ?: apkCandidates.firstOrNull { (name, _, _) ->
                name.contains("release", ignoreCase = true)
            } ?: apkCandidates.firstOrNull { (name, _, _) ->
                name.contains("LumiGlass", ignoreCase = true)
            } ?: apkCandidates.firstOrNull()

            if (bestCandidate != null) {
                apkName = bestCandidate.first
                apkUrl = bestCandidate.second
                apkSize = bestCandidate.third
            }
        }

        val isNewer = isRemoteVersionNewer(currentVersionName, cleanVersion)

        return GitHubReleaseInfo(
            tagName = tagName,
            versionName = cleanVersion,
            releaseTitle = title,
            releaseNotes = notes,
            publishedAt = publishedAt,
            apkDownloadUrl = apkUrl,
            apkFileName = apkName,
            apkSizeBytes = apkSize,
            htmlReleaseUrl = htmlUrl,
            isNewerThanCurrent = isNewer
        )
    }

    /**
     * Streams the APK file from GitHub Releases into the app's cache update folder
     * while emitting real-time progress callbacks.
     */
    suspend fun downloadReleaseApk(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadUrl = release.apkDownloadUrl
                ?: throw IllegalStateException("Bản phát hành ${release.tagName} chưa đính kèm file APK")

            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            // Clean old cached APKs before downloading
            updatesDir.listFiles()?.forEach { it.delete() }

            val safeFileName = (release.apkFileName ?: "LumiGlass-${release.versionName}.apk")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(updatesDir, safeFileName)

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "LumiGlassWidgets/${BuildConfig.VERSION_NAME} (Android)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Tải APK thất bại (HTTP ${response.code})")
                }
                val body = response.body
                    ?: throw IllegalStateException("Không nhận được dữ liệu APK")
                val contentLength = body.contentLength().let {
                    if (it > 0) it else release.apkSizeBytes
                }

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var read: Int
                        var lastReportedPct = -1

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = if (contentLength > 0) {
                                ((downloaded * 100L) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                50
                            }
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                onProgress(pct, downloaded, contentLength)
                            }
                        }
                        output.flush()
                    }
                }
            }
            onProgress(100, targetFile.length(), targetFile.length())
            targetFile
        }
    }

    /**
     * Launches the Android system package installer for the downloaded [apkFile].
     * Automatically checks and guides the user if REQUEST_INSTALL_PACKAGES permission is needed.
     */
    fun triggerInstallApk(context: Context, apkFile: File): InstallLaunchResult {
        return runCatching {
            if (!apkFile.exists()) return InstallLaunchResult.ERROR

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    val permIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permIntent)
                    return InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            InstallLaunchResult.INSTALLER_LAUNCHED
        }.getOrDefault(InstallLaunchResult.ERROR)
    }
}
