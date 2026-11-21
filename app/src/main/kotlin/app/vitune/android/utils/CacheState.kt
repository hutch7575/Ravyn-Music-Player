package app.vitune.android.utils

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheSpan
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Format
import app.vitune.android.models.Song
import app.vitune.android.service.LOCAL_KEY_PREFIX
import app.vitune.android.service.PlayerService
import app.vitune.android.service.PrecacheService
import app.vitune.android.service.downloadState
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Composable
fun PlaylistDownloadIcon(
    songs: ImmutableList<MediaItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val isDownloading by downloadState.collectAsState()

    AnimatedContent(
        targetState = isDownloading,
        label = "",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { currentIsDownloading ->
        when {
            currentIsDownloading -> CircularProgressIndicator(modifier = Modifier.size(18.dp))

            !songs.map { it.mediaId }.fastAll {
                isCached(
                    mediaId = it,
                    key = isDownloading
                )
            } -> HeaderIconButton(
                icon = R.drawable.download,
                color = colorPalette.text,
                onClick = {
                    songs.forEach {
                        PrecacheService.scheduleCache(context.applicationContext, it)
                    }
                },
                modifier = modifier
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun isCached(
    mediaId: String,
    key: Any? = Unit,
    binder: PlayerService.Binder? = LocalPlayerServiceBinder.current
): Boolean {
    // Keep the compatibility API for menus and download actions, backed by the
    // same span listener used by the player and song rows. `key` is retained for callers.
    val status = trackAvailability(mediaId, binder)
    return status == TrackAvailability.Offline || status == TrackAvailability.Local
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
fun PlayerService.Binder.cachedSongs(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<Song>> =
    Database.songsWithContentLength(sortBy, sortOrder).flatMapLatest { candidates ->
        callbackFlow {
            val listener = object : Cache.Listener {
                override fun onSpanAdded(cache: Cache, span: CacheSpan) { trySend(Unit) }
                override fun onSpanRemoved(cache: Cache, span: CacheSpan) { trySend(Unit) }
                override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit
            }
            candidates.forEach { cache.addListener(it.song.id, listener) }
            trySend(Unit)
            awaitClose { candidates.forEach { cache.removeListener(it.song.id, listener) } }
        }.conflate().map { candidates.filter(::isCached).map { it.song } }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

@OptIn(UnstableApi::class)
class ConditionalCacheDataSourceFactory(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val upstreamDataSourceFactory: DataSource.Factory,
    private val shouldCache: (DataSpec) -> Boolean
) : DataSource.Factory {
    init {
        cacheDataSourceFactory.setUpstreamDataSourceFactory(upstreamDataSourceFactory)
    }

    override fun createDataSource() = object : DataSource {
        private var source: DataSource? = null
        private val transferListeners = mutableListOf<TransferListener>()

        private fun createSource(factory: DataSource.Factory) =
            factory.createDataSource().apply {
                transferListeners.forEach(::addTransferListener)
            }

        override fun read(buffer: ByteArray, offset: Int, length: Int) =
            checkNotNull(source) { "Data source is not open" }.read(buffer, offset, length)

        override fun addTransferListener(transferListener: TransferListener) {
            source?.addTransferListener(transferListener)
            transferListeners += transferListener
        }

        override fun open(dataSpec: DataSpec): Long {
            val factory = if (shouldCache(dataSpec)) cacheDataSourceFactory else upstreamDataSourceFactory
            val selected = createSource(factory)
            source = selected
            return try {
                selected.open(dataSpec)
            } catch (error: ReadOnlyException) {
                // A failed cache open may hold an upstream connection and a hole
                // span. Close it before replacing it, including when writes were paused.
                selected.close()
                createSource(upstreamDataSourceFactory).also { source = it }.open(dataSpec)
            }
        }

        override fun getUri() = source?.uri
        override fun getResponseHeaders() = source?.responseHeaders.orEmpty()
        override fun close() {
            val opened = source
            source = null
            opened?.close()
        }
    }
}
