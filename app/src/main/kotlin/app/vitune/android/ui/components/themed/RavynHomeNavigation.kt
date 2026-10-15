package app.vitune.android.ui.components.themed

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList

/**
 * The portrait shell: a classic Material titlebar, contextual collection chips,
 * a real navigation drawer, and the three destinations from the Ravyn concept.
 * The existing sheet/player is intentionally owned by the application's host.
 */
@Composable
fun RavynHomeNavigation(
    tabs: List<Tab>,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    hiddenTabs: ImmutableList<String>,
    setHiddenTabs: (List<String>) -> Unit,
    @DrawableRes settingsIcon: Int,
    onSettingsClick: () -> Unit,
    onDrawerSettingsClick: () -> Unit = onSettingsClick,
    showDrawer: Boolean = true,
    pageTitle: String = "Ravyn",
    content: @Composable () -> Unit
) {
    val palette = LocalAppearance.current.colorPalette
    var drawerOpen by remember { mutableStateOf(false) }
    val library = tabIndex >= 2
    val insets = LocalPlayerAwareWindowInsets.current
    val topInsets = insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues()
    val bottomInset = insets.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val visibleTabs = tabs.withIndex().filter { it.index == tabIndex || it.value.key !in hiddenTabs }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(Modifier.fillMaxSize().background(palette.background0)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(topInsets)
                    .height(60.dp)
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                if (!showDrawer) {
                    NavIcon(settingsIcon, "Back", palette.text, onSettingsClick)
                    Spacer(Modifier.width(12.dp))
                }
                BasicText(
                    pageTitle,
                    style = TextStyle(
                        color = palette.text,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                        .clickable(enabled = showDrawer) { drawerOpen = true }
                )
                if (showDrawer) {
                    NavIcon(R.drawable.search, "Search", palette.text, onSettingsClick)
                    NavIcon(R.drawable.settings, "Settings", palette.text, onDrawerSettingsClick)
                }
            }

            if (showDrawer) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    if (library) {
                        val order = listOf(7, 2, 3, 4, 5, 6)
                        items(order.size) { index ->
                            val tabNumber = order[index]
                            val tab = tabs.getOrNull(tabNumber)
                            if (tab != null) RavynSortChip(
                                title = if (tabNumber == 7) "Liked" else tab.title(),
                                selected = tabIndex == tabNumber
                            ) { onTabChange(tabNumber) }
                        }
                    } else {
                        items(2) { index ->
                            RavynSortChip(
                                title = if (index == 0) "For you" else "Discover",
                                selected = tabIndex == index
                            ) { onTabChange(index) }
                        }
                    }
                }
            } else if (tabs.size > 1) {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    itemsIndexed(visibleTabs) { _, entry ->
                        RavynSortChip(entry.value.title(), entry.index == tabIndex) {
                            onTabChange(entry.index)
                        }
                    }
                }
            }

            CompositionLocalProvider(
                LocalPlayerAwareWindowInsets provides insets.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                )
            ) {
                Box(Modifier.fillMaxWidth().weight(1f)) { content() }
            }

            if (showDrawer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .background(palette.background1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomDestination("Home", R.drawable.home, !library, palette.accent,
                        Modifier.weight(1f)) { onTabChange(0) }
                    BottomDestination("Search", R.drawable.search, false, palette.accent,
                        Modifier.weight(1f), onSettingsClick)
                    BottomDestination("Your Library", R.drawable.library, library, palette.accent,
                        Modifier.weight(1f)) { onTabChange(if (library) tabIndex else 7) }
                }
            }
            Spacer(Modifier.height(bottomInset))
        }

        if (showDrawer && drawerOpen) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.64f))
                .clickable { drawerOpen = false }
        )

        if (showDrawer) AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = slideOutHorizontally(targetOffsetX = { -it },
                animationSpec = tween(200)) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Column(
                modifier = Modifier.width(294.dp).fillMaxHeight()
                    .background(palette.background1)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(palette.accent.copy(alpha = 0.13f))
                        .padding(top = topInsets.calculateTopPadding() + 35.dp,
                            start = 23.dp, bottom = 30.dp)
                ) {
                    BasicText("Ravyn", style = TextStyle(
                        fontSize = 31.sp, fontWeight = FontWeight.Bold, color = palette.text
                    ))
                    Spacer(Modifier.height(5.dp))
                    BasicText("YOUR MUSIC, YOUR WAY", style = TextStyle(
                        fontSize = 10.sp, letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold, color = palette.accent
                    ))
                }
                tabs.forEachIndexed { index, tab ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .background(if (tabIndex == index) palette.accent.copy(alpha = 0.14f)
                                else Color.Transparent)
                            .clickable {
                                onTabChange(index)
                                drawerOpen = false
                            }.padding(horizontal = 23.dp)
                    ) {
                        Image(painterResource(tab.icon), contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (tabIndex == index) palette.accent else palette.textSecondary
                            ), modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(23.dp))
                        BasicText(tab.title(), style = TextStyle(
                            color = if (tabIndex == index) palette.accent else palette.text,
                            fontSize = 15.sp, fontWeight = FontWeight.Medium
                        ))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .clickable { drawerOpen = false; onDrawerSettingsClick() }
                        .padding(horizontal = 23.dp)
                ) {
                    Image(painterResource(R.drawable.settings), null,
                        colorFilter = ColorFilter.tint(palette.textSecondary),
                        modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(23.dp))
                    BasicText("Settings", style = TextStyle(color = palette.text, fontSize = 15.sp))
                }
            }
        }
    }
}

@Composable
private fun NavIcon(@DrawableRes icon: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).semantics { contentDescription = description }) {
        Image(painterResource(icon), null,
            colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BottomDestination(
    title: String, @DrawableRes icon: Int, selected: Boolean, accent: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val palette = LocalAppearance.current.colorPalette
    val tint by animateColorAsState(
        if (selected) accent else palette.textSecondary,
        animationSpec = tween(250), label = "Navigation tint"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick)
    ) {
        Image(painterResource(icon), contentDescription = title,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(5.dp))
        BasicText(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = tint, fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
    }
}
