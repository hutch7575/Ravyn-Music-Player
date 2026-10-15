package app.vitune.android.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.vitune.android.BuildConfig
import app.vitune.android.MainActivity
import app.vitune.android.R
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.service.ServiceNotifications
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.DefaultDialog
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.AppUpdateInstaller
import app.vitune.android.utils.bold
import app.vitune.android.utils.center
import app.vitune.android.utils.hasPermission
import app.vitune.android.utils.pendingIntent
import app.vitune.android.utils.semiBold
import app.vitune.core.data.utils.Version
import app.vitune.core.data.utils.version
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isAtLeastAndroid13
import app.vitune.core.ui.utils.isCompositionLaunched
import app.vitune.providers.github.GitHub
import app.vitune.providers.github.models.Release
import app.vitune.providers.github.requests.releases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val VERSION_NAME = BuildConfig.VERSION_NAME.substringBeforeLast("-")
private const val REPO_OWNER = "hutch7575"
private const val REPO_NAME = "Ravyn-Music-Player"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val permission = Manifest.permission.POST_NOTIFICATIONS

class VersionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val WORK_TAG = "version_check_worker"

        fun upsert(context: Context, period: Duration?) = runCatching {
            val workManager = WorkManager.getInstance(context)

            if (period == null) {
                workManager.cancelAllWorkByTag(WORK_TAG)
                return@runCatching
            }

            val request = PeriodicWorkRequestBuilder<VersionCheckWorker>(period.toJavaDuration())
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED,
                        requiresBatteryNotLow = true
                    )
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                /* uniqueWorkName = */
                WORK_TAG,
                /* existingPeriodicWorkPolicy = */
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                /* periodicWork = */
                request
            )

            Unit
        }.also { it.exceptionOrNull()?.printStackTrace() }
    }

    override suspend fun doWork(): Result = with(applicationContext) {
        if (isAtLeastAndroid13 && !hasPermission(permission)) return Result.success()

        val result = withContext(Dispatchers.IO) {
            VERSION_NAME.version
                .getNewerVersion()
                .also { it?.exceptionOrNull()?.printStackTrace() }
        }

        result?.getOrNull()?.let { release ->
            ServiceNotifications.version.sendNotification(applicationContext) {
                this
                    .setSmallIcon(R.drawable.download)
                    .setContentTitle(getString(R.string.new_version_available))
                    .setContentText("Open Ravyn to download the new version")
                    .also {
                        it.setContentIntent(
                            pendingIntent(Intent(applicationContext, MainActivity::class.java))
                        )
                        it.setStyle(
                            NotificationCompat
                                .BigTextStyle(it)
                                .bigText(getString(R.string.new_version_available))
                        )
                    }
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }

        return when {
            result == null || result.isFailure -> Result.retry()
            result.isSuccess -> Result.success()
            else -> Result.failure() // Unreachable
        }
    }
}

private fun Release.installableApk(): Release.Asset? = assets.firstOrNull {
    it.state == Release.Asset.State.Uploaded &&
        it.name.endsWith(".apk", ignoreCase = true) &&
        !it.name.contains("debug", ignoreCase = true) &&
        !it.name.contains("nightly", ignoreCase = true)
}

private suspend fun Version.getNewerVersion(): Result<Release?>? = GitHub.releases(
    owner = REPO_OWNER,
    repo = REPO_NAME
)?.mapCatching { releases ->
    releases
        .filter { !it.draft && !it.preRelease && it.installableApk() != null }
        .sortedByDescending { it.publishedAt }
        .firstOrNull { it.tag.version > this }
}

@Route
@Composable
fun About() = SettingsCategoryScreen(
    title = "Ravyn",
    description = "Version ${BuildConfig.VERSION_NAME} · Based on ViTune · GPL-3.0"
) {
    val [_, typography] = LocalAppearance.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notificationPermission by remember(isCompositionLaunched()) {
        mutableStateOf(
            !isAtLeastAndroid13 || context.applicationContext.hasPermission(permission)
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationPermission = it }

    var checkingUpdates by rememberSaveable { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    SettingsGroup(title = "Updates") {
        SettingsEntry(
            title = "Check for updates",
            text = "Check Ravyn releases · ${BuildConfig.VERSION_NAME}",
            onClick = {
                checkingUpdates = true
                downloadError = null
            }
        )
        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.version_check),
            selectedValue = DataPreferences.versionCheckPeriod,
            onValueSelect = {
                DataPreferences.versionCheckPeriod = it
                if (isAtLeastAndroid13 && it.period != null && !notificationPermission) {
                    permissionLauncher.launch(permission)
                }
                VersionCheckWorker.upsert(context.applicationContext, it.period)
            },
            valueText = { it.displayName() }
        )
        if (BuildConfig.DEBUG) SettingsDescription(
            text = "This is a debug test build. It uses a separate app ID and signing key. " +
                "In-app APK installation will work with consistently signed production releases.",
            important = true
        )
    }

    SettingsGroup(title = "Project") {
        SettingsEntry(
            title = stringResource(R.string.github),
            text = "Source code and releases",
            onClick = { uriHandler.openUri("https://github.com/$REPO_OWNER/$REPO_NAME") }
        )
        SettingsEntry(
            title = stringResource(R.string.report_bug),
            text = "Report an issue with Ravyn",
            onClick = { uriHandler.openUri("https://github.com/$REPO_OWNER/$REPO_NAME/issues/new") }
        )
        SettingsDescription(
            text = "Ravyn is based on ViTune and retains the work and licenses " +
                "of its original contributors. Not affiliated with Google or YouTube."
        )
    }

    if (checkingUpdates) DefaultDialog(onDismiss = { if (!downloading) checkingUpdates = false }) {
        var newest by remember { mutableStateOf<Result<Release?>?>(null) }

        LaunchedEffect(Unit) {
            newest = withContext(Dispatchers.IO) {
                VERSION_NAME.version.getNewerVersion()
                    ?: Result.failure(IllegalStateException("Could not reach GitHub"))
            }
        }

        val release = newest?.getOrNull()
        if (release != null) {
            BasicText(
                text = "Ravyn ${release.tag} is available",
                style = typography.m.bold.center,
                modifier = Modifier.padding(16.dp)
            )
            if (BuildConfig.DEBUG) {
                BasicText(
                    text = "Test builds can't be updated using production APKs. " +
                        "Use GitHub Actions for test builds.",
                    style = typography.xs.center,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                SecondaryTextButton(
                    text = if (downloading) "Downloading update…" else "Download and install",
                    onClick = {
                        val asset = release.installableApk() ?: return@SecondaryTextButton
                        if (!AppUpdateInstaller.canInstall(context)) {
                            downloadError = "Allow updates from this app in Android settings, then try again."
                            runCatching { AppUpdateInstaller.requestInstallPermission(context) }
                            return@SecondaryTextButton
                        }
                        downloading = true
                        downloadError = null
                        scope.launch {
                            AppUpdateInstaller.download(
                                context.applicationContext,
                                asset.downloadUrl.toString()
                            ).onSuccess { uri ->
                                runCatching { AppUpdateInstaller.launchInstaller(context, uri) }
                                    .onFailure { downloadError = it.message ?: "Could not open Android installer" }
                            }.onFailure {
                                downloadError = it.message ?: "Unable to download the update"
                            }
                            downloading = false
                        }
                    },
                    enabled = !downloading
                )
            }
            SecondaryTextButton(
                text = stringResource(R.string.more_information),
                onClick = { uriHandler.openUri(release.frontendUrl.toString()) }
            )
        } else if (newest == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (newest?.isFailure == true) {
            BasicText(
                text = "Couldn't check GitHub for updates. Please try again later.",
                style = typography.xs.center,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            BasicText(
                text = "You're up to date. No newer Ravyn release is available yet.",
                style = typography.xs.center,
                modifier = Modifier.padding(16.dp)
            )
        }

        downloadError?.let {
            BasicText(
                text = it,
                style = typography.xs.center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
