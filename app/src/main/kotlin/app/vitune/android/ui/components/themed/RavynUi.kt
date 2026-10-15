package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.core.ui.LocalAppearance

/** Album-aware Material surfaces shared by Ravyn's home, library and detail screens. */
@Composable
fun RavynSectionTitle(text: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val palette = LocalAppearance.current.colorPalette
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        BasicText(text, style = TextStyle(
            color = palette.text, fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp
        ))
        if (subtitle != null) BasicText(subtitle, style = TextStyle(
            color = palette.textSecondary, fontSize = 13.sp
        ))
    }
}

@Composable
fun RavynSortChip(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAppearance.current.colorPalette
    val fill by animateColorAsState(
        if (selected) palette.accent.copy(alpha = 0.19f) else palette.background1,
        animationSpec = tween(240, easing = FastOutSlowInEasing), label = "Chip fill"
    )
    val outline by animateColorAsState(
        if (selected) palette.accent else palette.textSecondary.copy(alpha = 0.23f),
        animationSpec = tween(240), label = "Chip outline"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(1.dp, outline, RoundedCornerShape(22.dp))
            .background(fill, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        BasicText(title, style = TextStyle(
            color = if (selected) palette.accent else palette.textSecondary,
            fontWeight = FontWeight.Medium, fontSize = 12.sp
        ))
    }
}

@Composable
fun RavynIconAction(
    @DrawableRes icon: Int,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAppearance.current.colorPalette
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .background(palette.background1, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Image(painterResource(icon), description,
            colorFilter = ColorFilter.tint(palette.text), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun RavynGreetingHeader(modifier: Modifier = Modifier) {
    val palette = LocalAppearance.current.colorPalette
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val highlighted by animateColorAsState(
        palette.accent.copy(alpha = 0.28f), animationSpec = tween(400),
        label = "Greeting artwork tint"
    )
    Box(
        modifier = modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(palette.background1, highlighted, palette.background1)),
                RoundedCornerShape(13.dp)
            )
            .border(1.dp, palette.text.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            BasicText(greeting, style = TextStyle(
                color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold
            ))
            BasicText("Your music, your way", style = TextStyle(
                color = palette.textSecondary, fontSize = 13.sp
            ))
        }
    }
}
