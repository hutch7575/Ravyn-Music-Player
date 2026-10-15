package app.vitune.android.ui.screens.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import app.vitune.android.R
import app.vitune.android.models.ui.UiMedia
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.service.PlayerService
import app.vitune.android.ui.components.SeekBar
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious

/**
 * Ravyn's classic Material-inspired now-playing view.
 * Only the portrait presentation is replaced here. The existing player sheet,
 * queue, thumbnail gestures, lyrics, PiP and playback service remain in use.
 */
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
    position: Long,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    thumbnailContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) = BoxWithConstraints(modifier.fillMaxSize()) {
    val compact = maxHeight < 590.dp
    val narrow = maxWidth < 355.dp
    val horizontalPadding = if (narrow) 20.dp else 28.dp
    val animatedArtworkColor by animateColorAsState(
        artworkAccent,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "Artwork theme"
    )
    val accent = lerp(animatedArtworkColor, Color.White, 0.12f)
    val primaryText = Color.White
    val secondaryText = Color(0xFFBFC4C8)

    // Reuse the loaded album cover's colour palette. No extra artwork image
    // and no costly real-time blur; only a few smoothly animated colours.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lerp(Color(0xFF141719), animatedArtworkColor, 0.38f),
                        lerp(Color(0xFF111315), animatedArtworkColor, 0.13f),
                        Color(0xFF0D0F11)
                    )
                )
            )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset, start = horizontalPadding, end = horizontalPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 46.dp else 58.dp)
        ) {
            RavynIcon(
                icon = R.drawable.chevron_down,
                description = "Close player",
                onClick = onCollapse,
                color = primaryText,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.weight(1f))
            BasicText("NOW PLAYING", style = TextStyle(
                color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.0.sp
            ))
            Spacer(Modifier.weight(1f))
            RavynIcon(
                icon = R.drawable.ellipsis_horizontal,
                description = "More options",
                onClick = onMore,
                color = primaryText,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))

        // This is the original interactive thumbnail, including swipe/lyrics/PiP.
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val side = minOf(maxWidth, maxHeight - 8.dp, 440.dp)
            if (side > 0.dp) {
                thumbnailContent(
                    Modifier
                        .size(side)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }

        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = primaryText,
                        fontSize = if (narrow) 22.sp else 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.45).sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = secondaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
            RavynIcon(
                icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                description = if (likedAt == null) "Like song" else "Unlike song",
                onClick = { setLikedAt(if (likedAt == null) System.currentTimeMillis() else null) },
                color = if (likedAt == null) Color.White else accent,
                iconSize = 25.dp
            )
        }

        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))

        // Never let the seekbar pop in and shift every control after buffering.
        // The resting state has the same footprint as the interactive slider.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(15.dp)
        ) {
            if (binder != null && media != null && media.duration > 0L &&
                media.duration != androidx.media3.common.C.TIME_UNSET) {
                SeekBar(
                    binder = binder,
                    position = position,
                    media = media,
                    color = accent,
                    backgroundColor = Color.White.copy(alpha = 0.36f),
                    style = PlayerPreferences.SeekBarStyle.Static,
                    scrubberRadius = 6.dp
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.36f))
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            RavynTimeLabel(position)
            if (media != null && media.duration > 0L &&
                media.duration != androidx.media3.common.C.TIME_UNSET) {
                RavynTimeLabel((media.duration - position).coerceAtLeast(0L), remaining = true)
            } else {
                BasicText("--:--", style = TextStyle(color = secondaryText, fontSize = 12.sp))
            }
        }

        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val player = binder?.player
            RavynIcon(
                icon = R.drawable.shuffle,
                description = "Toggle shuffle",
                color = if (player?.shuffleModeEnabled == true) accent else primaryText,
                onClick = { if (player != null) player.shuffleModeEnabled = !player.shuffleModeEnabled },
                modifier = Modifier.weight(1f),
                iconSize = 22.dp
            )
            RavynIcon(
                icon = R.drawable.play_skip_back,
                description = "Previous song",
                color = primaryText,
                onClick = { player?.forceSeekToPrevious() },
                modifier = Modifier.weight(1f),
                iconSize = 24.dp
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (compact) 64.dp else 72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        if (player != null) {
                            if (shouldBePlaying) player.pause()
                            else {
                                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                                player.play()
                            }
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = shouldBePlaying,
                    transitionSpec = {
                        (fadeIn(tween(170, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.88f, animationSpec = tween(170)))
                            .togetherWith(fadeOut(tween(100)))
                    },
                    label = "Play/pause"
                ) { playing ->
                    Image(
                        painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                        contentDescription = if (playing) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(Color(0xFF111315)),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            RavynIcon(
                icon = R.drawable.play_skip_forward,
                description = "Next song",
                color = primaryText,
                onClick = { player?.forceSeekToNext() },
                modifier = Modifier.weight(1f),
                iconSize = 24.dp
            )
            RavynIcon(
                icon = if (PlayerPreferences.trackLoopEnabled) R.drawable.repeat_on else R.drawable.repeat,
                description = "Toggle repeat track",
                color = if (PlayerPreferences.trackLoopEnabled) accent else primaryText,
                onClick = { PlayerPreferences.trackLoopEnabled = !PlayerPreferences.trackLoopEnabled },
                modifier = Modifier.weight(1f),
                iconSize = 23.dp
            )
        }

        Spacer(Modifier.height(if (compact) 10.dp else 19.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RavynTextAction("LYRICS", R.drawable.musical_notes, accent, true, onShowLyrics)
            RavynTextAction("UP NEXT", R.drawable.enqueue, accent, false, onShowQueue)
        }

        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
    }
}

@Composable
private fun RavynIcon(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    alignEnd: Boolean = false
) {
    Box(
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun RavynTextAction(
    text: String,
    @DrawableRes icon: Int,
    accent: Color,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        if (highlighted) accent.copy(alpha = 0.16f) else Color(0xFF1B1F22),
        animationSpec = tween(320), label = "Action card"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Image(
            painterResource(icon), contentDescription = null,
            colorFilter = ColorFilter.tint(if (highlighted) accent else Color.White),
            modifier = Modifier.size(21.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(15.dp))
        BasicText(
            text, style = TextStyle(
                color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
            ),
            modifier = Modifier.weight(1f)
        )
        Image(painterResource(R.drawable.chevron_forward), contentDescription = null,
            colorFilter = ColorFilter.tint(if (highlighted) accent else Color(0xFFABB0B4)),
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RavynTimeLabel(timeMs: Long, remaining: Boolean = false) {
    val seconds = timeMs.coerceAtLeast(0L) / 1000
    val time = if (seconds >= 3600) {
        "%d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
    } else {
        "%d:%02d".format(seconds / 60, seconds % 60)
    }
    BasicText(
        text = if (remaining) "-$time" else time,
        style = TextStyle(color = Color(0xFFD4D4E9), fontSize = 12.sp)
    )
}
