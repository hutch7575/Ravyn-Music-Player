package app.vitune.android.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.ui.ravyn.RavynIcon
import app.vitune.android.ui.ravyn.RavynLabel
import app.vitune.android.ui.ravyn.RavynStyle
import app.vitune.android.ui.modifiers.PinchDirection
import app.vitune.android.ui.modifiers.onSwipe
import app.vitune.android.ui.modifiers.pinchToToggle
import app.vitune.android.utils.FullScreenState
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import app.vitune.android.utils.windowState

/** Full-height, classic Ravyn lyrics reader. Existing fetch/sync logic lives in Lyrics. */
@Composable
fun LyricsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    FullScreenState(shown = PlayerPreferences.lyricsShowSystemBars)

    val player = LocalPlayerServiceBinder.current?.player ?: return@Dialog
    val (window, error) = windowState()

    LaunchedEffect(window, error) {
        if (window == null || error != null) currentOnDismiss()
    }
    window ?: return@Dialog

    AnimatedContent(
        targetState = window,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(180)),
                initialContentExit = fadeOut(tween(140)),
                sizeTransform = null
            )
        }, label = "Ravyn lyrics song"
    ) { currentWindow ->
        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
                .background(RavynStyle.background)
                .pinchToToggle(
                    direction = PinchDirection.In, threshold = 0.9f,
                    onPinch = { onDismiss() }
                )
                .onSwipe(
                    onSwipeLeft = { player.forceSeekToNext() },
                    onSwipeRight = {
                        player.seekToDefaultPosition()
                        player.forceSeekToPrevious()
                    }
                )
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                        .background(RavynStyle.panel)
                        .padding(start = 9.dp, end = 18.dp, top = 16.dp, bottom = 13.dp)
                ) {
                    RavynIcon(R.drawable.chevron_down, "Close lyrics", onDismiss)
                    Column(Modifier.weight(1f)) {
                        RavynLabel("LYRICS", size = 11, color = RavynStyle.secondary,
                            bold = true)
                        Spacer(Modifier.height(6.dp))
                        RavynLabel(currentWindow.mediaItem.mediaMetadata.title
                            ?.toString().orEmpty(), 17, bold = true)
                        RavynLabel(currentWindow.mediaItem.mediaMetadata.artist
                            ?.toString().orEmpty(), 12, color = RavynStyle.secondary)
                    }
                }
                Lyrics(
                    mediaId = currentWindow.mediaItem.mediaId,
                    isDisplayed = true,
                    onDismiss = {},
                    mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                    durationProvider = player::getDuration,
                    ensureSongInserted = { Database.insert(currentWindow.mediaItem) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onMenuLaunch = {},
                    shouldKeepScreenAwake = PlayerPreferences.lyricsKeepScreenAwake,
                    shouldUpdateLyrics = true,
                    ravynStyle = true
                )
            }
        }
    }
}
