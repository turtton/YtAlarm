package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.turtton.ytalarm.R

@Composable
fun RemoveOrDeleteVideoDialog(
    videoTitle: String,
    onRemoveFromPlaylist: () -> Unit,
    onDeleteVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_remove_or_delete_video_title)) },
        text = {
            Text(stringResource(R.string.dialog_remove_or_delete_video_message, videoTitle))
        },
        confirmButton = {
            TextButton(onClick = onDeleteVideo) {
                Text(stringResource(R.string.dialog_remove_or_delete_video_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onRemoveFromPlaylist) {
                Text(stringResource(R.string.dialog_remove_or_delete_video_remove))
            }
        }
    )
}