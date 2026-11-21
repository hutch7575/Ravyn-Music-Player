@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package app.vitune.android

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vitune.android.service.PlayerService
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CachedPlaybackTest {
    @Test fun completeCachedAudioPreparesSeeksAndAdvancesWithoutExtraction() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "playback-test-${UUID.randomUUID()}")
        val cache = SimpleCache(directory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        // One second of PCM silence. An invalid YouTube ID makes accidental
        // extraction fail, proving a complete cached source works independently.
        val audio = ByteBuffer.allocate(16044).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(16036); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1.toShort()); putShort(1.toShort()); putInt(8000)
            putInt(16000); putShort(2.toShort()); putShort(16.toShort())
            put("data".toByteArray()); putInt(16000)
        }.array()
        val hole = cache.startReadWrite("offline-fixture", 0, audio.size.toLong())
        try {
            cache.applyContentMetadataMutations("offline-fixture", ContentMetadataMutations().apply {
                ContentMetadataMutations.setContentLength(this, audio.size.toLong())
            })
            val file = cache.startFile("offline-fixture", 0, audio.size.toLong())
            file.writeBytes(audio)
            cache.commitFile(file, audio.size.toLong())
        } finally { cache.releaseHoleSpan(hole) }
        val ready = CountDownLatch(1)
        val advanced = CountDownLatch(1)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var player: ExoPlayer? = null
        try {
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(
                    PlayerService.createYouTubeDataSourceResolverFactory(context, cache)
                )).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) ready.countDown()
                        }
                        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) advanced.countDown()
                        }
                    })
                    val item = MediaItem.Builder().setMediaId("offline-fixture")
                        .setUri("offline-fixture").setCustomCacheKey("offline-fixture").build()
                    setMediaItems(listOf(item, item)); prepare()
                }
            }
            assertTrue("Cached audio did not prepare", ready.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                assertTrue("Duration was not decoded", player!!.duration in 900L..1100L)
                player!!.pause(); player!!.seekTo(300L); player!!.play()
            }
            assertTrue("Cached queue did not advance", advanced.await(10, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync { player?.release() }
            cache.release(); directory.deleteRecursively()
        }
    }
}
