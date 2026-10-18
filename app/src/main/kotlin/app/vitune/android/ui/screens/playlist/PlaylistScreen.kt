package app.vitune.android.ui.screens.playlist

import androidx.compose.runtime.Composable
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler

@Route
@Composable
fun PlaylistScreen(
    browseId: String,
    params: String?,
    shouldDedup: Boolean,
    maxDepth: Int? = null
) {
    PersistMapCleanup(prefix = "playlist/" + browseId)
    RouteHandler {
        GlobalRoutes()
        Content {
            PlaylistSongList(
                browseId = browseId,
                params = params,
                maxDepth = maxDepth,
                shouldDedup = shouldDedup,
                onBack = pop
            )
        }
    }
}
