package app.vitune.android.ui.screens.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import app.vitune.android.R
import app.vitune.android.models.ui.UiMedia
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.service.PlayerService
import app.vitune.android.ui.components.SeekBar
import app.vitune.android.ui.components.themed.availabilityLabel
import app.vitune.android.ui.ravyn.RavynStyle
import app.vitune.android.utils.DisposableListener
import app.vitune.android.utils.TrackAvailability
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** One set of controls and visual tokens for both orientations. */
@Composable
fun RavynNowPlaying(
    media: UiMedia?,
    artworkAccent: Color,
    topInset: Dp,
    binder: PlayerService.Binder?,
    title: String,
    artist: String,
    likedAt: Long?,
    setLikedAt: (Long?) -> Unit,
    shouldBePlaying: Boolean,
    isBuffering: Boolean,
    availability: TrackAvailability,
    position: Long,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    isShowingLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    albumTitle: String?,
    onArtistClick: (() -> Unit)?,
    thumbnailContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) = BoxWithConstraints(modifier.fillMaxSize()) {
    val landscape = maxWidth > maxHeight
    val compact = maxHeight < 615.dp
    val animatedColor by animateColorAsState(artworkAccent, tween(500), label = "Artwork colour")
    val accent = lerp(animatedColor, Color.White, 0.08f)
    val player = binder?.player
    var shuffled by remember(player) { mutableStateOf(player?.shuffleModeEnabled == true) }
    player.DisposableListener {
        object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffled = shuffleModeEnabled
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        lerp(RavynStyle.panel, animatedColor, 0.25f), RavynStyle.background
    ))))

    val songInfo: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                PlayerText(title, if (compact) 19 else 22, bold = true)
                Spacer(Modifier.height(4.dp))
                PlayerText(artist, 13, RavynStyle.secondary)
            }
            RavynPlayerIcon(
                if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                if (likedAt == null) "Like song" else "Unlike song",
                { setLikedAt(if (likedAt == null) System.currentTimeMillis() else null) },
                tint = if (likedAt == null) RavynStyle.text else accent
            )
        }
        // Reserve one line so buffering does not move the controls or artwork.
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth().height(24.dp)) {
            if (isBuffering) {
                CircularProgressIndicator(Modifier.size(12.dp), color = accent, strokeWidth = 1.5.dp)
                PlayerText(stringResource(R.string.loading_audio), 11, RavynStyle.secondary)
            } else {
                PlayerText(availabilityLabel(availability), 11, RavynStyle.secondary)
            }
        }
    }
    val progress: @Composable () -> Unit = {
        val validDuration = media != null && media.duration > 0L && media.duration != C.TIME_UNSET
        Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
            if (binder != null && media != null && validDuration) SeekBar(
                binder = binder, position = position, media = media,
                color = accent, backgroundColor = Color.White.copy(alpha = 0.24f),
                style = PlayerPreferences.SeekBarStyle.Static, scrubberRadius = 5.dp
            ) else Box(Modifier.fillMaxWidth().height(3.dp)
                .background(Color.White.copy(alpha = 0.24f), CircleShape))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RavynTimestamp(position)
            if (validDuration) RavynTimestamp(media!!.duration)
            else PlayerText("—:—", 12, RavynStyle.secondary)
        }
    }
    val transport: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().height(66.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            RavynPlayerIcon(R.drawable.shuffle, "Shuffle",
                { player?.shuffleModeEnabled = !shuffled },
                if (shuffled) accent else RavynStyle.text,
                state = if (shuffled) "On" else "Off")
            RavynPlayerIcon(R.drawable.play_skip_back, "Previous", { player?.forceSeekToPrevious() })
            Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White)
                .semantics {
                    contentDescription = if (shouldBePlaying) "Pause" else "Play"
                    stateDescription = if (isBuffering) "Loading audio" else if (shouldBePlaying) "Playing" else "Paused"
                }.clickable(role = Role.Button) {
                    if (player != null) {
                        if (shouldBePlaying) player.pause()
                        else if (binder.playbackFailure != null || player.playerError != null)
                            binder.retryPlayback()
                        else {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
                            player.play()
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                // Keep a recognisable pause target while the surrounding ring shows loading.
                if (isBuffering) CircularProgressIndicator(Modifier.size(49.dp),
                    color = Color(0xFF111315), strokeWidth = 2.dp)
                Image(painterResource(if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null, colorFilter = ColorFilter.tint(Color(0xFF111315)),
                    modifier = Modifier.size(29.dp))
            }
            RavynPlayerIcon(R.drawable.play_skip_forward, "Next", { player?.forceSeekToNext() })
            RavynPlayerIcon(
                if (PlayerPreferences.trackLoopEnabled) R.drawable.repeat_on else R.drawable.repeat,
                "Repeat track", { PlayerPreferences.trackLoopEnabled = !PlayerPreferences.trackLoopEnabled },
                if (PlayerPreferences.trackLoopEnabled) accent else RavynStyle.text,
                state = if (PlayerPreferences.trackLoopEnabled) "On" else "Off")
        }
    }
    val actions: @Composable () -> Unit = {
        if (landscape) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RavynPlayerCard(stringResource(R.string.lyrics_action), R.drawable.musical_notes,
                accent, isShowingLyrics, onToggleLyrics, Modifier.weight(1f))
            RavynPlayerCard("UP NEXT", R.drawable.enqueue, accent, false, onShowQueue, Modifier.weight(1f))
        } else {
            RavynPlayerCard(stringResource(if (isShowingLyrics) R.string.hide_lyrics_action else R.string.lyrics_action),
                R.drawable.musical_notes, accent, isShowingLyrics, onToggleLyrics)
            Spacer(Modifier.height(6.dp))
            RavynPlayerCard("UP NEXT", R.drawable.enqueue, accent, false, onShowQueue)
        }
    }
    val failure: @Composable () -> Unit = {
        if (binder?.playbackFailure != null) {
            PlayerText(stringResource(R.string.playback_stalled), 12, RavynStyle.secondary, lines = 3)
            RavynPlayerCard(stringResource(R.string.retry_playback), R.drawable.sync,
                accent, true, { binder.retryPlayback() })
        }
    }

    Column(Modifier.fillMaxSize().padding(start = if (landscape) 16.dp else 22.dp,
        end = if (landscape) 16.dp else 22.dp, top = topInset)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            RavynPlayerIcon(R.drawable.chevron_down, "Close player", onCollapse)
            Spacer(Modifier.weight(1f))
            PlayerText("NOW PLAYING", 12, bold = true)
            Spacer(Modifier.weight(1f))
            RavynPlayerIcon(R.drawable.ellipsis_horizontal, "More options", onMore)
        }
        if (landscape) Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(Modifier.weight(0.85f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                thumbnailContent(Modifier.size(minOf(maxWidth, maxHeight, 450.dp))
                    .clip(RoundedCornerShape(5.dp)))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                songInfo(); progress(); transport(); actions(); failure()
            }
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight, 450.dp)
                if (side > 0.dp) thumbnailContent(Modifier.size(side).clip(RoundedCornerShape(5.dp)))
            }
            // Normally fits without scrolling; retains every action with large accessibility fonts.
            Column(Modifier.heightIn(max = maxHeight - 48.dp - topInset)
                .verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
                songInfo(); progress(); transport(); actions(); failure()
                if (!compact && artist.isNotBlank()) Row(Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp).background(RavynStyle.panel, RavynStyle.radius)
                    .then(if (onArtistClick != null) Modifier.clickable(role = Role.Button, onClick = onArtistClick) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PlayerText("ARTIST", 10, RavynStyle.secondary)
                        PlayerText(artist, 13)
                        if (!albumTitle.isNullOrBlank()) PlayerText(albumTitle, 11, RavynStyle.secondary)
                    }
                    if (onArtistClick != null) Image(painterResource(R.drawable.chevron_forward), null,
                        colorFilter = ColorFilter.tint(RavynStyle.secondary), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerText(text: String, size: Int, color: Color = RavynStyle.text,
    bold: Boolean = false, lines: Int = 1) = BasicText(text, maxLines = lines,
    overflow = TextOverflow.Ellipsis, style = TextStyle(color = color, fontSize = size.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal))

@Composable
private fun RavynPlayerIcon(@DrawableRes icon: Int, description: String, onClick: () -> Unit,
    tint: Color = RavynStyle.text, modifier: Modifier = Modifier, state: String? = null) {
    Box(modifier.size(48.dp).clip(CircleShape).semantics {
        contentDescription = description
        state?.let { stateDescription = it }
    }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun RavynPlayerCard(label: String, @DrawableRes icon: Int, accent: Color,
    highlighted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RavynStyle.radius)
        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RavynStyle.radius)
        .background(RavynStyle.panel.copy(alpha = 0.7f))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(icon), null, colorFilter = ColorFilter.tint(if (highlighted) accent else RavynStyle.text),
            modifier = Modifier.size(18.dp))
        BasicText(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = RavynStyle.text, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun RavynTimestamp(value: Long) {
    val seconds = value.coerceAtLeast(0L) / 1000L
    val formatted = if (seconds >= 3600L) "%d:%02d:%02d".format(seconds / 3600L, seconds / 60L % 60L, seconds % 60L)
        else "%d:%02d".format(seconds / 60L, seconds % 60L)
    PlayerText(formatted, 12, RavynStyle.secondary)
}
