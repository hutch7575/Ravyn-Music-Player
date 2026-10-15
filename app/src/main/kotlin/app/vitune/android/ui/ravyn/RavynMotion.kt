package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stable home sections prevent asynchronous Database/Innertube updates from
 * instantly inserting new LazyColumn items and moving the page around.
 * Keep a lightweight skeleton in place until the first source result arrives.
 */
@Composable
internal fun RavynReveal(
    ready: Boolean,
    title: String,
    style: RavynPlaceholderStyle,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = ready,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(350, delayMillis = 65,
                    easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 14 },
                        animationSpec = tween(380, delayMillis = 65,
                            easing = FastOutSlowInEasing)
                    ),
                initialContentExit = fadeOut(tween(170)),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        label = "Reveal $title"
    ) { loaded ->
        if (loaded) content() else RavynLoadingSection(title, style)
    }
}

internal enum class RavynPlaceholderStyle {
    Artists, Recent, Albums, Tracks
}

@Composable
private fun RavynLoadingSection(title: String, style: RavynPlaceholderStyle) {
    val pulse = rememberInfiniteTransition(label = "Artwork loading")
    val alpha = pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            tween(1050, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "Skeleton pulse"
    ).value
    val fill = RavynStyle.raised.copy(alpha = alpha)
    Column(
        verticalArrangement = Arrangement.spacedBy(11.dp),
        modifier = Modifier.padding(start = 17.dp)
    ) {
        RavynLabel(title, size = 16, bold = true)
        when (style) {
            RavynPlaceholderStyle.Artists -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(5) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(61.dp).background(fill, CircleShape))
                            Box(Modifier.width(55.dp).height(9.dp)
                                .background(fill, RoundedCornerShape(5.dp)))
                        }
                    }
                }
            }
            RavynPlaceholderStyle.Recent -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.width(165.dp).height(57.dp)
                                .background(RavynStyle.panel, RoundedCornerShape(8.dp))
                                .padding(5.dp)) {
                            Box(Modifier.size(47.dp)
                                .background(fill, RoundedCornerShape(6.dp)))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.width(77.dp).height(9.dp)
                                    .background(fill, RoundedCornerShape(4.dp)))
                                Box(Modifier.width(56.dp).height(7.dp)
                                    .background(fill, RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }
            RavynPlaceholderStyle.Albums -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(125.dp)
                                .background(fill, RoundedCornerShape(8.dp)))
                            Box(Modifier.width(96.dp).height(9.dp)
                                .background(fill, RoundedCornerShape(4.dp)))
                            Box(Modifier.width(72.dp).height(7.dp)
                                .background(fill, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
            RavynPlaceholderStyle.Tracks -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp)
                                .background(fill, RoundedCornerShape(7.dp)))
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.width(135.dp).height(10.dp)
                                    .background(fill, RoundedCornerShape(5.dp)))
                                Box(Modifier.width(95.dp).height(8.dp)
                                    .background(fill, RoundedCornerShape(5.dp)))
                            }
                        }
                    }
                }
            }
        }
    }
}
