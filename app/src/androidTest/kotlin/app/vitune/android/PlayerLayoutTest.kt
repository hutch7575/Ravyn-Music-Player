package app.vitune.android

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vitune.android.ui.screens.player.RavynNowPlaying
import app.vitune.android.utils.TrackAvailability
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlayerLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun checkLayout(width: Int, height: Int, name: String) {
        val liked = mutableStateOf<Long?>(null)
        val lyrics = mutableStateOf(false)
        compose.setContent {
            Box(Modifier.requiredSize(width.dp, height.dp)) {
                RavynNowPlaying(media = null, artworkAccent = Color(0xFF8877AA), topInset = 0.dp,
                    binder = null, title = "A long song title that should stay inside its available space",
                    artist = "Example artist", likedAt = liked.value, setLikedAt = { liked.value = it },
                    shouldBePlaying = true, isBuffering = true, availability = TrackAvailability.Partial,
                    position = 0, onCollapse = {}, onMore = {}, isShowingLyrics = lyrics.value,
                    onToggleLyrics = { lyrics.value = !lyrics.value }, onShowQueue = {},
                    albumTitle = "Example album", onArtistClick = {},
                    thumbnailContent = { Box(it.background(Color.DarkGray)) })
            }
        }
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("More options").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next").assertIsDisplayed()
        compose.onNodeWithContentDescription("Shuffle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repeat track").assertIsDisplayed()
        compose.onNodeWithText("UP NEXT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Like song").performClick()
        compose.onNodeWithContentDescription("Unlike song").assertIsDisplayed()
        screenshot(name)
    }
    @Test fun portrait() = checkLayout(360, 720, "player-portrait")
    @Test fun smallLandscape() = checkLayout(640, 320, "player-landscape-small")
    @Test fun wideLandscape() = checkLayout(800, 360, "player-landscape-wide")

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        try {
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}
