package app.vitune.android.ui.screens.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
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
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious

/** New Now Playing layout based on the approved Ravyn reference. Playback and
 * artwork/lyrics gesture logic come from the existing media engine, not ViTune UI. */
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
    albumTitle: String?,
    onArtistClick: (() -> Unit)?,
    thumbnailContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) = BoxWithConstraints(modifier.fillMaxSize()) {
    val compact = maxHeight < 615.dp
    val horizontal = if (maxWidth < 350.dp) 18.dp else 23.dp
    val animatedColor by animateColorAsState(
        targetValue = artworkAccent,
        animationSpec = tween(560, easing = FastOutSlowInEasing),
        label = "Album artwork colour"
    )
    val accent = lerp(animatedColor, Color.White, 0.08f)
    val main = Color(0xFFF8F8FA)
    val sub = Color(0xFFC1C4C7)

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            0f to lerp(Color(0xFF18191A), animatedColor, 0.27f),
            0.33f to lerp(Color(0xFF171819), animatedColor, 0.12f),
            0.78f to Color(0xFF141516),
            1f to Color(0xFF111214)
        )
    ))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
            .padding(start = horizontal, end = horizontal, top = topInset)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(57.dp)) {
            RavynPlayerIcon(R.drawable.chevron_down, "Close player", onCollapse)
            Spacer(Modifier.weight(1f))
            BasicText("NOW PLAYING", style = TextStyle(
                color = main, fontSize = 13.sp,
                letterSpacing = 0.35.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.weight(1f))
            RavynPlayerIcon(R.drawable.ellipsis_horizontal, "More options", onMore)
        }

        Spacer(Modifier.height(if (compact) 1.dp else 9.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight, 450.dp)
            if (side > 0.dp) {
                thumbnailContent(Modifier.size(side)
                    .clip(RoundedCornerShape(2.dp)))
            }
        }

        Spacer(Modifier.height(if (compact) 6.dp else 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = main, fontSize = if (compact) 19.sp else 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.6).sp
                    ))
                Spacer(Modifier.height(5.dp))
                BasicText(artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = sub, fontSize = 13.sp))
            }
            RavynPlayerIcon(
                if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                if (likedAt == null) "Like song" else "Unlike song",
                { setLikedAt(if (likedAt == null) System.currentTimeMillis() else null) },
                tint = if (likedAt == null) main else accent
            )
        }

        Spacer(Modifier.height(if (compact) 8.dp else 15.dp))
        Box(Modifier.fillMaxWidth().height(15.dp),
            contentAlignment = Alignment.Center) {
            if (binder != null && media != null && media.duration > 0L
                && media.duration != C.TIME_UNSET) {
                SeekBar(
                    binder = binder, position = position, media = media,
                    color = accent, backgroundColor = Color.White.copy(alpha = 0.27f),
                    style = PlayerPreferences.SeekBarStyle.Static,
                    scrubberRadius = 5.dp
                )
            } else {
                Box(Modifier.fillMaxWidth().height(3.dp)
                    .background(Color.White.copy(alpha = 0.24f),
                        RoundedCornerShape(3.dp)))
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            RavynTimestamp(position, sub)
            RavynTimestamp(
                if (media != null && media.duration > 0L &&
                    media.duration != C.TIME_UNSET) media.duration else 0L, sub
            )
        }

        Spacer(Modifier.height(if (compact) 7.dp else 15.dp))
        val player = binder?.player
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(66.dp)
        ) {
            RavynPlayerIcon(
                R.drawable.shuffle, "Shuffle",
                {
                    if (player != null) player.shuffleModeEnabled = !player.shuffleModeEnabled
                },
                tint = if (player?.shuffleModeEnabled == true) accent else main,
                modifier = Modifier.weight(1f)
            )
            RavynPlayerIcon(
                R.drawable.play_skip_back, "Previous",
                { player?.forceSeekToPrevious() }, main, Modifier.weight(1f)
            )
            Box(
                Modifier.size(60.dp).clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        if (player != null) {
                            if (shouldBePlaying) player.pause()
                            else {
                                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                                player.play()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = shouldBePlaying,
                    transitionSpec = {
                        ContentTransform(
                            fadeIn(tween(170)) + scaleIn(
                                initialScale = 0.82f, animationSpec = tween(170)),
                            fadeOut(tween(110)), sizeTransform = null
                        )
                    }, label = "Animated playback controls") { playing ->
                    Image(painterResource(if (playing) R.drawable.pause else R.drawable.play),
                        contentDescription = if (playing) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(Color(0xFF111315)),
                        modifier = Modifier.size(31.dp))
                }
            }
            RavynPlayerIcon(
                R.drawable.play_skip_forward, "Next", { player?.forceSeekToNext() },
                main, Modifier.weight(1f)
            )
            RavynPlayerIcon(
                if (PlayerPreferences.trackLoopEnabled) R.drawable.repeat_on
                    else R.drawable.repeat,
                "Repeat track",
                { PlayerPreferences.trackLoopEnabled = !PlayerPreferences.trackLoopEnabled },
                if (PlayerPreferences.trackLoopEnabled) accent else main,
                Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(if (compact) 7.dp else 15.dp))

        RavynPlayerCard("LYRICS", R.drawable.musical_notes,
            accent, highlighted = true, onShowLyrics)
        Spacer(Modifier.height(if (compact) 3.dp else 5.dp))
        RavynPlayerCard("UP NEXT", R.drawable.enqueue,
            accent, highlighted = false, onShowQueue)
        if (artist.isNotBlank()) {
            Spacer(Modifier.height(if (compact) 5.dp else 10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .height(if (compact) 45.dp else 52.dp)
                    .background(Color.White.copy(alpha = 0.045f))
                    .then(if (onArtistClick != null) Modifier.clickable(onClick = onArtistClick)
                          else Modifier)
                    .padding(horizontal = 14.dp)
            ) {
                Image(painterResource(R.drawable.artist), contentDescription = null,
                    colorFilter = ColorFilter.tint(accent), modifier = Modifier.size(21.dp))
                Column(Modifier.weight(1f)) {
                    BasicText("ARTIST", style = TextStyle(
                        color = sub, fontSize = 10.sp, letterSpacing = 0.5.sp))
                    BasicText(artist, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = main, fontWeight = FontWeight.Medium,
                            fontSize = 13.sp))
                    if (!compact && !albumTitle.isNullOrBlank()) {
                        BasicText(albumTitle, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(color = sub, fontSize = 10.sp))
                    }
                }
                if (onArtistClick != null) Image(
                    painterResource(R.drawable.chevron_forward),
                    contentDescription = "Open artist profile",
                    colorFilter = ColorFilter.tint(sub),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 9.dp))
    }
}

@Composable
private fun RavynPlayerIcon(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(modifier.height(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Image(painterResource(icon), contentDescription = description,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun RavynPlayerCard(
    label: String, @DrawableRes icon: Int, accent: Color,
    highlighted: Boolean, onClick: () -> Unit
) {
    val line = Color.White.copy(alpha = 0.12f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxWidth().height(47.dp)
            .border(width = 0.5.dp, color = line)
            .background(Color(0xFF191B1D).copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Image(painterResource(icon), contentDescription = null,
            colorFilter = ColorFilter.tint(if (highlighted) accent else Color.White),
            modifier = Modifier.size(18.dp))
        BasicText(label, modifier = Modifier.weight(1f),
            style = TextStyle(
                color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 0.45.sp
            ))
        Image(painterResource(R.drawable.chevron_forward), contentDescription = null,
            colorFilter = ColorFilter.tint(Color(0xFFB4B8BA)),
            modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RavynTimestamp(value: Long, color: Color) {
    val seconds = value.coerceAtLeast(0L) / 1000L
    val formatted = if (seconds >= 3600L) {
        "%d:%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
    } else "%d:%02d".format(seconds / 60L, seconds % 60L)
    BasicText(formatted,
        style = TextStyle(color = color, fontSize = 12.sp))
}
