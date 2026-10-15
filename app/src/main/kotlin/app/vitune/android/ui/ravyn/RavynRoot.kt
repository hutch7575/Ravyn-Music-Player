package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.models.Artist
import app.vitune.android.models.Playlist
import app.vitune.android.models.PlaylistPreview
import app.vitune.android.models.Song
import app.vitune.android.query
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.core.data.enums.AlbumSortBy
import app.vitune.core.data.enums.ArtistSortBy
import app.vitune.core.data.enums.PlaylistSortBy
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.requests.relatedPage

/** Brand-new Ravyn portrait shell, retaining only existing data and playback APIs. */
@Composable
fun RavynRoot(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onDiscover: () -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onOnlinePlaylist: (Innertube.PlaylistItem) -> Unit,
    onLocalPlaylist: (Long) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val ravynNavigation = LocalRavynNavigation.current
    ravynNavigation.search = onSearch
    var tab by ravynNavigation.destination
    var collection by rememberSaveable { mutableStateOf(0) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var liked by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var local by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recent by remember { mutableStateOf<List<Song>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    var related by remember { mutableStateOf<Innertube.RelatedPage?>(null) }

    LaunchedEffect(Unit) { Database.favorites().collect { liked = it } }
    LaunchedEffect(Unit) {
        Database.songs(SongSortBy.DateAdded, SortOrder.Descending).collect { songs = it }
    }
    LaunchedEffect(Unit) {
        Database.songs(SongSortBy.DateAdded, SortOrder.Descending, isLocal = true)
            .collect { local = it }
    }
    LaunchedEffect(Unit) {
        Database.events().collect { events ->
            recent = events.map { it.song }.distinctBy { it.id }.take(24)
        }
    }
    LaunchedEffect(Unit) {
        Database.albums(AlbumSortBy.DateAdded, SortOrder.Descending).collect { albums = it }
    }
    LaunchedEffect(Unit) {
        Database.artists(ArtistSortBy.Name, SortOrder.Ascending).collect { artists = it }
    }
    LaunchedEffect(Unit) {
        Database.playlistPreviews(PlaylistSortBy.DateAdded, SortOrder.Descending)
            .collect { playlists = it }
    }
    LaunchedEffect(recent.firstOrNull()?.id) {
        related = runCatching {
            Innertube.relatedPage(
                NextBody(videoId = recent.firstOrNull()?.id ?: "J7p4bzqLvCw")
            )?.getOrNull()
        }.getOrNull()
    }

    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("Create playlist", color = RavynStyle.text) },
        text = { TextField(newName, onValueChange = { newName = it },
            placeholder = { Text("Playlist name") }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = {
                if (newName.isNotBlank()) {
                    val name = newName.trim()
                    query { Database.insert(Playlist(name = name)) }
                    showCreate = false
                    newName = ""
                }
            }) { Text("Create", color = ravynAccent()) }
        },
        dismissButton = { TextButton(onClick = { showCreate = false }) {
            Text("Cancel", color = RavynStyle.secondary)
        } },
        containerColor = RavynStyle.panel
    )

    val insets = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = insets.calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(62.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(ravynAccent().copy(alpha = 0.10f),
                            RavynStyle.background, RavynStyle.background)
                    )
                )
                .padding(start = 17.dp, end = 8.dp)) {
            if (tab == 0) {
                val hour = java.util.Calendar.getInstance()
                    .get(java.util.Calendar.HOUR_OF_DAY)
                RavynLabel(
                    when (hour) {
                        in 5..11 -> "Good morning"
                        in 12..17 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    size = 20, bold = true, modifier = Modifier.weight(1f)
                )
            } else {
                RavynLabel("Your Library", size = 21, bold = true,
                    modifier = Modifier.weight(1f))
            }
            RavynIcon(R.drawable.history, "History", onHistory)
            RavynIcon(R.drawable.settings, "Settings", onSettings)
        }
        AnimatedContent(
            targetState = tab, modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = { ContentTransform(
                fadeIn(tween(230)), fadeOut(tween(120)), sizeTransform = null
            ) }, label = "Ravyn destinations"
        ) { active ->
            if (active == 0) RavynHome(
                recent = recent, artists = artists, albums = albums,
                playlists = playlists, related = related,
                onArtist = onArtist, onAlbum = onAlbum,
                onOnlinePlaylist = onOnlinePlaylist, onLocalPlaylist = onLocalPlaylist,
                onDiscover = onDiscover,
                onPlay = { song ->
                    binder?.stopRadio()
                    binder?.player?.forcePlay(song.asMediaItem)
                },
                onPlayOnline = { song ->
                    val media = song.asMediaItem
                    binder?.stopRadio()
                    binder?.player?.forcePlay(media)
                    binder?.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = media.mediaId))
                }
            ) else RavynLibrary(
                category = collection, setCategory = { collection = it },
                liked = liked, songs = songs, local = local, albums = albums,
                artists = artists, playlists = playlists,
                onAlbum = onAlbum, onArtist = onArtist, onLocalPlaylist = onLocalPlaylist,
                onCreate = { showCreate = true },
                onPlayAll = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(liked.map(Song::asMediaItem))
                },
                onPlay = { list, index ->
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(list.map(Song::asMediaItem), index)
                },
                onLike = { song ->
                    query { Database.like(song.id,
                        if (song.likedAt == null) System.currentTimeMillis() else null) }
                }
            )
        }
        RavynBottomNavigation()
        Spacer(Modifier.height(WindowInsets.systemBars
            .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()))
    }
}

@Composable
private fun RavynHome(
    recent: List<Song>,
    artists: List<Artist>,
    albums: List<Album>,
    playlists: List<PlaylistPreview>,
    related: Innertube.RelatedPage?,
    onArtist: (String) -> Unit,
    onAlbum: (String) -> Unit,
    onOnlinePlaylist: (Innertube.PlaylistItem) -> Unit,
    onLocalPlaylist: (Long) -> Unit,
    onDiscover: () -> Unit,
    onPlay: (Song) -> Unit,
    onPlayOnline: (Innertube.SongItem) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()
                .padding(start = 17.dp, top = 0.dp, bottom = 1.dp)) {
                RavynHomeCategoryChip()
            }
        }
        if (artists.isNotEmpty() || !related?.artists.isNullOrEmpty()) item {
            Column {
                RavynSection("Your favorite artists", Modifier.padding(horizontal = 17.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 17.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (artists.isNotEmpty()) {
                        items(artists.take(16), key = { it.id }) { artist ->
                            RavynArtistTile(artist.thumbnailUrl, artist.name.orEmpty()) {
                                onArtist(artist.id)
                            }
                        }
                    } else {
                        items(related?.artists.orEmpty().take(16), key = { it.key }) { artist ->
                            RavynArtistTile(artist.thumbnail?.url, artist.info?.name.orEmpty()) {
                                onArtist(artist.key)
                            }
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) item {
            Column {
                RavynSection("Recently played", Modifier.padding(horizontal = 17.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recent.take(16), key = { it.id }) { song ->
                        RavynRecentTile(song.thumbnailUrl, song.title, song.artistsText) {
                            onPlay(song)
                        }
                    }
                }
            }
        }
        if (!related?.playlists.isNullOrEmpty() || playlists.isNotEmpty()) item {
            Column {
                RavynSection("Made for you", Modifier.padding(horizontal = 17.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!related?.playlists.isNullOrEmpty()) {
                        items(related?.playlists.orEmpty().take(12), key = { it.key }) { mix ->
                            RavynSquareTile(mix.thumbnail?.url, mix.info?.name.orEmpty(),
                                mix.channel?.name, width = 125.dp) { onOnlinePlaylist(mix) }
                        }
                    } else {
                        items(playlists.take(12), key = { it.id }) { p ->
                            RavynSquareTile(p.thumbnail, p.name,
                                p.songCount.toString() + " songs", width = 125.dp) {
                                onLocalPlaylist(p.id)
                            }
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty() || !related?.albums.isNullOrEmpty()) item {
            Column {
                RavynSection("Albums for you", Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (albums.isNotEmpty()) {
                        items(albums.take(14), key = { it.id }) { album ->
                            RavynSquareTile(album.thumbnailUrl, album.title.orEmpty(),
                                album.authorsText) { onAlbum(album.id) }
                        }
                    } else {
                        items(related?.albums.orEmpty().take(14), key = { it.key }) { album ->
                            RavynSquareTile(album.thumbnail?.url, album.info?.name.orEmpty(),
                                album.authors?.joinToString { it.name.orEmpty() }) {
                                onAlbum(album.key)
                            }
                        }
                    }
                }
            }
        }
        if (!related?.songs.isNullOrEmpty()) item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                RavynSection("Tracks for you")
                Spacer(Modifier.height(9.dp))
                related?.songs.orEmpty().take(8).forEach { song ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(68.dp)
                            .clickable { onPlayOnline(song) }) {
                        RavynArtwork(song.thumbnail?.url, Modifier.size(54.dp))
                        Column(Modifier.weight(1f)) {
                            RavynLabel(song.info?.name.orEmpty(), 14, bold = true)
                            RavynLabel(song.authors?.joinToString { it.name.orEmpty() }.orEmpty(),
                                12, RavynStyle.secondary)
                        }
                        Image(painterResource(R.drawable.play), "Play",
                            colorFilter = ColorFilter.tint(RavynStyle.secondary),
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        if (recent.isEmpty() && related == null) item {
            Column(Modifier.padding(24.dp)) {
                RavynLabel("Start listening", 21, bold = true)
                RavynLabel("Search for music to build your recommendations.", 13,
                    RavynStyle.secondary, lines = 2)
            }
        }
    }
}

@Composable
private fun RavynLibrary(
    category: Int, setCategory: (Int) -> Unit,
    liked: List<Song>, songs: List<Song>, local: List<Song>,
    albums: List<Album>, artists: List<Artist>, playlists: List<PlaylistPreview>,
    onAlbum: (String) -> Unit, onArtist: (String) -> Unit,
    onLocalPlaylist: (Long) -> Unit, onCreate: () -> Unit,
    onPlayAll: () -> Unit, onPlay: (List<Song>, Int) -> Unit,
    onLike: (Song) -> Unit
) {
    val categories = listOf("Liked", "Playlists", "Artists", "Albums", "Songs", "Local")
    var reverse by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEachIndexed { i, name ->
                RavynPill(name, category == i) { setCategory(i) }
            }
        }
        AnimatedContent(targetState = category,
            transitionSpec = { ContentTransform(fadeIn(tween(220)), fadeOut(tween(110)),
                sizeTransform = null) },
            label = "Library category") { current ->
            when (current) {
                0, 4, 5 -> {
                    val data = when (current) { 0 -> liked; 5 -> local; else -> songs }
                    val visible = if (reverse) data.reversed() else data
                    LazyColumn(contentPadding = PaddingValues(
                        start = 18.dp, end = 18.dp, bottom = 105.dp)) {
                        item {
                            Spacer(Modifier.height(5.dp))
                            if (current == 0) RavynHero("Liked Songs",
                                data.size.toString() + " songs", R.drawable.play, onPlayAll)
                            else {
                                RavynLabel(if (current == 4) "Your songs" else "Local music",
                                    25, bold = true)
                                Spacer(Modifier.height(8.dp))
                                RavynLabel(data.size.toString() + " songs", 12,
                                    RavynStyle.secondary)
                            }
                            Spacer(Modifier.height(15.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RavynPill(if (reverse) "Oldest" else "Recent", true) {
                                    reverse = !reverse
                                }
                                Spacer(Modifier.weight(1f))
                                RavynIcon(R.drawable.shuffle, "Shuffle", {
                                    val shuffled = data.shuffled()
                                    if (shuffled.isNotEmpty()) onPlay(shuffled, 0)
                                }, tint = ravynAccent())
                            }
                            Spacer(Modifier.height(7.dp))
                        }
                        items(visible, key = { it.id }) { song ->
                            RavynTrackRow(song, onClick = {
                                onPlay(visible, visible.indexOf(song))
                            }, onLike = { onLike(song) })
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 105.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RavynLabel("Your playlists", 25, bold = true,
                                modifier = Modifier.weight(1f))
                            RavynIcon(R.drawable.add, "New playlist", onCreate,
                                tint = ravynAccent())
                        }
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().height(78.dp)
                                .clickable { onLocalPlaylist(playlist.id) }) {
                            RavynArtwork(playlist.thumbnail, Modifier.size(58.dp))
                            Column(Modifier.weight(1f)) {
                                RavynLabel(playlist.name, 15, bold = true)
                                RavynLabel(playlist.songCount.toString() + " songs",
                                    12, RavynStyle.secondary)
                            }
                            Image(painterResource(R.drawable.chevron_forward), null,
                                colorFilter = ColorFilter.tint(RavynStyle.secondary),
                                modifier = Modifier.size(19.dp))
                        }
                    }
                }
                2 -> LazyVerticalGrid(columns = GridCells.Adaptive(125.dp),
                    contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 105.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RavynLabel("Your artists", 25, bold = true)
                    }
                    gridItems(artists, key = { it.id }) { artist ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onArtist(artist.id) }) {
                            RavynArtwork(artist.thumbnailUrl, Modifier.size(105.dp), round = true)
                            RavynLabel(artist.name.orEmpty(), 13, bold = true)
                        }
                    }
                }
                else -> LazyVerticalGrid(columns = GridCells.Adaptive(145.dp),
                    contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 105.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RavynLabel("Your albums", 25, bold = true)
                    }
                    gridItems(albums, key = { it.id }) { album ->
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onAlbum(album.id) }) {
                            RavynArtwork(album.thumbnailUrl,
                                Modifier.fillMaxWidth().height(140.dp))
                            RavynLabel(album.title.orEmpty(), 13, bold = true)
                            RavynLabel(album.authorsText.orEmpty(), 11, RavynStyle.secondary)
                        }
                    }
                }
            }
        }
    }
}
