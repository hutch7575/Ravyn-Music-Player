package app.vitune.android.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.isLandscape
import app.vitune.core.ui.utils.px
import coil3.compose.AsyncImage
import com.valentinilk.shimmer.shimmer

@Composable
fun LayoutWithAdaptiveThumbnail(
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val content = remember { movableContentOf(content) }
    val thumbnailContent = remember { movableContentOf(thumbnailContent) }

    if (isLandscape) Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        thumbnailContent()
        content()
    } else Box(modifier = modifier) { content() }
}

fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape? = null
): @Composable () -> Unit = {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        val [colorPalette, _, _, thumbnailShape] = LocalAppearance.current
        val thumbnailSize =
            if (isLandscape) (maxHeight - 96.dp - Dimensions.items.collapsedPlayerHeight).coerceAtLeast(80.dp)
            else minOf(maxWidth, 340.dp)

        val innerModifier = Modifier
            .clip(shape ?: thumbnailShape)
            .size(thumbnailSize)

        var artworkLoading by remember(url) { mutableStateOf(true) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = innerModifier.background(colorPalette.background2)
        ) {
            if (isLoading || (url != null && artworkLoading)) CircularProgressIndicator(
                color = colorPalette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            if (!isLoading && url != null) AsyncImage(
                model = url.thumbnail(thumbnailSize.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onLoading = { artworkLoading = true },
                onSuccess = { artworkLoading = false },
                onError = { artworkLoading = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
