package app.vitune.android.ui.ravyn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R

/**
 * Navigation is shared by the app host and its player instead of being inside
 * the Home scroll view. When music plays, the player owns the entire bottom dock.
 */
@Stable
class RavynNavigationState {
    val destination = mutableIntStateOf(0)
    /** Changes the visible destination and clears nested route overlays. */
    var navigate: (Int) -> Unit = { index -> destination.intValue = index }
}

val LocalRavynNavigation = staticCompositionLocalOf { RavynNavigationState() }

val RavynBottomNavigationHeight = 54.dp

@Composable
fun RavynBottomNavigation(modifier: Modifier = Modifier) {
    val navigation = LocalRavynNavigation.current
    val selected by navigation.destination
    Row(
        modifier = modifier.fillMaxWidth().height(RavynBottomNavigationHeight)
            .background(RavynStyle.panel),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RavynNavigationDestination("Home", R.drawable.home, selected == 0,
            Modifier.weight(1f)) { navigation.navigate(0) }
        RavynNavigationDestination("Search", R.drawable.search, false,
            Modifier.weight(1f)) { navigation.navigate(1) }
        RavynNavigationDestination("Your Library", R.drawable.library, selected == 2,
            Modifier.weight(1f)) { navigation.navigate(2) }
    }
}

@Composable
private fun RavynNavigationDestination(
    label: String, icon: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) ravynAccent() else RavynStyle.secondary,
        animationSpec = tween(240), label = "Ravyn navigation tint"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick)) {
        Image(painterResource(icon), contentDescription = label,
            colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        RavynLabel(label, size = 10, color = tint, bold = active)
    }
}
