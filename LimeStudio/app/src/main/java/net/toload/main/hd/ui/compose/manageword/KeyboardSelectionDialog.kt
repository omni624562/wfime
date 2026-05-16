/*
 *
 *  **    Copyright 2024, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 */

package net.toload.main.hd.ui.compose.manageword

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.toload.main.hd.R
import net.toload.main.hd.data.Keyboard

/**
 * Dialog for selecting keyboard layout.
 *
 * Displays a list of available keyboard layouts for the user to choose from.
 * When a keyboard is selected, calls the onKeyboardSelected callback and dismisses.
 *
 * @param keyboards List of available keyboard layouts
 * @param onKeyboardSelected Callback when a keyboard is selected
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun KeyboardSelectionDialog(
    keyboards: List<Keyboard>,
    onKeyboardSelected: (Keyboard) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.manage_select_keyboard),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                keyboards.forEachIndexed { index, keyboard ->
                    KeyboardItem(
                        keyboard = keyboard,
                        onClick = {
                            onKeyboardSelected(keyboard)
                        }
                    )

                    // Add divider between items (but not after last item)
                    if (index < keyboards.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Individual keyboard item in the selection list.
 *
 * @param keyboard Keyboard layout data
 * @param onClick Callback when item is clicked
 */
@Composable
private fun KeyboardItem(
    keyboard: Keyboard,
    onClick: () -> Unit
) {
    Text(
        text = keyboard.desc,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    )
}
