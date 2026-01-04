package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Video

@Composable
fun DeleteVideoDialog(video: Video, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_video_title)) },
        text = { Text(stringResource(R.string.dialog_delete_video_message, video.title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_remove_video_positive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_remove_video_negative))
            }
        }
    )
}