package app.vitune.android.ui.screens.pipedplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.ShimmerHost
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderPlaceholder
import app.vitune.android.ui.components.themed.LayoutWithAdaptiveThumbnail
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.adaptiveThumbnailContent
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.utils.PlaylistDownloadIcon
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.android.utils.playingSong
import app.vitune.compose.persist.persist
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isLandscape
import app.vitune.providers.piped.Piped
import app.vitune.providers.piped.models.Playlist
import app.vitune.providers.piped.models.Session
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun PipedPlaylistSongList(
    session: Session,
    playlistId: UUID,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var playlist by persist<Playlist>(tag = "pipedplaylist/$playlistId/playlistPage")
    val mediaItems = remember(playlist) {
        playlist?.videos?.mapNotNull { it.asMediaItem }?.toImmutableList()
    }

    LaunchedEffect(Unit) {
        playlist = withContext(Dispatchers.IO) {
            Piped.playlist.songs(
                session = session,
                id = playlistId
            )?.getOrNull()
        }
    }


    app.vitune.android.ui.ravyn.RavynMediaPlaylist(
        title = playlist?.name ?: "Playlist",
        artwork = playlist?.thumbnailUrl?.toString(),
        media = mediaItems.orEmpty(),
        onBack = onBack,
        onPlay = { index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(mediaItems.orEmpty(), index)
        },
        onShuffle = {
            binder?.stopRadio()
            binder?.player?.forcePlayFromBeginning(mediaItems.orEmpty().shuffled())
        },
        onQueue = {
            binder?.player?.enqueue(mediaItems.orEmpty())
        }
    )
}
