@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package app.vitune.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.service.LOCAL_KEY_PREFIX
import app.vitune.android.service.PlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Full availability requires a known EOF and a contiguous byte range from zero. */
enum class TrackAvailability {
    NotCached, Partial, Offline, Local
}

fun Cache.trackAvailability(mediaId: String): TrackAvailability {
    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) return TrackAvailability.Local
    val length = ContentMetadata.getContentLength(getContentMetadata(mediaId))
    return when {
        length > 0L && isCached(mediaId, 0L, length) -> TrackAvailability.Offline
        getCachedSpans(mediaId).isNotEmpty() -> TrackAvailability.Partial
        else -> TrackAvailability.NotCached
    }
}

/** Listener callbacks only enqueue invalidations; cache inspection runs off the UI thread. */
@kotlin.OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun Cache.trackAvailabilityFlow(mediaId: String) = callbackFlow {
    val listener = object : Cache.Listener {
        override fun onSpanAdded(cache: Cache, span: CacheSpan) { trySend(Unit) }
        override fun onSpanRemoved(cache: Cache, span: CacheSpan) { trySend(Unit) }
        override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit
    }
    addListener(mediaId, listener)
    trySend(Unit)
    awaitClose { removeListener(mediaId, listener) }
}.conflate().map { trackAvailability(mediaId) }.distinctUntilChanged().flowOn(Dispatchers.IO)

@Composable
fun trackAvailability(
    mediaId: String?,
    binder: PlayerService.Binder? = LocalPlayerServiceBinder.current
): TrackAvailability {
    // Key the state as well as the producer: never show the previous track's offline badge.
    val initial = if (mediaId?.startsWith(LOCAL_KEY_PREFIX) == true)
        TrackAvailability.Local else TrackAvailability.NotCached
    val status by androidx.compose.runtime.key(mediaId, binder) {
        produceState(initial, mediaId, binder) {
            if (mediaId != null && binder != null && initial != TrackAvailability.Local) {
                binder.cache.trackAvailabilityFlow(mediaId).collect { value = it }
            }
        }
    }
    return status
}
