package app.vitune.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.RavynIconAction
import app.vitune.android.ui.components.themed.RavynSectionTitle
import app.vitune.android.ui.components.themed.RavynSortChip
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.persist
import app.vitune.core.data.enums.AlbumSortBy
import app.vitune.core.ui.LocalAppearance

@Route
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    val colors = LocalAppearance.current.colorPalette
    var albums by persist<List<Album>>(tag = "home/albums", emptyList())
    var grid by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(albumSortBy, albumSortOrder) {
        Database.albums(albumSortBy, albumSortOrder).collect { albums = it }
    }
    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize().background(colors.background0)) {
        LazyVerticalGrid(
            state = gridState,
            columns = if (grid) GridCells.Adaptive(164.dp) else GridCells.Fixed(1),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical).asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            item(key = "album-header", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp)) {
                    RavynSectionTitle("Your albums", "Every record, all in one place")
                    Spacer(Modifier.height(17.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        RavynSortChip("Recent", albumSortBy == AlbumSortBy.DateAdded) {
                            albumSortBy = AlbumSortBy.DateAdded
                        }
                        RavynSortChip("A–Z", albumSortBy == AlbumSortBy.Title) {
                            albumSortBy = AlbumSortBy.Title
                        }
                        RavynSortChip("Year", albumSortBy == AlbumSortBy.Year) {
                            albumSortBy = AlbumSortBy.Year
                        }
                        RavynIconAction(R.drawable.arrow_up, "Reverse album order") {
                            albumSortOrder = !albumSortOrder
                        }
                        RavynIconAction(
                            if (grid) R.drawable.list else R.drawable.grid,
                            if (grid) "List view" else "Grid view"
                        ) { grid = !grid }
                    }
                }
            }
            items(albums, key = Album::id) { album ->
                AlbumItem(
                    album = album,
                    thumbnailSize = if (grid) 132.dp else 72.dp,
                    alternative = grid,
                    modifier = Modifier.clickable { onAlbumClick(album) }
                )
            }
        }
        FloatingActionsContainerWithScrollToTop(
            lazyGridState = gridState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
