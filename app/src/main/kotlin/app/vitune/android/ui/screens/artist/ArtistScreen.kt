package app.vitune.android.ui.screens.artist

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Artist
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.preferences.UIStatePreferences.artistScreenTabIndexProperty
import app.vitune.android.query
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.HeaderPlaceholder
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.adaptiveThumbnailContent
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.items.AlbumItemPlaceholder
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.searchresult.ItemsPage
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.playingSong
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.requests.artistPage
import app.vitune.providers.innertube.requests.itemsPage
import app.vitune.providers.innertube.utils.from
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Route
@Composable
fun ArtistScreen(browseId: String) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "artist/$browseId/")

    var artist by persist<Artist?>("artist/$browseId/artist")

    var artistPage by persist<Innertube.ArtistPage?>("artist/$browseId/artistPage")

    var loading by remember(browseId) { mutableStateOf(artistPage == null) }
    var failed by remember(browseId) { mutableStateOf(false) }
    var retry by remember(browseId) { mutableIntStateOf(0) }
    LaunchedEffect(browseId) {
        Database.artist(browseId).distinctUntilChanged().collect { artist = it }
    }
    LaunchedEffect(browseId, retry) {
        loading = true
        failed = false
        try {
            val page = withContext(Dispatchers.IO) {
                Innertube.artistPage(BrowseBody(browseId = browseId))?.getOrNull()
            }
            if (page == null) failed = true else {
                artistPage = page
                query {
                    Database.upsert(Artist(id = browseId, name = page.name,
                        thumbnailUrl = page.thumbnail?.url, timestamp = System.currentTimeMillis(),
                        bookmarkedAt = artist?.bookmarkedAt))
                }
            }
        } finally { loading = false }
    }

    RouteHandler {
        GlobalRoutes()
        Content {
            app.vitune.android.ui.ravyn.RavynArtistDetail(
                artist = artist,
                artistPage = artistPage,
                loading = loading, failed = failed, onRetry = { retry++ },
                onBack = pop,
                onAlbum = { albumRoute(it) },
                onBookmark = {
                    artist?.let { current ->
                        query {
                            Database.upsert(
                                current.copy(bookmarkedAt =
                                    if (current.bookmarkedAt == null) System.currentTimeMillis()
                                    else null)
                            )
                        }
                    }
                },
                onSong = { song ->
                    binder?.stopRadio()
                    binder?.player?.forcePlay(song.asMediaItem)
                    binder?.setupRadio(song.info?.endpoint)
                }
            )
        }
    }
}
