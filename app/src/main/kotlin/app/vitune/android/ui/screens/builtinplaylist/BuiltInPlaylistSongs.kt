package app.vitune.android.ui.screens.builtinplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.ValueSelectorDialog
import app.vitune.android.ui.ravyn.RavynIcon
import app.vitune.android.ui.ravyn.RavynLabel
import app.vitune.android.ui.ravyn.RavynPill
import app.vitune.android.ui.ravyn.RavynStyle
import app.vitune.android.ui.ravyn.RavynTrackRow
import app.vitune.android.ui.screens.home.HeaderSongSortBy
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.cachedSongs
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.core.data.enums.BuiltInPlaylist
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf

@Composable
fun BuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    onBack: () -> Unit,
    onSelect: (BuiltInPlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val menu = LocalMenuState.current
    var sortBy by rememberSaveable { mutableStateOf(SongSortBy.DateAdded) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Descending) }
    val flow = when (builtInPlaylist) {
        BuiltInPlaylist.History -> remember { Database.history() }
        BuiltInPlaylist.Favorites -> remember(sortBy, sortOrder) { Database.favorites(sortBy, sortOrder) }
        BuiltInPlaylist.Offline -> remember(binder, sortBy, sortOrder) {
            binder?.cachedSongs(sortBy, sortOrder) ?: flowOf(emptyList())
        }
        BuiltInPlaylist.Top -> {
            val period = DataPreferences.topListPeriod.duration
            val length = DataPreferences.topListLength
            remember(period, length) { Database.topSongs(length, period?.inWholeMilliseconds) }
        }
    }
    val songs by key(flow) { flow.collectAsState(initial = null) }
    val tracks = songs.orEmpty()

    fun play(queue: List<Song>, index: Int) {
        binder?.stopRadio()
        binder?.player?.forcePlayAtIndex(queue.map(Song::asMediaItem), index)
    }

    Column(
        modifier.fillMaxSize().background(RavynStyle.background)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Vertical))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(RavynStyle.panel).padding(8.dp)
        ) {
            RavynIcon(R.drawable.chevron_back, stringResource(R.string.back), onBack)
            RavynLabel(stringResource(builtInPlaylist.title), 21, bold = true, modifier = Modifier.weight(1f))
            if (tracks.isNotEmpty() && binder != null) {
                RavynIcon(R.drawable.shuffle, stringResource(R.string.shuffle), { play(tracks.shuffled(), 0) })
                RavynIcon(R.drawable.play, stringResource(R.string.play), { play(tracks, 0) })
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BuiltInPlaylist.entries.forEach { category ->
                RavynPill(stringResource(category.tab), category == builtInPlaylist) { onSelect(category) }
            }
        }
        if (builtInPlaylist.sortable) Row(Modifier.padding(horizontal = 16.dp)) {
            HeaderSongSortBy(sortBy, { sortBy = it }, sortOrder, { sortOrder = it })
        }
        if (builtInPlaylist == BuiltInPlaylist.Top) {
            var choosePeriod by rememberSaveable { mutableStateOf(false) }
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                RavynPill(DataPreferences.topListPeriod.displayName(), false) { choosePeriod = true }
            }
            if (choosePeriod) ValueSelectorDialog(
                onDismiss = { choosePeriod = false },
                title = stringResource(R.string.listening_period),
                selectedValue = DataPreferences.topListPeriod,
                values = DataPreferences.TopListPeriod.entries.toImmutableList(),
                onValueSelect = { DataPreferences.topListPeriod = it },
                valueText = { it.displayName() }
            )
        }
        when {
            songs == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            tracks.isEmpty() -> RavynLabel(
                stringResource(builtInPlaylist.emptyMessage), 14, RavynStyle.secondary,
                modifier = Modifier.padding(16.dp), lines = 3
            )
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                itemsIndexed(tracks, key = { _, song -> song.id }) { index, song ->
                    RavynTrackRow(
                        song = song,
                        onClick = { play(tracks, index) },
                        onLike = { query { Database.like(song.id, song.toggleLike().likedAt) } },
                        onLongClick = {
                            menu.display {
                                NonQueuedMediaItemMenu(onDismiss = menu::hide, mediaItem = song.asMediaItem)
                            }
                        }
                    )
                }
            }
        }
    }
}

private val BuiltInPlaylist.tab get() = when (this) {
    BuiltInPlaylist.Favorites -> R.string.liked_tab
    BuiltInPlaylist.Offline -> R.string.offline
    BuiltInPlaylist.Top -> R.string.top_tab
    BuiltInPlaylist.History -> R.string.history
}

private val BuiltInPlaylist.title get() = when (this) {
    BuiltInPlaylist.Favorites -> R.string.liked_songs_title
    BuiltInPlaylist.Offline -> R.string.offline_music_title
    BuiltInPlaylist.Top -> R.string.top_songs_title
    BuiltInPlaylist.History -> R.string.listening_history_title
}

private val BuiltInPlaylist.emptyMessage get() = when (this) {
    BuiltInPlaylist.Favorites -> R.string.no_liked_songs
    BuiltInPlaylist.Offline -> R.string.no_offline_music
    BuiltInPlaylist.Top -> R.string.no_ranked_songs
    BuiltInPlaylist.History -> R.string.no_listening_history
}
