/*
 *
 *  **    Copyright 2015, The LimeIME Open Source Project
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

package nan.toload.main.hd.ui.compose.manageword

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nan.toload.main.hd.R
import nan.toload.main.hd.data.Word

/**
 * Dialog mode for word operations.
 */
enum class WordDialogMode {
    ADD,    // Add new word
    EDIT    // Edit existing word
}

/**
 * Composable dialog for adding or editing a word.
 *
 * Features:
 * - Input fields for code, word, and score
 * - Score increment/decrement buttons
 * - Confirmation dialog before save/update
 * - Delete button with confirmation (edit mode only)
 * - Form validation
 *
 * @param mode Dialog mode (ADD or EDIT)
 * @param word Existing word data (for EDIT mode)
 * @param onDismiss Callback when dialog is dismissed
 * @param onSave Callback when save is confirmed (code, word, score)
 * @param onDelete Callback when delete is confirmed (edit mode only)
 */
@Composable
fun WordDialog(
    mode: WordDialogMode,
    word: Word? = null,
    onDismiss: () -> Unit,
    onSave: (code: String, word: String, score: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var code by remember { mutableStateOf(word?.code ?: "") }
    var wordText by remember { mutableStateOf(word?.word ?: "") }
    var score by remember { mutableIntStateOf(word?.score ?: 1) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmMessage by remember { mutableStateOf("") }

    // Load all string resources at the top of composable
    val addTitle = stringResource(R.string.manage_word_dialog_add)
    val editTitle = stringResource(R.string.manage_word_dialog_edit)
    val deleteTitle = stringResource(R.string.manage_word_dialog_delete)
    val addMessage = stringResource(R.string.manage_word_dialog_add_message)
    val editMessage = stringResource(R.string.manage_word_dialog_message)
    val deleteMessage = stringResource(R.string.manage_word_dialog_delete_message)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    WordDialogMode.ADD -> stringResource(R.string.manage_word_dialog_add)
                    WordDialogMode.EDIT -> stringResource(R.string.manage_word_dialog_edit)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Code input
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.manage_word_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Word input
                OutlinedTextField(
                    value = wordText,
                    onValueChange = { wordText = it },
                    label = { Text(stringResource(R.string.manage_word_word)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Score with increment/decrement
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.manage_word_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrement button
                        FilledTonalIconButton(
                            onClick = { if (score > 0) score-- },
                            enabled = score > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Decrease score"
                            )
                        }

                        // Score display
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        // Increment button
                        FilledTonalIconButton(
                            onClick = { score++ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase score"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete button (edit mode only)
                if (mode == WordDialogMode.EDIT && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            confirmTitle = deleteTitle
                            confirmMessage = deleteMessage
                            confirmAction = {
                                onDelete()
                                onDismiss()
                            }
                            showConfirmDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.manage_im_remove))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Cancel button
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.manage_im_cancel))
                }

                // Save/Update button
                Button(
                    onClick = {
                        if (code.isNotBlank() && wordText.isNotBlank()) {
                            confirmTitle = when (mode) {
                                WordDialogMode.ADD -> addTitle
                                WordDialogMode.EDIT -> editTitle
                            }
                            confirmMessage = when (mode) {
                                WordDialogMode.ADD -> addMessage
                                WordDialogMode.EDIT -> editMessage
                            }
                            confirmAction = {
                                onSave(code.trim(), wordText.trim(), score)
                                onDismiss()
                            }
                            showConfirmDialog = true
                        }
                    },
                    enabled = code.isNotBlank() && wordText.isNotBlank()
                ) {
                    Text(
                        when (mode) {
                            WordDialogMode.ADD -> stringResource(R.string.manage_im_save)
                            WordDialogMode.EDIT -> stringResource(R.string.manage_im_update)
                        }
                    )
                }
            }
        }
    )

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmTitle) },
            text = { Text(confirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction()
                        showConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}