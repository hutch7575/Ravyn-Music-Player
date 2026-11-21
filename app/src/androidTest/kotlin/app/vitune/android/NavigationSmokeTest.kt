package app.vitune.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun destinationsAndSettingsRemainUsable() {
        // Monet initialises asynchronously before the application sets its content.
        compose.waitUntil(20000L) { compose.onAllNodesWithText("Your Library").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Your Library").onLast().performClick()
        compose.onNodeWithText("Nothing here yet").assertIsDisplayed()
        compose.onNodeWithText("Playlists", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No playlists yet").assertIsDisplayed()
        compose.onAllNodesWithText("Search").onLast().performClick()
        compose.onNodeWithContentDescription("Clear search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").performClick()
        listOf("Appearance", "Player", "Cache", "Data", "Other", "About").forEach { tab ->
            compose.onAllNodesWithText(tab, substring = false).onFirst().performScrollTo().performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onAllNodesWithText("Home").onLast().assertIsDisplayed()
    }
}
