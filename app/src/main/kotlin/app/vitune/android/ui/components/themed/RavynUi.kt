package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.core.ui.LocalAppearance

/** Reusable, low-overhead 2016 Material styling for library and details screens. */
@Composable
fun RavynSectionTitle(
    text: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppearance.current.colorPalette
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = palette.text
            )
        )
        if (subtitle != null) BasicText(
            text = subtitle,
            style = TextStyle(color = palette.textSecondary, fontSize = 13.sp)
        )
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (selected) Color(0xFF7556C2) else palette.background2,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicText(
            title.uppercase(),
            style = TextStyle(
                color = if (selected) Color.White else palette.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.55.sp
            )
        )
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
            .size(39.dp)
            .background(palette.background2, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(palette.text),
            modifier = Modifier.size(19.dp)
        )
    }
}
