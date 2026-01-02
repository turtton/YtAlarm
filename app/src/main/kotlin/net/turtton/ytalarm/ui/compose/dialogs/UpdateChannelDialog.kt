@file:Suppress("MatchingDeclarationName")

package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

enum class UpdateChannelSelection {
    STABLE,
    NIGHTLY
}

@Composable
fun UpdateChannelDialog(
    currentChannel: UpdateChannelSelection,
    onChannelSelected: (UpdateChannelSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val channelOptions = stringArrayResource(R.array.dialog_update_channel_choice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_update_channel_title)) },
        text = {
            Column {
                UpdateChannelSelection.entries.forEachIndexed { index, channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChannelSelected(channel)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentChannel == channel,
                            onClick = null // Rowのクリックハンドラに統一
                        )
                        Text(
                            text = channelOptions[index],
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Preview
@Composable
fun UpdateChannelDialogPreview() {
    AppTheme {
        UpdateChannelDialog(
            currentChannel = UpdateChannelSelection.STABLE,
            onChannelSelected = {},
            onDismiss = {}
        )
    }
}