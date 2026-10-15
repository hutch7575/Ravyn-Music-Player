package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.requests.searchPage
import app.vitune.providers.innertube.utils.from
import kotlinx.coroutines.launch

/** Online search results with full bespoke Ravyn rows, chips and pagination. */
@Composable
fun RavynSearchResults(
    query: String,
    onBack: () -> Unit,
    onSearchAgain: () -> Unit,
    onSong: (Innertube.SongItem) -> Unit,
    onVideo: (Innertube.VideoItem) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlaylist: (String) -> Unit
) {
    var tab by rememberSaveable(query) { mutableStateOf(0) }
    val insets = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    val filters = listOf("Songs", "Albums", "Artists", "Videos", "Playlists")
    Column(Modifier.fillMaxSize().background(RavynStyle.background)
        .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(66.dp).padding(start = 4.dp, end = 15.dp)) {
            RavynIcon(R.drawable.chevron_back, "Back", onBack)
            Column(Modifier.weight(1f).clickable(onClick = onSearchAgain)) {
                RavynLabel("Search results", 12, RavynStyle.secondary)
                RavynLabel(query, 23, bold = true)
            }
            RavynIcon(R.drawable.search, "Search again", onSearchAgain)
        }
        Row(Modifier.horizontalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { i, label ->
                RavynPill(label, tab == i) { tab = i }
            }
        }
        AnimatedContent(tab,
            transitionSpec = { ContentTransform(fadeIn(tween(220)),
                fadeOut(tween(120)), sizeTransform = null) },
            label = "Search filters", modifier = Modifier.weight(1f)) { section ->
            when (section) {
                0 -> RavynResultList(
                    query, section,
                    provider = { continuation ->
                        if (continuation == null) Innertube.searchPage(
                            body = SearchBody(query = query,
                                params = Innertube.SearchFilter.Song.value),
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        ) else Innertube.searchPage(
                            body = ContinuationBody(continuation),
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        )
                    },
                    itemContent = { item ->
                        RavynResultRow(
                            item.thumbnail?.url, item.info?.name.orEmpty(),
                            item.authors?.joinToString { it.name.orEmpty() }.orEmpty()
                        ) { onSong(item) }
                    }
                )
                1 -> RavynResultList(
                    query, section,
                    provider = { continuation ->
                        if (continuation == null) Innertube.searchPage(
                            body = SearchBody(query = query,
                                params = Innertube.SearchFilter.Album.value),
                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                        ) else Innertube.searchPage(
                            body = ContinuationBody(continuation),
                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                        )
                    },
                    itemContent = { item ->
                        RavynResultRow(
                            item.thumbnail?.url, item.info?.name.orEmpty(),
                            item.authors?.joinToString { it.name.orEmpty() }.orEmpty()
                        ) { onAlbum(item.key) }
                    }
                )
                2 -> RavynResultList(
                    query, section,
                    provider = { continuation ->
                        if (continuation == null) Innertube.searchPage(
                            body = SearchBody(query = query,
                                params = Innertube.SearchFilter.Artist.value),
                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                        ) else Innertube.searchPage(
                            body = ContinuationBody(continuation),
                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                        )
                    },
                    itemContent = { item ->
                        RavynResultRow(
                            item.thumbnail?.url, item.info?.name.orEmpty(),
                            item.subscribersCountText.orEmpty(), round = true
                        ) { onArtist(item.key) }
                    }
                )
                3 -> RavynResultList(
                    query, section,
                    provider = { continuation ->
                        if (continuation == null) Innertube.searchPage(
                            body = SearchBody(query = query,
                                params = Innertube.SearchFilter.Video.value),
                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                        ) else Innertube.searchPage(
                            body = ContinuationBody(continuation),
                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                        )
                    },
                    itemContent = { item ->
                        RavynResultRow(
                            item.thumbnail?.url, item.info?.name.orEmpty(),
                            item.authors?.joinToString { it.name.orEmpty() }.orEmpty()
                        ) { onVideo(item) }
                    }
                )
                else -> RavynResultList(
                    query, section,
                    provider = { continuation ->
                        if (continuation == null) Innertube.searchPage(
                            body = SearchBody(query = query,
                                params = Innertube.SearchFilter.CommunityPlaylist.value),
                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                        ) else Innertube.searchPage(
                            body = ContinuationBody(continuation),
                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                        )
                    },
                    itemContent = { item ->
                        RavynResultRow(
                            item.thumbnail?.url, item.info?.name.orEmpty(),
                            item.channel?.name.orEmpty()
                        ) { onPlaylist(item.key) }
                    }
                )
            }
        }
    }
}

@Composable
private fun <T : Innertube.Item> RavynResultList(
    query: String, category: Int,
    provider: suspend (String?) -> Result<Innertube.ItemsPage<T>?>?,
    itemContent: @Composable (T) -> Unit
) {
    var page by remember(query, category) {
        mutableStateOf<Innertube.ItemsPage<T>?>(null)
    }
    var loading by remember(query, category) { mutableStateOf(false) }
    var failed by remember(query, category) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(query, category) {
        loading = true
        failed = false
        val response = runCatching { provider(null)?.getOrNull() }.getOrNull()
        page = response
        failed = response == null
        loading = false
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 18.dp, end = 18.dp, bottom = 25.dp
    )) {
        items(page?.items.orEmpty(), key = { it.key }) { item ->
            itemContent(item)
        }
        if (loading) item { RavynLabel("Loading music...", 13, RavynStyle.secondary,
            modifier = Modifier.padding(vertical = 20.dp)) }
        if (failed && !loading) item {
            Column {
                RavynLabel("Couldn't load these results.", 14, RavynStyle.secondary)
                Spacer(Modifier.height(10.dp))
                RavynPill("Retry", false) {
                    scope.launch {
                        loading = true
                        page = runCatching { provider(null)?.getOrNull() }.getOrNull()
                        failed = page == null
                        loading = false
                    }
                }
            }
        }
        if (page != null && page?.items.isNullOrEmpty() && !loading) item {
            RavynLabel("No results found.", 14, RavynStyle.secondary,
                modifier = Modifier.padding(vertical = 18.dp))
        }
        if (page?.continuation != null && !loading) item {
            Spacer(Modifier.height(12.dp))
            RavynPill("Load more results", false) {
                val token = page?.continuation
                if (token != null) scope.launch {
                    loading = true
                    val next = runCatching { provider(token)?.getOrNull() }.getOrNull()
                    if (next != null) page = page?.copy(
                        items = page?.items.orEmpty() + next.items.orEmpty(),
                        continuation = next.continuation
                    )
                    loading = false
                }
            }
        }
    }
}

@Composable
private fun RavynResultRow(
    image: String?, title: String, subtitle: String,
    round: Boolean = false, onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().height(76.dp)
            .clickable(onClick = onClick)) {
        RavynArtwork(image, Modifier.size(58.dp), round = round)
        Column(Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            RavynLabel(title, 14, bold = true)
            RavynLabel(subtitle, 12, RavynStyle.secondary)
        }
        Image(painterResource(R.drawable.chevron_forward), null,
            colorFilter = ColorFilter.tint(RavynStyle.secondary),
            modifier = Modifier.size(18.dp))
    }
}
