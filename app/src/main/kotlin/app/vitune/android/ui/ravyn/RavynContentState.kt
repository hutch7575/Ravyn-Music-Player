package app.vitune.android.ui.ravyn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The same loading, empty and retry treatment in album, artist and playlist routes. */
@Composable
fun RavynContentState(
    loading: Boolean, failed: Boolean, emptyMessage: String,
    onRetry: (() -> Unit)? = null
) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), color = ravynAccent(), strokeWidth = 2.dp)
            RavynLabel("Loading music…", 13, RavynStyle.secondary)
        } else {
            RavynLabel(if (failed) "Couldn't load this music. Check your connection." else emptyMessage,
                13, RavynStyle.secondary, lines = 3)
            if (failed && onRetry != null) RavynPill("Retry", false, onRetry)
        }
    }
}
