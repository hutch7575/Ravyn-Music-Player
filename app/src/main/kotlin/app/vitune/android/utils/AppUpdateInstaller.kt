package app.vitune.android.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Explicit, user-initiated release update. Android's package installer handles
 * signature verification and user consent; the app never silently installs APKs.
 */
object AppUpdateInstaller {
    private const val MAX_APK_BYTES = 250L * 1024L * 1024L
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    suspend fun download(context: Context, url: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = URL(url)
                require(source.protocol == "https" && source.host == "github.com") {
                    "Updates must come from the official Ravyn GitHub releases"
                }
                require(source.path.startsWith("/hutch7575/Ravyn-Music-Player/releases/download/")) {
                    "Unrecognized update source"
                }

                val folder = File(context.cacheDir, "updates").apply { mkdirs() }
                val partial = File(folder, "ravyn.part")
                val destination = File(folder, "ravyn.apk")
                partial.delete()
                try {
                    val connection = (source.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        instanceFollowRedirects = true
                        setRequestProperty("Accept", "application/octet-stream")
                    }
                    try {
                        connection.connect()
                        require(connection.responseCode in 200..299) {
                            "GitHub returned HTTP ${connection.responseCode}"
                        }
                        require(connection.contentLengthLong <= MAX_APK_BYTES) {
                            "The update file is unexpectedly large"
                        }

                        var total = 0L
                        connection.inputStream.use { input ->
                            partial.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val bytes = input.read(buffer)
                                    if (bytes < 0) break
                                    total += bytes
                                    require(total <= MAX_APK_BYTES) { "Update download exceeded size limit" }
                                    output.write(buffer, 0, bytes)
                                }
                            }
                        }
                        require(total > 0L) { "The APK download was empty" }
                    } finally {
                        connection.disconnect()
                    }

                    @Suppress("DEPRECATION")
                    val packageInfo = context.packageManager.getPackageArchiveInfo(
                        partial.absolutePath,
                        PackageManager.GET_ACTIVITIES
                    ) ?: error("The downloaded file is not a valid Android APK")

                    require(packageInfo.packageName == context.packageName) {
                        "This APK cannot update the current app edition. Install the matching release."
                    }

                    val incoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }
                    val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
                    }
                    require(incoming > installed) { "The installed build is already this version or newer" }

                    if (destination.exists()) destination.delete()
                    check(partial.renameTo(destination)) { "Could not prepare the downloaded APK" }
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.update-files",
                        destination
                    )
                } finally {
                    partial.delete()
                }
            }
        }

    fun launchInstaller(context: Context, apkUri: Uri) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
