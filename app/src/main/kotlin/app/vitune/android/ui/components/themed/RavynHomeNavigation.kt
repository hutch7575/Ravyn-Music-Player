package app.vitune.android.ui.components.themed

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val classicPurple = Color(0xFF9A77F4)
private val classicPurpleDark = Color(0xFF24213F)
private val navText = Color.White

/**
 * Compact 2016 Material app bar, scrollable category tabs and a proper drawer.
 * The original NavigationRail remains available in landscape and other screens.
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
    var drawerOpen by remember { mutableStateOf(false) }
    var editingTabs by remember { mutableStateOf(false) }
    val insets = LocalPlayerAwareWindowInsets.current
    val topInset = insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        .asPaddingValues()
    val visible = remember(tabs, hiddenTabs, tabIndex) {
        tabs.withIndex()
            .filter { (index, tab) -> index == tabIndex || tab.key !in hiddenTabs }
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF24203F), Color(0xFF191B32))
                        )
                    )
                    .padding(topInset)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { if (showDrawer) drawerOpen = true else onSettingsClick() }
                            .semantics { contentDescription = "Open navigation menu" }
                    ) {
                        if (showDrawer) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) {
                                    Spacer(
                                        Modifier.width(20.dp).height(2.dp).background(Color.White)
                                    )
                                }
                            }
                        } else {
                            Image(
                                painter = painterResource(settingsIcon),
                                contentDescription = "Back",
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    BasicText(
                        text = pageTitle,
                        style = TextStyle(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Medium,
                            color = navText,
                            letterSpacing = (-0.35).sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (showDrawer) Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .clickable(onClick = onSettingsClick)
                    ) {
                        Image(
                            painter = painterResource(settingsIcon),
                            contentDescription = "Settings",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                // Scrollable Material tabs: navigation stays fast even with
                // seven categories and narrow/older Android displays.
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(47.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(visible, key = { _, entry -> entry.value.key }) { _, entry ->
                        val selected = entry.index == tabIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .height(47.dp)
                                .clickable { onTabChange(entry.index) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 17.dp)
                            ) {
                                BasicText(
                                    text = entry.value.title().uppercase(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(
                                        color = if (selected) Color.White
                                            else Color(0xFFB7B7CF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.45.sp
                                    )
                                )
                            }
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        if (selected) classicPurple else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }

            // The header consumes the status-bar inset. Screens must only add
            // their usual horizontal and bottom insets, avoiding double gaps.
            androidx.compose.runtime.CompositionLocalProvider(
                LocalPlayerAwareWindowInsets provides insets.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                )
            ) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    content()
                }
            }
        }

        if (showDrawer && drawerOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable { drawerOpen = false }
            )
        }

        if (showDrawer) AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(230, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(130)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(190, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(170)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Column(
                Modifier
                    .width(296.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF171B2B))
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF423077), Color(0xFF1D284C), Color(0xFF11182A))
                            )
                        )
                        .padding(top = topInset.calculateTopPadding(), start = 22.dp)
                ) {
                    BasicText(
                        text = "Ravyn",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 29.sp
                        )
                    )
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        "YOUR MUSIC, YOUR WAY",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.1.sp
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                tabs.forEachIndexed { index, tab ->
                    val selected = tabIndex == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(49.dp)
                            .background(
                                if (selected) Color(0xFF333050) else Color.Transparent
                            )
                            .clickable(enabled = !editingTabs) {
                                onTabChange(index)
                                drawerOpen = false
                            }
                            .padding(horizontal = 22.dp)
                    ) {
                        Image(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (selected) classicPurple else Color(0xFF9095B3)
                            ),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(28.dp))
                        BasicText(
                            text = tab.title(),
                            style = TextStyle(
                                color = if (selected) Color.White else Color(0xFFD7D8E9),
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.SemiBold
                                    else FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (editingTabs) {
                            Checkbox(
                                checked = tab.key !in hiddenTabs,
                                enabled = tab.canHide && (tab.key in hiddenTabs ||
                                    tabs.size - hiddenTabs.size > 1),
                                onCheckedChange = { show ->
                                    setHiddenTabs(
                                        (if (show) hiddenTabs - tab.key
                                         else hiddenTabs + tab.key).toList()
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF37394E))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { editingTabs = !editingTabs }
                        .padding(horizontal = 22.dp)
                ) {
                    BasicText(
                        if (editingTabs) "DONE CUSTOMISING" else "CUSTOMISE TABS",
                        style = TextStyle(
                            color = classicPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            drawerOpen = false
                            onDrawerSettingsClick()
                        }
                        .padding(horizontal = 22.dp)
                ) {
                    Image(
                        painter = painterResource(settingsIcon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFF9095B3)),
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(28.dp))
                    BasicText(
                        "Settings",
                        style = TextStyle(
                            color = Color(0xFFD7D8E9),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
