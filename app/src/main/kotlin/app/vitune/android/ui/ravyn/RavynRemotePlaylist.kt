package app.vitune.android.ui.ravyn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.providers.innertube.Innertube

/** Fully custom remote playlist display, keeping existing playlist retrieval and actions. */
@Composable
fun RavynRemotePlaylist(
    title: String, description: String?, artwork: String?,
    songs: List<Innertube.SongItem>,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onImport: () -> Unit,
    onEnqueue: () -> Unit
) {
    val accent = ravynAccent()
    val inset = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = inset.calculateTopPadding(), bottom = inset.calculateBottomPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(60.dp)
                .padding(start = 8.dp, end = 13.dp)) {
            RavynIcon(R.drawable.chevron_back, "Back", onBack)
            RavynLabel("PLAYLIST", 12, RavynStyle.secondary, bold = true,
                modifier = Modifier.weight(1f))
            RavynIcon(R.drawable.enqueue, "Add to queue", onEnqueue,
                tint = RavynStyle.secondary)
            RavynIcon(R.drawable.add, "Save playlist", onImport, tint = accent)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.18f), RavynStyle.background)
                        )
                    ).padding(horizontal = 18.dp, vertical = 15.dp)) {
                    RavynArtwork(artwork, Modifier.size(218.dp))
                    Spacer(Modifier.height(18.dp))
                    RavynLabel(title, 25, bold = true, lines = 2)
                    if (!description.isNullOrBlank()) {
                        Spacer(Modifier.height(7.dp))
                        RavynLabel(description, 13, RavynStyle.secondary, lines = 2)
                    }
                    Spacer(Modifier.height(7.dp))
                    RavynLabel(songs.size.toString() + " songs", 12, RavynStyle.muted)
                    Spacer(Modifier.height(19.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RavynPill("Shuffle", false, onShuffle)
                        RavynPill("Play all", true) { if (songs.isNotEmpty()) onPlay(0) }
                    }
                }
                Spacer(Modifier.height(15.dp))
                RavynSection("Songs", Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(9.dp))
            }
            itemsIndexed(songs, key = { _, song -> song.key }) { i, song ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(70.dp)
                        .clickable { onPlay(i) }.padding(horizontal = 18.dp)
                ) {
                    RavynArtwork(song.thumbnail?.url, Modifier.size(54.dp))
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        RavynLabel(song.info?.name.orEmpty(), 14, bold = true)
                        RavynLabel(song.authors?.joinToString { it.name.orEmpty() }
                            .orEmpty(), 12, RavynStyle.secondary)
                    }
                    Image(painterResource(R.drawable.play), "Play",
                        colorFilter = ColorFilter.tint(RavynStyle.secondary),
                        modifier = Modifier.size(19.dp))
                }
            }
            if (songs.isEmpty()) item {
                RavynLabel("Loading playlist...", 13, RavynStyle.secondary,
                    modifier = Modifier.padding(24.dp))
            }
        }
    }
}


/** Piped collections display Media3 items, not Innertube song items. */
@Composable
fun RavynMediaPlaylist(
    title: String, artwork: String?, media: List<androidx.media3.common.MediaItem>,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit
) {
    val insets = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = insets.calculateTopPadding(),
            bottom = insets.calculateBottomPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            RavynIcon(R.drawable.chevron_back, "Back", onBack)
            RavynLabel("PLAYLIST", 12, RavynStyle.secondary, bold = true,
                modifier = Modifier.weight(1f))
            RavynIcon(R.drawable.enqueue, "Add to queue", onQueue)
        }
        LazyColumn {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    RavynArtwork(artwork, Modifier.size(207.dp))
                    Spacer(Modifier.height(18.dp))
                    RavynLabel(title, 24, bold = true, lines = 2)
                    Spacer(Modifier.height(9.dp))
                    RavynLabel(media.size.toString() + " songs",
                        12, RavynStyle.secondary)
                    Spacer(Modifier.height(17.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        RavynPill("Shuffle", false, onShuffle)
                        RavynPill("Play all", true) {
                            if (media.isNotEmpty()) onPlay(0)
                        }
                    }
                }
                RavynSection("Songs", Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(10.dp))
            }
            itemsIndexed(media, key = { _, item -> item.mediaId }) { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(71.dp)
                        .clickable { onPlay(index) }.padding(horizontal = 18.dp)) {
                    RavynArtwork(item.mediaMetadata.artworkUri?.toString(),
                        Modifier.size(53.dp))
                    Column(Modifier.weight(1f)) {
                        RavynLabel(item.mediaMetadata.title?.toString().orEmpty(),
                            14, bold = true)
                        RavynLabel(item.mediaMetadata.artist?.toString().orEmpty(),
                            12, RavynStyle.secondary)
                    }
                    Image(painterResource(R.drawable.play), "Play",
                        colorFilter = ColorFilter.tint(RavynStyle.secondary),
                        modifier = Modifier.size(19.dp))
                }
            }
            if (media.isEmpty()) item {
                RavynLabel("Loading playlist...", 13, RavynStyle.secondary,
                    modifier = Modifier.padding(20.dp))
            }
        }
    }
}
