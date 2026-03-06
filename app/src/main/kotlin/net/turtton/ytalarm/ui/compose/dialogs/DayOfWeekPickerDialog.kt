@file:Suppress("NewApi")

package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.getDisplay

@Composable
fun DayOfWeekPickerDialog(
    initialSelectedDays: List<DayOfWeek>,
    onConfirm: (List<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedDays = remember(initialSelectedDays) {
        val map = mutableStateMapOf<DayOfWeek, Boolean>()
        DayOfWeek.entries.forEach { day ->
            map[day] = initialSelectedDays.contains(day)
        }
        map
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_repeat_days_title)) },
        text = {
            Column {
                DayOfWeek.entries.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDays[day] = !(selectedDays[day] ?: false)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedDays[day] ?: false,
                            onCheckedChange = { checked ->
                                selectedDays[day] = checked
                            }
                        )
                        Text(
                            text = day.getDisplay(context)?.toString() ?: day.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasSelection = selectedDays.values.any { it }
            TextButton(
                onClick = {
                    val selected = DayOfWeek.entries.filter { selectedDays[it] == true }
                    if (selected.isNotEmpty()) {
                        onConfirm(selected)
                    }
                },
                enabled = hasSelection
            ) {
                Text(stringResource(R.string.dialog_repeat_days_ok))
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
fun DayOfWeekPickerDialogPreview() {
    AppTheme {
        DayOfWeekPickerDialog(
            initialSelectedDays = listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}