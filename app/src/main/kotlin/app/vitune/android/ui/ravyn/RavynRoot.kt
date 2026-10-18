package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
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
import app.vitune.android.preferences.DataPreferences
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay


/** An empty successful network result should not replace a populated cache. */
private fun Innertube.RelatedPage.hasRavynRecommendations(): Boolean =
    !artists.isNullOrEmpty() || !songs.isNullOrEmpty() ||
        !albums.isNullOrEmpty() || !playlists.isNullOrEmpty()

/** Brand-new Ravyn portrait shell, retaining only existing data and playback APIs. */
@Composable
fun RavynRoot(
    onSearchSubmit: (String) -> Unit,
    onPlaylistUrl: (String) -> Unit,
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
    val tab by ravynNavigation.destination
    val tabStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
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
    var historyLoaded by remember { mutableStateOf(false) }
    var artistsLoaded by remember { mutableStateOf(false) }
    var playlistsLoaded by remember { mutableStateOf(false) }
    var albumsLoaded by remember { mutableStateOf(false) }
    var relatedLoaded by remember { mutableStateOf(false) }
    var trendingSeed by remember { mutableStateOf<String?>(null) }
    var relatedError by remember { mutableStateOf(false) }
    var refreshRecommendations by remember { mutableStateOf(0) }

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
            historyLoaded = true
        }
    }
    LaunchedEffect(Unit) {
        Database.albums(AlbumSortBy.DateAdded, SortOrder.Descending).collect {
            albums = it
            albumsLoaded = true
        }
    }
    LaunchedEffect(Unit) {
        Database.artists(ArtistSortBy.Name, SortOrder.Ascending).collect {
            artists = it
            artistsLoaded = true
        }
    }
    LaunchedEffect(Unit) {
        Database.playlistPreviews(PlaylistSortBy.DateAdded, SortOrder.Descending)
            .collect {
                playlists = it
                playlistsLoaded = true
            }
    }
    // Preserve the Quick Picks cache from the existing app. It provides a
    // useful Home feed immediately after opening Ravyn, even if the network
    // is slow and the play history has not been collected yet.
    LaunchedEffect(Unit) {
        if (DataPreferences.shouldCacheQuickPicks) {
            val cached = DataPreferences.cachedQuickPicks
            if (cached.hasRavynRecommendations()) {
                related = cached
                relatedLoaded = true
            }
        }
    }

    // A fresh install may have no listening history at all. The former ViTune
    // Home used trending tracks (or a stable discovery seed) for exactly this case.
    LaunchedEffect(Unit) {
        Database.trending().collect { tracks ->
            trendingSeed = tracks.firstOrNull()?.id
        }
    }

    LaunchedEffect(historyLoaded, recent.firstOrNull()?.id, trendingSeed,
        refreshRecommendations, DataPreferences.quickPicksSource) {
        if (!historyLoaded) return@LaunchedEffect
        val seed = when (DataPreferences.quickPicksSource) {
            DataPreferences.QuickPicksSource.LastInteraction ->
                recent.firstOrNull { !it.id.startsWith("local:") }?.id ?: trendingSeed
            DataPreferences.QuickPicksSource.Trending ->
                trendingSeed ?: recent.firstOrNull { !it.id.startsWith("local:") }?.id
        } ?: "J7p4bzqLvCw"

        // Never clear valid artwork and recommendations just to show a loader.
        val fresh = runCatching {
            Innertube.relatedPage(NextBody(videoId = seed))?.getOrNull()
        }.getOrNull()
        if (fresh != null && fresh.hasRavynRecommendations()) {
            related = fresh
            relatedError = false
            if (DataPreferences.shouldCacheQuickPicks) {
                DataPreferences.cachedQuickPicks = fresh
            }
        } else {
            relatedError = related?.hasRavynRecommendations() != true
        }
        relatedLoaded = true
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
                RavynLabel(if (tab == 1) "Search" else "Your Library",
                    size = 21, bold = true, modifier = Modifier.weight(1f))
            }
            RavynIcon(R.drawable.history, "History", onHistory)
            RavynIcon(R.drawable.settings, "Settings", onSettings)
        }
        AnimatedContent(
            targetState = tab, modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                ContentTransform(
                    fadeIn(tween(270, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) {
                            if (forward) it / 8 else -it / 8
                        },
                    fadeOut(tween(160)) +
                        slideOutHorizontally(tween(240)) {
                            if (forward) -it / 12 else it / 12
                        },
                    sizeTransform = null
                )
            }, label = "Ravyn destinations"
        ) { active ->
            tabStates.SaveableStateProvider(active) {
            if (active == 0) RavynClassicHome(
                recent = recent, artists = artists, albums = albums,
                playlists = playlists, related = related,
                historyLoaded = historyLoaded, artistsLoaded = artistsLoaded,
                playlistsLoaded = playlistsLoaded, albumsLoaded = albumsLoaded,
                relatedLoaded = relatedLoaded, relatedError = relatedError,
                onRetry = { refreshRecommendations += 1 },
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
            ) else if (active == 1) {
                RavynSearchPage(
                    initialQuery = "",
                    embedded = true,
                    onBack = { ravynNavigation.navigate(0) },
                    onSubmit = onSearchSubmit,
                    onPlaylistUrl = onPlaylistUrl
                )
            } else RavynClassicLibrary(
                category = collection, setCategory = { collection = it },
                liked = liked, songs = songs, local = local, albums = albums,
                artists = artists, playlists = playlists,
                onAlbum = onAlbum, onArtist = onArtist, onLocalPlaylist = onLocalPlaylist,
                onCreate = { showCreate = true },
                onAddSongs = { ravynNavigation.navigate(1) },
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
        }
        RavynBottomNavigation()
        Spacer(Modifier.height(WindowInsets.systemBars
            .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()))
    }
}

