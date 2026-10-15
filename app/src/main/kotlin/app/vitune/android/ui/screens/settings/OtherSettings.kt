package app.vitune.android.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import app.vitune.android.Database
import app.vitune.android.DatabaseDependency
import app.vitune.android.Dependencies
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.service.PlayerMediaBrowserService
import app.vitune.android.service.PrecacheService
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.logsRoute
import app.vitune.android.utils.isIgnoringBatteryOptimizations
import app.vitune.android.utils.toast
import app.vitune.core.ui.utils.isAtLeastAndroid6
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("BatteryLife")
@Route
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val scope = rememberCoroutineScope()

    val autoComponent = remember(context) {
        ComponentName(context, PlayerMediaBrowserService::class.java)
    }
    var androidAutoEnabled by remember(context) {
        mutableStateOf(
            context.packageManager.getComponentEnabledSetting(autoComponent) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        )
    }
    var batteryUnrestricted by remember(context) {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { batteryUnrestricted = context.isIgnoringBatteryOptimizations }

    val searchCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var upgrading by remember { mutableStateOf(false) }
    var reloading by remember { mutableStateOf(false) }

    SettingsCategoryScreen(title = stringResource(R.string.other)) {
        SettingsGroup(title = stringResource(R.string.service_lifetime)) {
            SettingsEntry(
                title = stringResource(R.string.ignore_battery_optimizations),
                text = if (batteryUnrestricted) {
                    stringResource(R.string.ignoring_battery_optimizations)
                } else {
                    stringResource(R.string.ignore_battery_optimizations_action)
                },
                onClick = {
                    if (isAtLeastAndroid6) {
                        try {
                            batteryLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        } catch (_: ActivityNotFoundException) {
                            runCatching {
                                batteryLauncher.launch(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            }.onFailure { context.toast(R.string.no_battery_optimization_settings_found) }
                        }
                    }
                },
                isEnabled = !batteryUnrestricted
            )
            SwitchSettingsEntry(
                title = stringResource(R.string.invincible_service),
                text = stringResource(R.string.invincible_service_description),
                isChecked = PlayerPreferences.isInvincibilityEnabled,
                onCheckedChange = { PlayerPreferences.isInvincibilityEnabled = it }
            )
        }

        SettingsGroup(title = stringResource(R.string.android_auto)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.android_auto),
                text = stringResource(R.string.android_auto_description),
                isChecked = androidAutoEnabled,
                onCheckedChange = { enabled ->
                    context.packageManager.setComponentEnabledSetting(
                        autoComponent,
                        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    androidAutoEnabled = enabled
                }
            )
        }

        SettingsGroup(title = stringResource(R.string.search_history)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.pause_search_history),
                text = stringResource(R.string.pause_search_history_description),
                isChecked = DataPreferences.pauseSearchHistory,
                onCheckedChange = { DataPreferences.pauseSearchHistory = it }
            )
            SettingsEntry(
                title = stringResource(R.string.clear_search_history),
                text = if (searchCount > 0) {
                    stringResource(R.string.format_clear_search_history_amount, searchCount)
                } else {
                    stringResource(R.string.empty_history)
                },
                onClick = { query(Database::clearQueries) },
                isEnabled = searchCount > 0
            )
        }

        SettingsGroup(title = stringResource(R.string.troubleshooting)) {
            SettingsEntry(
                title = stringResource(R.string.show_logs),
                text = "View and share playback diagnostics",
                onClick = { logsRoute.global() }
            )
            SettingsEntry(
                title = stringResource(R.string.upgrade_yt_dlp),
                text = "Refresh the audio extractor if songs stop loading",
                isEnabled = !upgrading,
                onClick = {
                    upgrading = true
                    scope.launch {
                        val success = runCatching {
                            withContext(Dispatchers.IO) { Dependencies.upgradeYoutubeDl() }
                        }.getOrDefault(false)
                        context.toast(if (success) R.string.yt_dlp_success else R.string.yt_dlp_fail)
                        upgrading = false
                    }
                }
            )
            SettingsEntry(
                title = stringResource(R.string.reload_app_internals),
                text = "Restart playback services without deleting your library",
                isEnabled = !reloading,
                onClick = {
                    reloading = true
                    scope.launch {
                        runCatching {
                            context.stopService(Intent(context, PrecacheService::class.java))
                            binder?.restartForegroundOrStop()
                            withContext(Dispatchers.IO) { DatabaseDependency.reload() }
                        }.onFailure { it.printStackTrace() }
                        reloading = false
                    }
                }
            )
        }
    }
}
