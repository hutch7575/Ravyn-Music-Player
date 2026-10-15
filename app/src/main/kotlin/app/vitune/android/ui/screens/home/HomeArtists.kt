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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import app.vitune.android.models.Artist
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.RavynIconAction
import app.vitune.android.ui.components.themed.RavynSectionTitle
import app.vitune.android.ui.components.themed.RavynSortChip
import app.vitune.android.ui.items.ArtistItem
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.ArtistSortBy
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList

@Route
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    val colors = LocalAppearance.current.colorPalette
    var artists by persistList<Artist>("home/artists")
    var grid by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(artistSortBy, artistSortOrder) {
        Database.artists(artistSortBy, artistSortOrder)
            .collect { artists = it.toImmutableList() }
    }
    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize().background(colors.background0)) {
        LazyVerticalGrid(
            state = gridState,
            columns = if (grid) GridCells.Adaptive(150.dp) else GridCells.Fixed(1),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical).asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            item(key = "artist-header", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp)) {
                    RavynSectionTitle("Your artists", "The people behind your music")
                    Spacer(Modifier.height(17.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        RavynSortChip("A–Z", artistSortBy == ArtistSortBy.Name) {
                            artistSortBy = ArtistSortBy.Name
                        }
                        RavynSortChip("Recent", artistSortBy == ArtistSortBy.DateAdded) {
                            artistSortBy = ArtistSortBy.DateAdded
                        }
                        RavynIconAction(R.drawable.arrow_up, "Reverse artist order") {
                            artistSortOrder = !artistSortOrder
                        }
                        RavynIconAction(
                            if (grid) R.drawable.list else R.drawable.grid,
                            if (grid) "List view" else "Grid view"
                        ) { grid = !grid }
                    }
                }
            }
            items(artists, key = Artist::id) { artist ->
                ArtistItem(
                    artist = artist,
                    thumbnailSize = if (grid) 118.dp else 66.dp,
                    alternative = grid,
                    modifier = Modifier.clickable { onArtistClick(artist) }
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
