package app.vitune.android.ui.screens.search

import androidx.compose.runtime.Composable
import app.vitune.android.ui.ravyn.RavynSearchPage
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler

@Route
@Composable
fun SearchScreen(
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit
) {
    PersistMapCleanup(prefix = "search/")
    RouteHandler {
        GlobalRoutes()
        Content {
            RavynSearchPage(
                initialQuery = initialTextInput,
                onBack = pop,
                onSubmit = onSearch,
                onPlaylistUrl = onViewPlaylist
            )
        }
    }
}
