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

enum class RepeatTypeSelection {
    ONCE,
    EVERYDAY,
    DAYS,
    DATE
}

@Composable
fun RepeatTypeDialog(
    currentRepeatType: RepeatTypeSelection,
    onTypeSelected: (RepeatTypeSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val repeatOptions = stringArrayResource(R.array.dialog_repeat_type_choice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_repeat_choice_title)) },
        text = {
            Column {
                RepeatTypeSelection.entries.forEachIndexed { index, type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTypeSelected(type)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentRepeatType == type,
                            onClick = { onTypeSelected(type) }
                        )
                        Text(
                            text = repeatOptions[index],
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun RepeatTypeDialogPreview() {
    AppTheme {
        RepeatTypeDialog(
            currentRepeatType = RepeatTypeSelection.EVERYDAY,
            onTypeSelected = {},
            onDismiss = {}
        )
    }
}