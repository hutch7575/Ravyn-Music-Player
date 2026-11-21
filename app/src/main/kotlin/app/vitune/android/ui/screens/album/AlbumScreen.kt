package app.vitune.android.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.vitune.android.Database
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.models.Song
import app.vitune.android.models.SongAlbumMap
import app.vitune.android.query
import app.vitune.android.transaction
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.HeaderPlaceholder
import app.vitune.android.ui.components.themed.PlaylistInfo
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.adaptiveThumbnailContent
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.items.AlbumItemPlaceholder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.searchresult.ItemsPage
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.completed
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.persist.persistList
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.stateFlowSaver
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.requests.albumPage
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Route
@Composable
fun AlbumScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val tabIndexState = rememberSaveable(saver = stateFlowSaver()) { MutableStateFlow(0) }
    val tabIndex by tabIndexState.collectAsState()

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")
    var songs by persistList<Song>("album/$browseId/songs")

    PersistMapCleanup(prefix = "album/$browseId/")

    var loading by remember(browseId) { mutableStateOf(albumPage == null) }
    var failed by remember(browseId) { mutableStateOf(false) }
    var retry by remember(browseId) { mutableIntStateOf(0) }

    LaunchedEffect(browseId) {
        Database.albumSongs(browseId).distinctUntilChanged().collect { songs = it.toImmutableList() }
    }
    LaunchedEffect(browseId) {
        Database.album(browseId).distinctUntilChanged().collect { album = it }
    }
    // Network requests must not run inside Room's observers: writes and likes
    // invalidate those observers and used to trigger another full album fetch.
    LaunchedEffect(browseId, retry) {
        loading = true
        failed = false
        try {
            val page = withContext(Dispatchers.IO) {
                Innertube.albumPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
            }
            if (page == null) failed = true else {
                albumPage = page
                transaction {
                    Database.clearAlbum(browseId)
                    Database.upsert(
                        album = Album(id = browseId, title = page.title,
                            description = page.description, thumbnailUrl = page.thumbnail?.url,
                            year = page.year, authorsText = page.authors?.joinToString { it.name.orEmpty() },
                            shareUrl = page.url, timestamp = System.currentTimeMillis(),
                            bookmarkedAt = album?.bookmarkedAt, otherInfo = page.otherInfo),
                        songAlbumMaps = page.songsPage?.items?.map { it.asMediaItem }
                            ?.onEach { Database.insert(it) }?.mapIndexed { position, item ->
                                SongAlbumMap(songId = item.mediaId, albumId = browseId, position = position)
                            }.orEmpty()
                    )
                }
            }
        } finally { loading = false }
    }

    RouteHandler {
        GlobalRoutes()
        Content {
            val binder = app.vitune.android.LocalPlayerServiceBinder.current
            app.vitune.android.ui.ravyn.RavynCollectionDetail(
                title = album?.title ?: albumPage?.title ?: "Album",
                artist = album?.authorsText ?: albumPage?.authors
                    ?.joinToString { it.name.orEmpty() },
                artwork = album?.thumbnailUrl ?: albumPage?.thumbnail?.url,
                tracks = songs,
                onBack = pop,
                onPlay = { index ->
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(
                        songs.map { it.asMediaItem }, index
                    )
                },
                onShuffle = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(
                        songs.shuffled().map { it.asMediaItem }
                    )
                },
                onLike = { song ->
                    query {
                        Database.like(song.id,
                            if (song.likedAt == null) System.currentTimeMillis() else null)
                    }
                },
                bookmarked = album?.bookmarkedAt != null,
                onBookmark = {
                    album?.let { current ->
                        query {
                            Database.update(current.copy(bookmarkedAt =
                                if (current.bookmarkedAt == null) System.currentTimeMillis()
                                else null))
                        }
                    }
                },
                emptyMessage = "This album has no tracks.",
                loading = loading, failed = failed, onRetry = { retry++ }
            )
        }
    }
}
