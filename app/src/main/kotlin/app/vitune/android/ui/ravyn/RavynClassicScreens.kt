package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.models.Artist
import app.vitune.android.models.PlaylistPreview
import app.vitune.android.models.Song
import app.vitune.providers.innertube.Innertube

/**
 * Ravyn Classic (Android 2016).
 * Compact rows, restrained horizontal collections, neutral dark backgrounds,
 * classic album-art-first composition, no modern giant card dashboard.
 */
@Composable
internal fun RavynClassicHome(
    recent: List<Song>, artists: List<Artist>, albums: List<Album>,
    playlists: List<PlaylistPreview>, related: Innertube.RelatedPage?,
    historyLoaded: Boolean, artistsLoaded: Boolean,
    playlistsLoaded: Boolean, albumsLoaded: Boolean, relatedLoaded: Boolean,
    relatedError: Boolean, onRetry: () -> Unit,
    onArtist: (String) -> Unit, onAlbum: (String) -> Unit,
    onOnlinePlaylist: (Innertube.PlaylistItem) -> Unit,
    onLocalPlaylist: (Long) -> Unit,
    onDiscover: () -> Unit, onPlay: (Song) -> Unit,
    onPlayOnline: (Innertube.SongItem) -> Unit
) {
    val recommendArtists = related?.artists.orEmpty().take(14)
    val recommendPlaylists = related?.playlists.orEmpty().take(14)
    val recommendAlbums = related?.albums.orEmpty().take(14)
    val recommendSongs = related?.songs.orEmpty().take(10)
    LazyColumn(
        contentPadding = PaddingValues(top = 3.dp, bottom = 106.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "filter") {
            Row(Modifier.padding(horizontal = 17.dp)) { RavynHomeCategoryChip() }
        }
        item(key = "artists") {
            RavynReveal(
                artists.isNotEmpty() || recommendArtists.isNotEmpty() ||
                    (artistsLoaded && relatedLoaded),
                "Your favorite artists", RavynPlaceholderStyle.Artists
            ) {
                if (recommendArtists.isNotEmpty() || artists.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RavynSection("Your favorite artists",
                            Modifier.padding(horizontal = 17.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 17.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (recommendArtists.isNotEmpty()) {
                                items(recommendArtists, key = { it.key }) { artist ->
                                    RavynArtistTile(
                                        artist.thumbnail?.url, artist.info?.name.orEmpty()
                                    ) { onArtist(artist.key) }
                                }
                            } else {
                                items(artists.take(14), key = { it.id }) { artist ->
                                    RavynArtistTile(
                                        artist.thumbnailUrl, artist.name.orEmpty()
                                    ) { onArtist(artist.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "history") {
            RavynReveal(historyLoaded, "Recently played", RavynPlaceholderStyle.Recent) {
                if (recent.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RavynSection("Recently played", Modifier.padding(horizontal = 17.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 17.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recent.take(18), key = { it.id }) { song ->
                                RavynRecentTile(
                                    song.thumbnailUrl, song.title, song.artistsText
                                ) { onPlay(song) }
                            }
                        }
                    }
                }
            }
        }
        item(key = "mixes") {
            RavynReveal(
                recommendPlaylists.isNotEmpty() || playlists.isNotEmpty() ||
                    (playlistsLoaded && relatedLoaded),
                "Made for you", RavynPlaceholderStyle.Albums
            ) {
                if (recommendPlaylists.isNotEmpty() || playlists.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RavynSection("Made for you", Modifier.padding(horizontal = 17.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 17.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (recommendPlaylists.isNotEmpty()) {
                                items(recommendPlaylists, key = { it.key }) { mix ->
                                    RavynSquareTile(
                                        mix.thumbnail?.url, mix.info?.name.orEmpty(),
                                        mix.channel?.name, 124.dp
                                    ) { onOnlinePlaylist(mix) }
                                }
                            } else {
                                items(playlists.take(14), key = { it.id }) { playlist ->
                                    RavynSquareTile(
                                        playlist.thumbnail, playlist.name,
                                        playlist.songCount.toString() + " songs", 124.dp
                                    ) { onLocalPlaylist(playlist.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "albums") {
            RavynReveal(
                recommendAlbums.isNotEmpty() || albums.isNotEmpty() ||
                    (albumsLoaded && relatedLoaded),
                "Albums for you", RavynPlaceholderStyle.Albums
            ) {
                if (recommendAlbums.isNotEmpty() || albums.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RavynSection("Albums for you", Modifier.padding(horizontal = 17.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 17.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (recommendAlbums.isNotEmpty()) {
                                items(recommendAlbums, key = { it.key }) { album ->
                                    RavynSquareTile(
                                        album.thumbnail?.url, album.info?.name.orEmpty(),
                                        album.authors?.joinToString { it.name.orEmpty() }, 124.dp
                                    ) { onAlbum(album.key) }
                                }
                            } else {
                                items(albums.take(14), key = { it.id }) { album ->
                                    RavynSquareTile(
                                        album.thumbnailUrl, album.title.orEmpty(),
                                        album.authorsText, 124.dp
                                    ) { onAlbum(album.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "tracks") {
            RavynReveal(
                recommendSongs.isNotEmpty() || relatedLoaded,
                "Tracks for you", RavynPlaceholderStyle.Tracks
            ) {
                if (recommendSongs.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 17.dp)) {
                        RavynSection("Tracks for you")
                        recommendSongs.forEach { song ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().height(61.dp)
                                    .clickable { onPlayOnline(song) }
                            ) {
                                RavynArtwork(song.thumbnail?.url, Modifier.size(46.dp))
                                Column(Modifier.weight(1f)) {
                                    RavynLabel(song.info?.name.orEmpty(), 12, bold = true)
                                    Spacer(Modifier.height(4.dp))
                                    RavynLabel(
                                        song.authors?.joinToString { it.name.orEmpty() }
                                            .orEmpty(), 10, RavynStyle.secondary
                                    )
                                }
                                Image(painterResource(R.drawable.play), "Play",
                                    colorFilter = ColorFilter.tint(ravynAccent()),
                                    modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
        if (relatedLoaded && recent.isEmpty() && artists.isEmpty() &&
            albums.isEmpty() && playlists.isEmpty() &&
            recommendArtists.isEmpty() && recommendSongs.isEmpty() &&
            recommendPlaylists.isEmpty()) {
            item(key = "retry") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)
                ) {
                    RavynLabel(if (relatedError) "Couldn't load music"
                        else "Discover your next favorite", 18, bold = true)
                    RavynLabel("Find something new, or try reloading recommendations.",
                        12, RavynStyle.secondary, lines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RavynPill("Discover", true, onDiscover)
                        RavynPill("Retry", false, onRetry)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RavynClassicLibrary(
    category: Int, setCategory: (Int) -> Unit,
    liked: List<Song>, songs: List<Song>, local: List<Song>,
    albums: List<Album>, artists: List<Artist>, playlists: List<PlaylistPreview>,
    onAlbum: (String) -> Unit, onArtist: (String) -> Unit,
    onLocalPlaylist: (Long) -> Unit,
    onCreate: () -> Unit, onAddSongs: () -> Unit,
    onPlayAll: () -> Unit, onPlay: (List<Song>, Int) -> Unit,
    onLike: (Song) -> Unit
) {
    val filters = listOf("Liked", "Playlists", "Artists", "Albums", "Songs", "Local")
    var ascending by rememberSaveable { mutableStateOf(false) }
    val rememberTabs = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            filters.forEachIndexed { i, filter ->
                RavynPill(filter, i == category) { setCategory(i) }
            }
        }
        AnimatedContent(
            targetState = category,
            transitionSpec = {
                ContentTransform(
                    fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(240)) { it / 12 },
                    fadeOut(tween(120)), sizeTransform = null
                )
            }, label = "Classic library pages", modifier = Modifier.fillMaxSize()
        ) { selected ->
            rememberTabs.SaveableStateProvider(selected) {
                when (selected) {
                    0, 4, 5 -> {
                        val collection = when (selected) {
                            0 -> liked
                            5 -> local
                            else -> songs
                        }
                        val visible = if (ascending) collection.reversed() else collection
                        LazyColumn(contentPadding = PaddingValues(
                            start = 17.dp, end = 17.dp, bottom = 105.dp)) {
                            item {
                                if (selected == 0) {
                                    RavynHero("Liked Songs", collection.size.toString() + " Songs",
                                        R.drawable.play, onPlayAll)
                                } else {
                                    RavynLabel(if (selected == 4) "Your Songs"
                                        else "Local Music", 22, bold = true)
                                    Spacer(Modifier.height(5.dp))
                                    RavynLabel(collection.size.toString() + " Songs",
                                        11, RavynStyle.secondary)
                                }
                                Spacer(Modifier.height(13.dp))
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    RavynPill(if (ascending) "Oldest" else "Recent", true) {
                                        ascending = !ascending
                                    }
                                    if (selected == 0) RavynPill(
                                        "Add Songs", false, onAddSongs
                                    )
                                    Spacer(Modifier.weight(1f))
                                    RavynIcon(R.drawable.shuffle, "Shuffle", {
                                        val shuffled = collection.shuffled()
                                        if (shuffled.isNotEmpty()) onPlay(shuffled, 0)
                                    }, tint = ravynAccent())
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            items(visible, key = { it.id }) { song ->
                                RavynTrackRow(song,
                                    onClick = { onPlay(visible, visible.indexOf(song)) },
                                    onLike = { onLike(song) })
                            }
                            if (collection.isEmpty()) item {
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp),
                                    modifier = Modifier.padding(vertical = 25.dp)) {
                                    RavynLabel("Nothing here yet", 15, bold = true)
                                    RavynLabel("Search for music and save your favorites.",
                                        12, RavynStyle.secondary)
                                    RavynPill("Find songs", true, onAddSongs)
                                }
                            }
                        }
                    }
                    1 -> LazyColumn(contentPadding = PaddingValues(
                        start = 17.dp, end = 17.dp, bottom = 105.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RavynLabel("Your Playlists", 22, bold = true,
                                    modifier = Modifier.weight(1f))
                                RavynIcon(R.drawable.add, "New playlist", onCreate,
                                    tint = ravynAccent())
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().height(66.dp)
                                    .clickable { onLocalPlaylist(playlist.id) }
                            ) {
                                RavynArtwork(playlist.thumbnail, Modifier.size(50.dp))
                                Column(Modifier.weight(1f)) {
                                    RavynLabel(playlist.name, 13, bold = true)
                                    Spacer(Modifier.height(4.dp))
                                    RavynLabel(playlist.songCount.toString() + " songs",
                                        10, RavynStyle.secondary)
                                }
                                Image(painterResource(R.drawable.chevron_forward), null,
                                    colorFilter = ColorFilter.tint(RavynStyle.muted),
                                    modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    2 -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(
                            start = 17.dp, end = 17.dp, bottom = 105.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RavynLabel("Your Artists", 22, bold = true)
                        }
                        gridItems(artists, key = { it.id }) { artist ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onArtist(artist.id) }) {
                                RavynArtwork(artist.thumbnailUrl,
                                    Modifier.size(94.dp), round = true)
                                RavynLabel(artist.name.orEmpty(), 12, lines = 2)
                            }
                        }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(125.dp),
                        contentPadding = PaddingValues(
                            start = 17.dp, end = 17.dp, bottom = 105.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RavynLabel("Your Albums", 22, bold = true)
                        }
                        gridItems(albums, key = { it.id }) { album ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onAlbum(album.id) }) {
                                RavynArtwork(album.thumbnailUrl,
                                    Modifier.fillMaxWidth().aspectRatio(1f))
                                RavynLabel(album.title.orEmpty(), 12, bold = true, lines = 2)
                                RavynLabel(album.authorsText.orEmpty(), 10, RavynStyle.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
