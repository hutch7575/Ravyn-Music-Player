package app.vitune.android.ui.screens.builtinplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.RavynSortChip
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.InHistoryMediaItemMenu
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.ValueSelectorDialog
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.screens.home.HeaderSongSortBy
import app.vitune.android.utils.PlaylistDownloadIcon
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.android.utils.playingSong
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.BuiltInPlaylist
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.enumSaver
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun BuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    onBack: () -> Unit = {},
    onSelect: (BuiltInPlaylist) -> Unit = {},
    modifier: Modifier = Modifier
) = with(DataPreferences) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var songs by persistList<Song>("${builtInPlaylist.name}/songs")

    var sortBy by rememberSaveable(stateSaver = enumSaver()) { mutableStateOf(SongSortBy.DateAdded) }
    var sortOrder by rememberSaveable(stateSaver = enumSaver()) { mutableStateOf(SortOrder.Descending) }

    LaunchedEffect(binder, sortBy, sortOrder) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> Database.favorites(
                sortBy = sortBy,
                sortOrder = sortOrder
            )

            BuiltInPlaylist.Offline ->
                Database
                    .songsWithContentLength(
                        sortBy = sortBy,
                        sortOrder = sortOrder
                    )
                    .map { songs ->
                        songs.filter { binder?.isCached(it) ?: false }.map { it.song }
                    }

            BuiltInPlaylist.Top -> combine(
                flow = topListPeriodProperty.stateFlow,
                flow2 = topListLengthProperty.stateFlow
            ) { period, length -> period to length }.flatMapLatest { [period, length] ->
                if (period.duration == null) Database
                    .songsByPlayTimeDesc(limit = length)
                    .distinctUntilChanged()
                    .cancellable()
                else Database
                    .trending(
                        limit = length,
                        period = period.duration.inWholeMilliseconds
                    )
                    .distinctUntilChanged()
                    .cancellable()
            }

            BuiltInPlaylist.History -> Database.history()
        }.collect { songs = it.toImmutableList() }
    }


    app.vitune.android.ui.ravyn.RavynCollectionDetail(
        title = when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> "Liked Songs"
            BuiltInPlaylist.Offline -> "Offline music"
            BuiltInPlaylist.Top -> "Your top songs"
            BuiltInPlaylist.History -> "Listening history"
        },
        artist = "Your music",
        artwork = songs.firstOrNull()?.thumbnailUrl,
        tracks = songs,
        onBack = onBack,
        onPlay = { index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(songs.map { it.asMediaItem }, index)
        },
        onShuffle = {
            binder?.stopRadio()
            binder?.player?.forcePlayFromBeginning(
                songs.shuffled().map { it.asMediaItem }
            )
        },
        onLike = { song ->
            app.vitune.android.query {
                Database.like(song.id,
                    if (song.likedAt == null) System.currentTimeMillis() else null)
            }
        },
        subnav = {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 17.dp, vertical = 11.dp)
            ) {
                listOf("Liked", "Offline", "Top", "History")
                    .forEachIndexed { index, label ->
                        app.vitune.android.ui.ravyn.RavynPill(
                            label, builtInPlaylist.ordinal == index
                        ) { onSelect(BuiltInPlaylist.entries[index]) }
                    }
            }
        }
    )
}
