package net.turtton.ytalarm.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist

@Composable
fun PlaylistItemDropdownMenu(
    playlist: Playlist,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            // 名称変更
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_playlist_option_rename)) },
                onClick = {
                    onRename(playlist)
                    onDismiss()
                }
            )
        }
    }
}
