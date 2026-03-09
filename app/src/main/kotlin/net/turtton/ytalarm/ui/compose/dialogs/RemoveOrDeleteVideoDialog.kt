package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_remove_video_negative))
                }
                Column {
                    TextButton(onClick = onDeleteVideo) {
                        Text(
                            stringResource(R.string.dialog_remove_or_delete_video_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onRemoveFromPlaylist) {
                        Text(stringResource(R.string.dialog_remove_or_delete_video_remove))
                    }
                }
            }
        }
    )
}