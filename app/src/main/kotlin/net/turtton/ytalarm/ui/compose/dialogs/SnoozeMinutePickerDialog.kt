package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun SnoozeMinutePickerDialog(
    initialMinute: Int,
    onConfirm: (minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinute by remember { mutableStateOf(initialMinute.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_snooze)) },
        text = {
            Column {
                val minuteInt = selectedMinute.roundToInt()
                Text(
                    text = pluralStringResource(
                        R.plurals.setting_snooze_time,
                        minuteInt,
                        minuteInt
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    valueRange = 10f..60f,
                    steps = 4, // 10, 20, 30, 40, 50, 60
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedMinute.roundToInt())
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun SnoozeMinutePickerDialogPreview() {
    AppTheme {
        SnoozeMinutePickerDialog(
            initialMinute = 15,
            onConfirm = {},
            onDismiss = {}
        )
    }
}