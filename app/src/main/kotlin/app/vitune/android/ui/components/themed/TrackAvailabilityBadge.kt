package app.vitune.android.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.utils.TrackAvailability
import app.vitune.android.utils.trackAvailability
import app.vitune.core.ui.LocalAppearance

@Composable
fun availabilityLabel(status: TrackAvailability): String = stringResource(when (status) {
    TrackAvailability.Offline -> R.string.available_offline
    TrackAvailability.Partial -> R.string.partially_cached
    TrackAvailability.Local -> R.string.local_audio
    TrackAvailability.NotCached -> R.string.not_cached
})

@Composable
fun TrackAvailabilityBadge(
    mediaId: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalAppearance.current.colorPalette.textSecondary
) {
    val status = trackAvailability(mediaId)
    if (status != TrackAvailability.NotCached) Image(
        painterResource(when (status) {
            TrackAvailability.Local -> R.drawable.musical_notes
            TrackAvailability.Partial -> R.drawable.sync
            else -> R.drawable.download
        }),
        contentDescription = availabilityLabel(status),
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(16.dp)
    )
}
