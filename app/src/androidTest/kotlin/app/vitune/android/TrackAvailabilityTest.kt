@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package app.vitune.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.ContentMetadataMutations
import app.vitune.android.utils.TrackAvailability
import app.vitune.android.utils.trackAvailability
import app.vitune.android.utils.trackAvailabilityFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TrackAvailabilityTest {
    private lateinit var cache: SimpleCache
    private lateinit var directory: File
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        directory = File(context.cacheDir, "test-cache-${UUID.randomUUID()}")
        cache = SimpleCache(directory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
    }
    @After fun cleanup() { cache.release(); directory.deleteRecursively() }

    private fun write(position: Long, length: Int) {
        val hole = cache.startReadWrite("track", position, length.toLong())
        try {
            val file = cache.startFile("track", position, length.toLong())
            file.writeBytes(ByteArray(length))
            cache.commitFile(file, length.toLong())
        } finally { cache.releaseHoleSpan(hole) }
    }
    private fun size(length: Long) {
        cache.applyContentMetadataMutations("track", ContentMetadataMutations().apply {
            ContentMetadataMutations.setContentLength(this, length)
        })
    }
    @Test fun unknownLengthAndHolesAreNeverOffline() {
        write(0, 4)
        assertEquals(TrackAvailability.Partial, cache.trackAvailability("track"))
        size(12)
        write(8, 4)
        assertEquals(TrackAvailability.Partial, cache.trackAvailability("track"))
        write(4, 4)
        assertEquals(TrackAvailability.Offline, cache.trackAvailability("track"))
        assertEquals(TrackAvailability.Local, cache.trackAvailability("local:42"))
    }
    @Test fun additionsAndClearingUpdateTheObserver() = runBlocking {
        withTimeout(10000L) {
            val updates = cache.trackAvailabilityFlow("track").produceIn(this)
            try {
                assertEquals(TrackAvailability.NotCached, updates.receive())
                size(8)
                write(0, 4)
                assertEquals(TrackAvailability.Partial, updates.receive())
                write(4, 4)
                assertEquals(TrackAvailability.Offline, updates.receive())
                cache.removeResource("track")
                assertEquals(TrackAvailability.NotCached, updates.receive())
            } finally { updates.cancel() }
        }
    }
}
