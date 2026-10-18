package app.vitune.android.ui.ravyn

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.LocalAppearance
import coil3.compose.AsyncImage

/**
 * RAVYN / CLASSIC
 * A fresh 2016-inspired Material music-player visual system. No inherited
 * ViTune UI components. Dark surfaces are neutral and the accent belongs to
 * the current album art; it is never hard-coded to a single green or purple.
 */
internal object RavynStyle {
    val background = Color(0xFF101113)
    val panel = Color(0xFF1B1D1E)
    val raised = Color(0xFF282B2D)
    val text = Color(0xFFF7F7F7)
    val secondary = Color(0xFFB8BCBD)
    val muted = Color(0xFF8E9294)
    val radius = RoundedCornerShape(10.dp)
}
@Composable internal fun ravynAccent(): Color =
    LocalAppearance.current.colorPalette.accent

@Composable
internal fun RavynLabel(
    value: String,
    size: Int = 14,
    color: Color = RavynStyle.text,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    lines: Int = 1
) {
    BasicText(
        text = value, modifier = modifier, maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = if (size >= 22) (-0.4).sp else 0.sp,
            color = color
        )
    )
}

@Composable
internal fun RavynIcon(
    @DrawableRes icon: Int, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, tint: Color = RavynStyle.text
) {
    Box(
        modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(icon), description,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun RavynArtwork(
    url: String?, modifier: Modifier = Modifier, round: Boolean = false
) {
    val shape = if (round) CircleShape else RoundedCornerShape(5.dp)
    Box(modifier.clip(shape).background(RavynStyle.raised),
        contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.musical_notes), null,
            colorFilter = ColorFilter.tint(RavynStyle.muted),
            modifier = Modifier.size(24.dp))
        AsyncImage(
            model = url?.thumbnail(512),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.matchParentSize().clip(shape)
        )
    }
}

@Composable
internal fun RavynSection(
    heading: String, modifier: Modifier = Modifier,
    action: String? = null, onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RavynLabel(heading, 16, bold = true, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Box(Modifier.clickable(onClick = onAction)
                .padding(horizontal = 7.dp, vertical = 5.dp)) {
                RavynLabel(action, 12, ravynAccent(), bold = true)
            }
        }
    }
}

@Composable
internal fun RavynPill(value: String, selected: Boolean, onClick: () -> Unit) {
    val accent = ravynAccent()
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.22f) else RavynStyle.panel,
        tween(210, easing = FastOutSlowInEasing), label = "Classic filter"
    )
    val fg by animateColorAsState(
        if (selected) accent else RavynStyle.secondary,
        tween(210), label = "Filter text"
    )
    Box(
        Modifier
            .border(0.7.dp, if (selected) accent.copy(alpha = .75f)
                else Color.White.copy(alpha = .16f), CircleShape)
            .clip(CircleShape).background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) { RavynLabel(value, 11, fg, selected) }
}

@Composable
internal fun RavynSquareTile(
    image: String?, name: String, subtitle: String? = null,
    width: Dp = 130.dp, onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.width(width).combinedClickable(
            onClick = onClick, onLongClick = onLongClick
        )
    ) {
        RavynArtwork(image, Modifier.size(width))
        RavynLabel(name, 12, bold = true, lines = 1)
        if (!subtitle.isNullOrBlank())
            RavynLabel(subtitle, 10, RavynStyle.secondary)
    }
}

@Composable
internal fun RavynArtistTile(
    image: String?, name: String, onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.width(72.dp).combinedClickable(
            onClick = onClick, onLongClick = onLongClick
        )
    ) {
        RavynArtwork(image, Modifier.size(62.dp), round = true)
        RavynLabel(name, 10, lines = 1)
    }
}

@Composable
internal fun RavynTrackRow(
    song: Song, onClick: () -> Unit,
    onLike: (() -> Unit)? = null, showHeart: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val accent = ravynAccent()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(61.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        RavynArtwork(song.thumbnailUrl, Modifier.size(45.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RavynLabel(song.title, 12, bold = true)
            RavynLabel(song.artistsText.orEmpty(), 10, RavynStyle.secondary)
        }
        if (showHeart && onLike != null) {
            RavynIcon(
                if (song.likedAt != null) R.drawable.heart else R.drawable.heart_outline,
                if (song.likedAt != null) "Unlike" else "Like",
                onLike, tint = if (song.likedAt != null) accent else RavynStyle.muted,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
internal fun RavynHero(
    title: String, subtitle: String, @DrawableRes icon: Int, onClick: () -> Unit
) {
    val accent = ravynAccent()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(
                androidx.compose.ui.graphics.lerp(RavynStyle.panel, accent, .22f),
                RavynStyle.panel
            )), RoundedCornerShape(7.dp))
            .padding(horizontal = 15.dp, vertical = 16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            RavynLabel(title, 22, bold = true)
            RavynLabel(subtitle, 11, RavynStyle.secondary)
        }
        Box(Modifier.size(44.dp).clip(CircleShape)
            .background(accent).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) {
            Image(painterResource(icon), "Play",
                colorFilter = ColorFilter.tint(Color(0xFF111315)),
                modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun RavynHomeCategoryChip() {
    Box(
        Modifier.background(RavynStyle.raised, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) { RavynLabel("Music", 10, RavynStyle.secondary) }
}

@Composable
internal fun RavynRecentTile(
    artwork: String?, title: String, artist: String?, onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(160.dp).height(58.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(RavynStyle.panel)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(5.dp)
    ) {
        RavynArtwork(artwork, Modifier.size(48.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RavynLabel(title, 11, bold = true)
            RavynLabel(artist.orEmpty(), 9, RavynStyle.secondary)
        }
    }
}

/** One-source-of-truth artwork-adaptive header used throughout the app. */
@Composable
internal fun RavynArtTintHeader(
    title: String, subtitle: String, modifier: Modifier = Modifier
) {
    val accent = ravynAccent()
    val wash by animateColorAsState(
        lerp(RavynStyle.panel, accent, .18f),
        tween(540), label = "Album colour wash"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(wash, RavynStyle.background)))
            .padding(horizontal = 17.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RavynLabel(title, 20, bold = true)
            RavynLabel(subtitle, 11, RavynStyle.secondary)
        }
    }
}
