package app.vitune.android.ui.ravyn

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
 * Ravyn 2 design primitives. No themed ViTune item, header or navigation composables
 * are involved. Neutral charcoal surfaces allow the current track artwork to
 * own the accent without turning the whole app into a saturated backdrop.
 */
internal object RavynStyle {
    val background = Color(0xFF0C0F11)
    val panel = Color(0xFF171B1E)
    val raised = Color(0xFF22272B)
    val text = Color(0xFFF7F8FA)
    val secondary = Color(0xFFB0B8BC)
    val muted = Color(0xFF81898E)
    val radius = RoundedCornerShape(10.dp)
}

@Composable
internal fun ravynAccent(): Color = LocalAppearance.current.colorPalette.accent

@Composable
internal fun RavynLabel(
    value: String,
    size: Int = 14,
    color: Color = RavynStyle.text,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    lines: Int = 1
) {
    BasicText(value, maxLines = lines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = if (size >= 24) (-0.5).sp else 0.sp
        ), modifier = modifier)
}

@Composable
internal fun RavynIcon(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = RavynStyle.text
) {
    Box(modifier = modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Image(painterResource(icon), contentDescription = description,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun RavynArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    round: Boolean = false
) {
    val shape = if (round) CircleShape else RoundedCornerShape(9.dp)
    Box(modifier.clip(shape).background(RavynStyle.raised),
        contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.musical_notes), contentDescription = null,
            colorFilter = ColorFilter.tint(RavynStyle.muted),
            modifier = Modifier.size(25.dp))
        AsyncImage(model = url?.thumbnail(600), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().clip(shape))
    }
}

@Composable
internal fun RavynSection(
    heading: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()) {
        RavynLabel(heading, size = 16, bold = true, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Box(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction)
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            RavynLabel(action, size = 12, color = ravynAccent())
        }
    }
}

@Composable
internal fun RavynPill(value: String, selected: Boolean, onClick: () -> Unit) {
    val accent = ravynAccent()
    val fill by animateColorAsState(
        if (selected) accent.copy(alpha = 0.20f) else RavynStyle.panel,
        animationSpec = tween(220, easing = FastOutSlowInEasing), label = "Pill")
    val stroke by animateColorAsState(
        if (selected) accent else Color.White.copy(alpha = 0.18f),
        animationSpec = tween(220), label = "Pill border")
    Box(
        Modifier.border(1.dp, stroke, CircleShape)
            .background(fill, CircleShape).clip(CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        RavynLabel(value, 12, if (selected) accent else RavynStyle.secondary, selected)
    }
}

@Composable
internal fun RavynSquareTile(
    image: String?,
    name: String,
    subtitle: String? = null,
    width: Dp = 136.dp,
    onClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(width).clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick)
    ) {
        RavynArtwork(image, Modifier.size(width))
        RavynLabel(name, size = 12, bold = true, modifier = Modifier.padding(horizontal = 2.dp))
        if (subtitle != null) RavynLabel(subtitle, 10, RavynStyle.secondary,
            modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
internal fun RavynArtistTile(image: String?, name: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)) {
        RavynArtwork(image, Modifier.size(61.dp), round = true)
        RavynLabel(name, 11, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun RavynTrackRow(
    song: Song,
    onClick: () -> Unit,
    onLike: (() -> Unit)? = null,
    showHeart: Boolean = true
) {
    val accent = ravynAccent()
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().height(70.dp).clickable(onClick = onClick)
            .padding(horizontal = 2.dp)) {
        RavynArtwork(song.thumbnailUrl, Modifier.size(54.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)) {
            RavynLabel(song.title, 14, bold = true)
            RavynLabel(song.artistsText.orEmpty(), 12, RavynStyle.secondary)
        }
        if (showHeart && onLike != null) {
            RavynIcon(
                if (song.likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                if (song.likedAt == null) "Like" else "Unlike",
                onClick = onLike,
                tint = if (song.likedAt == null) RavynStyle.secondary else accent
            )
        }
    }
}

@Composable
internal fun RavynHero(title: String, subtitle: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    val accent = ravynAccent()
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(RavynStyle.panel, RavynStyle.raised)),
                RavynStyle.radius
            ).padding(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
            RavynLabel(title, size = 23, bold = true)
            RavynLabel(subtitle, size = 12, color = RavynStyle.secondary)
        }
        Box(Modifier.size(49.dp).clip(CircleShape).background(accent)
            .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = "Play",
                colorFilter = ColorFilter.tint(RavynStyle.background),
                modifier = Modifier.size(24.dp))
        }
    }
}

/** A low-contrast filter chip, matching the classic reference's little 'Music' pill. */
@Composable
internal fun RavynHomeCategoryChip() {
    Box(
        modifier = Modifier
            .background(RavynStyle.raised, RoundedCornerShape(7.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        RavynLabel("Music", 10, RavynStyle.secondary)
    }
}

/** Compact history card with artwork beside the text, not another tall album tile. */
@Composable
internal fun RavynRecentTile(
    artwork: String?, title: String, artist: String?, onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .width(165.dp)
            .height(57.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RavynStyle.panel)
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        RavynArtwork(artwork, Modifier.size(47.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            RavynLabel(title, 11, RavynStyle.text, bold = true)
            RavynLabel(artist.orEmpty(), 10, RavynStyle.secondary)
        }
    }
}
