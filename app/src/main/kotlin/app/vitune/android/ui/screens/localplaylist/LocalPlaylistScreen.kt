package app.vitune.android.ui.screens.localplaylist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.models.Playlist
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.ravyn.RavynCollectionDetail
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forcePlayFromBeginning
import app.vitune.compose.routing.RouteHandler
import kotlinx.coroutines.launch

@Route
@Composable
fun LocalPlaylistScreen(playlistId: Long) {
    var playlist by remember(playlistId) { mutableStateOf<Playlist?>(null) }
    var songs by remember(playlistId) { mutableStateOf<List<Song>>(emptyList()) }
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val binder = LocalPlayerServiceBinder.current

    LaunchedEffect(playlistId) {
        Database.playlist(playlistId).collect { playlist = it }
    }
    LaunchedEffect(playlistId) {
        Database.playlistSongs(playlistId).collect { songs = it }
    }
    LaunchedEffect(playlist?.browseId) {
        if (DataPreferences.autoSyncPlaylists) {
            val item = playlist
            val browseId = item?.browseId
            if (item != null && browseId != null) {
                syncing = true
                try { sync(item, browseId) } finally { syncing = false }
            }
        }
    }

    RouteHandler {
        GlobalRoutes()
        Content {
            val current = playlist
            val accent = app.vitune.android.ui.ravyn.ravynAccent()
            if (menu) AlertDialog(
                onDismissRequest = { menu = false },
                title = { Text(current?.name ?: "Playlist") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        TextButton(onClick = {
                            binder?.player?.enqueue(songs.map { it.asMediaItem })
                            menu = false
                        }) { Text("Add all to queue") }
                        if (current?.browseId != null) TextButton(onClick = {
                            menu = false
                            scope.launch {
                                syncing = true
                                try { sync(current, current.browseId) } finally { syncing = false }
                            }
                        }) { Text(if (syncing) "Syncing..." else "Sync playlist") }
                        TextButton(onClick = {
                            editingName = current?.name.orEmpty()
                            menu = false
                            rename = true
                        }) { Text("Rename") }
                        TextButton(onClick = { menu = false; delete = true }) {
                            Text("Delete playlist")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { menu = false }) {
                    Text("Close")
                } }
            )
            if (rename) AlertDialog(
                onDismissRequest = { rename = false },
                title = { Text("Rename playlist") },
                text = { TextField(editingName, onValueChange = { editingName = it },
                    singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    if (editingName.isNotBlank() && current != null) query {
                        Database.update(current.copy(name = editingName.trim()))
                    }
                    rename = false
                }) { Text("Save", color = accent) } },
                dismissButton = { TextButton(onClick = { rename = false }) {
                    Text("Cancel")
                } }
            )
            if (delete) AlertDialog(
                onDismissRequest = { delete = false },
                title = { Text("Delete playlist?") },
                text = { Text("This removes the playlist, not the original songs.") },
                confirmButton = { TextButton(onClick = {
                    if (current != null) query { Database.delete(current) }
                    delete = false
                    pop()
                }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { delete = false }) {
                    Text("Cancel")
                } }
            )

            RavynCollectionDetail(
                title = current?.name ?: "Playlist",
                artist = "Your playlist",
                artwork = current?.thumbnail ?: songs.firstOrNull()?.thumbnailUrl,
                tracks = songs,
                onBack = pop,
                onPlay = { index ->
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(
                        songs.map { it.asMediaItem }, index
                    )
                },
                onShuffle = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(
                        songs.shuffled().map { it.asMediaItem }
                    )
                },
                onLike = { song ->
                    query { Database.like(song.id,
                        if (song.likedAt == null) System.currentTimeMillis() else null) }
                },
                onMenu = { menu = true }
            )
        }
    }
}
