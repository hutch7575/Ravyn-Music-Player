package app.vitune.android.ui.screens.pipedplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.providers.piped.models.authenticatedWith
import io.ktor.http.Url
import java.util.UUID

@Route
@Composable
fun PipedPlaylistScreen(
    apiBaseUrl: Url,
    sessionToken: String,
    playlistId: UUID
) {
    val session by remember { derivedStateOf { apiBaseUrl authenticatedWith sessionToken } }
    PersistMapCleanup(prefix = "pipedplaylist/" + playlistId)
    RouteHandler {
        GlobalRoutes()
        Content {
            PipedPlaylistSongList(session = session, playlistId = playlistId, onBack = pop)
        }
    }
}
