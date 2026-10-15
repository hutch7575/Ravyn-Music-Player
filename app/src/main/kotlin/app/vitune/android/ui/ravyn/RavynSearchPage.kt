package app.vitune.android.ui.ravyn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.SearchQuery
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.SearchSuggestionsBody
import app.vitune.providers.innertube.requests.searchSuggestions
import kotlinx.coroutines.delay

/** Brand-new search presentation; suggestions, history and local matching use ViTune's data layer. */
@Composable
fun RavynSearchPage(
    initialQuery: String,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
    onPlaylistUrl: (String) -> Unit,
    embedded: Boolean = false
) {
    val binder = LocalPlayerServiceBinder.current
    val accent = ravynAccent()
    var query by rememberSaveable(initialQuery, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialQuery, TextRange(initialQuery.length)))
    }
    var source by rememberSaveable { mutableStateOf(0) }
    var history by remember { mutableStateOf<List<SearchQuery>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var locals by remember { mutableStateOf<List<Song>>(emptyList()) }

    fun submit(value: String) {
        val text = value.trim()
        if (text.isNotEmpty()) {
            if (text.startsWith("https://") && text.contains("list=")) onPlaylistUrl(text)
            else onSubmit(text)
        }
    }

    LaunchedEffect(query.text) {
        if (!DataPreferences.pauseSearchHistory) {
            Database.queries("%" + query.text + "%").collect { history = it }
        } else history = emptyList()
    }
    LaunchedEffect(query.text) {
        suggestions = emptyList()
        if (query.text.length >= 2) {
            delay(330)
            suggestions = runCatching {
                Innertube.searchSuggestions(SearchSuggestionsBody(input = query.text))
                    ?.getOrNull().orEmpty()
            }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(query.text) {
        if (query.text.length < 2) locals = emptyList()
        else Database.search("%" + query.text + "%").collect { locals = it }
    }

    val insets = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical).asPaddingValues()
    Column(
        Modifier.fillMaxSize().background(RavynStyle.background)
            .padding(
                top = if (embedded) 0.dp else insets.calculateTopPadding(),
                bottom = if (embedded) 0.dp else insets.calculateBottomPadding()
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 18.dp, top = 9.dp)) {
            if (!embedded) RavynIcon(R.drawable.chevron_back, "Back", onBack)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = RavynStyle.text, fontSize = 16.sp),
                cursorBrush = SolidColor(accent), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(query.text) }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.text.isEmpty()) RavynLabel(
                            "Search songs, albums, artists...", 14, RavynStyle.muted)
                        field()
                    }
                },
                modifier = Modifier.weight(1f)
                    .background(RavynStyle.panel, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            )
            RavynIcon(R.drawable.close, "Clear search", { query = TextFieldValue("") },
                tint = RavynStyle.secondary)
        }
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 18.dp)) {
            RavynPill("Online", source == 0) { source = 0 }
            RavynPill("Your Library", source == 1) { source = 1 }
        }
        Spacer(Modifier.height(19.dp))
        AnimatedContent(targetState = source,
            transitionSpec = { ContentTransform(fadeIn(tween(190)),
                fadeOut(tween(90)), sizeTransform = null) },
            label = "Search sources", modifier = Modifier.weight(1f)) { active ->
            LazyColumn(contentPadding = PaddingValues(
                start = 18.dp, end = 18.dp, bottom = 24.dp)) {
                if (active == 0) {
                    if (query.text.isBlank() && history.isNotEmpty()) {
                        item {
                            RavynSection("Recent searches")
                            Spacer(Modifier.height(10.dp))
                        }
                        items(history.take(12), key = { it.id }) { result ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .clickable { submit(result.query) }) {
                                RavynLabel(result.query, 14, modifier = Modifier.weight(1f))
                                RavynLabel("↗", 15, RavynStyle.secondary)
                            }
                        }
                    }
                    if (query.text.isNotBlank()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(55.dp)
                                    .clickable { submit(query.text) }) {
                                RavynLabel("Search for \"" + query.text + "\"", 15,
                                    accent, bold = true)
                            }
                        }
                        items(suggestions.distinct().take(12), key = { it }) { suggestion ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(51.dp)
                                    .clickable { submit(suggestion) }) {
                                RavynLabel(suggestion, 14, modifier = Modifier.weight(1f))
                                RavynLabel("↗", 15, RavynStyle.secondary)
                            }
                        }
                    }
                } else {
                    if (query.text.length < 2) item {
                        RavynLabel("Type at least two letters to search your music.",
                            13, RavynStyle.secondary, lines = 2)
                    }
                    if (query.text.length >= 2 && locals.isEmpty()) item {
                        RavynLabel("No local matches yet.", 13, RavynStyle.secondary)
                    }
                    items(locals, key = { it.id }) { song ->
                        RavynTrackRow(song, onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlay(song.asMediaItem)
                        }, showHeart = false)
                    }
                }
            }
        }
    }
}
