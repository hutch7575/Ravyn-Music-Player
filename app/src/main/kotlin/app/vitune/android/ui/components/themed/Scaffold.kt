package app.vitune.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vitune.android.R
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isLandscape
import kotlinx.collections.immutable.toImmutableList

@Composable
fun Scaffold(
    key: String,
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    onDrawerSettingsClick: () -> Unit = onTopIconButtonClick,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    tabColumnContent: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)

    val showClassicHome = !isLandscape
    val tabContent: @Composable () -> Unit = {
        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(tween(155, easing = FastOutSlowInEasing)),
                    initialContentExit = fadeOut(tween(90)),
                    sizeTransform = null
                )
            },
            content = content,
            label = "Tabs"
        )
    }

    if (showClassicHome) {
        val tabs = TabsBuilder.rememberTabs(tabColumnContent)
        RavynHomeNavigation(
            tabs = tabs,
            tabIndex = tabIndex,
            onTabChange = onTabChange,
            hiddenTabs = hiddenTabs,
            setHiddenTabs = { hiddenTabs = it.toImmutableList() },
            settingsIcon = topIconButtonId,
            onSettingsClick = onTopIconButtonClick,
            onDrawerSettingsClick = onDrawerSettingsClick,
            showDrawer = key == "home",
            pageTitle = when (key) {
                "settings" -> "Settings"
                "home" -> "Ravyn"
                else -> "Search"
            },
            content = tabContent
        )
    } else {
        Row(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            NavigationRail(
                topIconButtonId = topIconButtonId,
                onTopIconButtonClick = onTopIconButtonClick,
                tabIndex = tabIndex,
                onTabIndexChange = onTabChange,
                hiddenTabs = hiddenTabs,
                setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                tabsEditingTitle = tabsEditingTitle,
                content = tabColumnContent
            )
            Box(Modifier.weight(1f)) { tabContent() }
        }
    }
}
