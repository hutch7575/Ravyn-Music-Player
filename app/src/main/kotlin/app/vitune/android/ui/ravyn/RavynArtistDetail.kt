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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.Artist
import app.vitune.providers.innertube.Innertube

/** Fresh artist overview, retaining the existing artist-page provider and bookmarks. */
@Composable
fun RavynArtistDetail(
    artist: Artist?,
    artistPage: Innertube.ArtistPage?,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onSong: (Innertube.SongItem) -> Unit,
    onAlbum: (String) -> Unit
) {
    val accent = ravynAccent()
    val inset = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = inset.calculateTopPadding(),
            bottom = inset.calculateBottomPadding())) {
        Row(
            modifier = Modifier.fillMaxWidth().height(57.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RavynIcon(R.drawable.chevron_back, "Back", onBack)
            RavynLabel("ARTIST", 12, RavynStyle.secondary, bold = true,
                modifier = Modifier.weight(1f))
            RavynIcon(
                if (artist?.bookmarkedAt == null) R.drawable.bookmark_outline
                else R.drawable.bookmark, "Save artist", onBookmark,
                tint = if (artist?.bookmarkedAt != null) accent else RavynStyle.text
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(
                            accent.copy(alpha = 0.25f), RavynStyle.background)))
                        .padding(top = 20.dp, bottom = 10.dp)) {
                    RavynArtwork(artist?.thumbnailUrl ?: artistPage?.thumbnail?.url,
                        Modifier.size(197.dp), round = true)
                    Spacer(Modifier.height(20.dp))
                    RavynLabel(artist?.name ?: artistPage?.name ?: "Artist", 26, bold = true)
                    Spacer(Modifier.height(8.dp))
                    RavynLabel("ARTIST", 11, RavynStyle.secondary, bold = true)
                }
            }
            if (!artistPage?.songs.isNullOrEmpty()) item {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    RavynSection("Popular songs")
                    Spacer(Modifier.height(10.dp))
                    artistPage?.songs.orEmpty().take(12).forEach { song ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(67.dp)
                                .clickable { onSong(song) }) {
                            RavynArtwork(song.thumbnail?.url, Modifier.size(53.dp))
                            Column(Modifier.weight(1f)) {
                                RavynLabel(song.info?.name.orEmpty(), 14, bold = true)
                                RavynLabel(song.authors?.joinToString { it.name.orEmpty() }
                                    .orEmpty(), 12, RavynStyle.secondary)
                            }
                            Image(painterResource(R.drawable.play), "Play",
                                colorFilter = ColorFilter.tint(RavynStyle.secondary),
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (!artistPage?.albums.isNullOrEmpty()) item {
                Column {
                    RavynSection("Albums", Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.height(11.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(artistPage?.albums.orEmpty().take(16), key = { it.key }) { album ->
                            RavynSquareTile(album.thumbnail?.url,
                                album.info?.name.orEmpty(), album.year) {
                                onAlbum(album.key)
                            }
                        }
                    }
                }
            }
            if (!artistPage?.singles.isNullOrEmpty()) item {
                Column {
                    RavynSection("Singles & EPs", Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.height(11.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)) {
                        items(artistPage?.singles.orEmpty().take(16), key = { it.key }) { album ->
                            RavynSquareTile(album.thumbnail?.url,
                                album.info?.name.orEmpty(), album.year) {
                                onAlbum(album.key)
                            }
                        }
                    }
                }
            }
        }
    }
}
