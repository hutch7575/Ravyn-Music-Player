package app.vitune.android.ui.screens.searchresult

import androidx.compose.runtime.Composable
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.ui.ravyn.RavynSearchResults
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler

@Route
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val binder = LocalPlayerServiceBinder.current
    PersistMapCleanup(prefix = "searchResults/" + query + "/")
    RouteHandler {
        GlobalRoutes()
        Content {
            RavynSearchResults(
                query = query,
                onBack = pop,
                onSearchAgain = onSearchAgain,
                onAlbum = { albumRoute(it) },
                onArtist = { artistRoute(it) },
                onPlaylist = { playlistRoute(it, null, null, false) },
                onSong = { song ->
                    val media = song.asMediaItem
                    binder?.stopRadio()
                    binder?.player?.forcePlay(media)
                    binder?.setupRadio(song.info?.endpoint)
                },
                onVideo = { video ->
                    val media = video.asMediaItem
                    binder?.stopRadio()
                    binder?.player?.forcePlay(media)
                    binder?.setupRadio(video.info?.endpoint)
                }
            )
        }
    }
}
