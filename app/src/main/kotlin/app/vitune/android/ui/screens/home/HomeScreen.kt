package app.vitune.android.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.handleUrl
import app.vitune.android.models.SearchQuery
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.ravyn.LocalRavynNavigation
import app.vitune.android.ui.screens.searchResultRoute
import app.vitune.android.utils.toast
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import app.vitune.android.R
import app.vitune.android.models.toUiMood
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.ravyn.RavynRoot
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.builtInPlaylistRoute
import app.vitune.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.vitune.android.ui.screens.builtinplaylist.BuiltInPlaylistSongs
import app.vitune.core.data.enums.BuiltInPlaylist
import app.vitune.android.ui.screens.localPlaylistRoute
import app.vitune.android.ui.screens.localplaylist.LocalPlaylistScreen
import app.vitune.android.ui.screens.mood.MoodScreen
import app.vitune.android.ui.screens.mood.MoreAlbumsScreen
import app.vitune.android.ui.screens.mood.MoreMoodsScreen
import app.vitune.android.ui.screens.moodRoute
import app.vitune.android.ui.screens.pipedPlaylistRoute
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.ui.screens.searchRoute
import app.vitune.android.ui.screens.settingsRoute
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.Route0
import app.vitune.compose.routing.RouteHandler

private val moreMoodsRoute = Route0("moreMoodsRoute")
private val moreAlbumsRoute = Route0("moreAlbumsRoute")

@Route
@Composable
fun HomeScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    val navigation = LocalRavynNavigation.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    RouteHandler {
        navigation.navigate = { tab ->
            navigation.destination.intValue = tab
            // Dismiss an open search-result/album/settings route, not only the
            // hidden Home tab underneath it.
            replace(null)
        }
        GlobalRoutes()

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(playlistId = playlistId)
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
        }

        moodRoute { mood ->
            MoodScreen(mood = mood)
        }

        moreMoodsRoute {
            MoreMoodsScreen()
        }

        moreAlbumsRoute {
            MoreAlbumsScreen()
        }

        Content {
            RavynRoot(
                onSearchSubmit = { text ->
                    searchResultRoute(text)
                    if (!DataPreferences.pauseSearchHistory) {
                        query { Database.insert(SearchQuery(query = text)) }
                    }
                },
                onPlaylistUrl = { url ->
                    with(context) {
                        runCatching { handleUrl(url.toUri(), binder) }
                            .onFailure { toast(getString(R.string.error_url, url)) }
                    }
                },
                onSettings = { settingsRoute() },
                onHistory = { builtInPlaylistRoute(BuiltInPlaylist.History) },
                onDiscover = { navigation.navigate(1) },
                onAlbum = { albumRoute(it) },
                onArtist = { artistRoute(it) },
                onOnlinePlaylist = {
                    playlistRoute(
                        p0 = it.key, p1 = null, p2 = null,
                        p3 = it.channel?.name == "YouTube Music"
                    )
                },
                onLocalPlaylist = { localPlaylistRoute(it) }
            )
        }
    }
}
