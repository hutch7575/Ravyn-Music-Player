package app.vitune.android.ui.ravyn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.Song

/**
 * Ravyn's own artist/album/playlist detail presentation. Data loading, playback,
 * caching and bookmark changes remain supplied by the original repository.
 */
@Composable
fun RavynCollectionDetail(
    title: String,
    artist: String?,
    artwork: String?,
    tracks: List<Song>,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onLike: (Song) -> Unit,
    circleArt: Boolean = false,
    bookmarked: Boolean? = null,
    onBookmark: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    subnav: @Composable () -> Unit = {},
    emptyMessage: String = "No songs here yet"
) {
    val accent = ravynAccent()
    val insets = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 9.dp)
        ) {
            RavynIcon(R.drawable.chevron_back, "Back", onBack)
            RavynLabel("RAVYN", 12, RavynStyle.secondary,
                bold = true, modifier = Modifier.weight(1f))
            if (onBookmark != null && bookmarked != null) RavynIcon(
                if (bookmarked) R.drawable.bookmark else R.drawable.bookmark_outline,
                "Bookmark", onBookmark, tint = if (bookmarked) accent else RavynStyle.text)
            if (onMenu != null) RavynIcon(
                R.drawable.ellipsis_horizontal, "More options", onMenu)
        }
        subnav()
        LazyColumn(contentPadding = PaddingValues(bottom = 35.dp)) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.20f), RavynStyle.background)
                        )
                    ).padding(horizontal = 18.dp, vertical = 18.dp)) {
                    RavynArtwork(artwork, Modifier.size(195.dp), round = circleArt)
                    Spacer(Modifier.height(14.dp))
                    RavynLabel(title, 25, bold = true, lines = 2)
                    if (!artist.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        RavynLabel(artist, 14, RavynStyle.secondary, lines = 2)
                    }
                    Spacer(Modifier.height(7.dp))
                    RavynLabel(tracks.size.toString() + " songs", 12, RavynStyle.muted)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(RavynStyle.panel, RoundedCornerShape(4.dp))
                                .clickable(onClick = onShuffle)
                                .padding(horizontal = 18.dp, vertical = 13.dp)) {
                            Image(painterResource(R.drawable.shuffle), null,
                                colorFilter = ColorFilter.tint(accent),
                                modifier = Modifier.size(18.dp))
                            RavynLabel("Shuffle", 13)
                        }
                        Spacer(Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .background(accent, RoundedCornerShape(24.dp))
                                .clickable(enabled = tracks.isNotEmpty()) { onPlay(0) }
                                .padding(horizontal = 25.dp, vertical = 13.dp)) {
                            Image(painterResource(R.drawable.play), "Play",
                                colorFilter = ColorFilter.tint(RavynStyle.background),
                                modifier = Modifier.size(20.dp))
                            RavynLabel("Play", 14, RavynStyle.background, bold = true)
                        }
                    }
                }
                RavynSection("Songs", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
            if (tracks.isEmpty()) item {
                RavynLabel(emptyMessage, 13, RavynStyle.secondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 23.dp))
            }
            itemsIndexed(tracks, key = { _, song -> song.id }) { index, song ->
                Box(Modifier.padding(horizontal = 18.dp)) {
                    RavynTrackRow(song, onClick = { onPlay(index) },
                        onLike = { onLike(song) })
                }
            }
        }
    }
}
