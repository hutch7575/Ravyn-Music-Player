package app.vitune.android.ui.screens.builtinplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.data.enums.BuiltInPlaylist

object BuiltInPlaylistScreen {
    internal const val KEY = "builtinplaylist"

    @Composable
    fun shownPlaylistsAsState(): State<List<BuiltInPlaylist>> {
        val hidden by UIStatePreferences.mutableTabStateOf(KEY)
        return remember {
            derivedStateOf {
                BuiltInPlaylist.entries.filter { it.ordinal.toString() !in hidden }
            }
        }
    }
}

@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val [index, setIndex] = rememberSaveable {
        mutableIntStateOf(builtInPlaylist.ordinal)
    }
    PersistMapCleanup(prefix = builtInPlaylist.name + "/")
    RouteHandler {
        GlobalRoutes()
        Content {
            BuiltInPlaylistSongs(
                builtInPlaylist = BuiltInPlaylist.entries[index],
                onBack = pop,
                onSelect = { setIndex(it.ordinal) }
            )
        }
    }
}
