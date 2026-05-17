/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package net.toload.main.hd.ui.compose.managerelated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.toload.main.hd.R
import net.toload.main.hd.data.Related
import net.toload.main.hd.ui.compose.manageword.PaginationControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRelatedScreen(viewModel: ManageRelatedViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_related_management)) },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.manage_related_add)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RelatedSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch() },
                onReset = { viewModel.resetSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.items) { item ->
                        RelatedItemCard(
                            item = item,
                            onClick = { viewModel.showEditDialog(item) }
                        )
                    }
                }
            }

            PaginationControls(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                totalRecords = uiState.totalRecords,
                pageSize = uiState.pageSize,
                onPreviousClick = { viewModel.previousPage() },
                onNextClick = { viewModel.nextPage() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (uiState.showAddDialog) {
        RelatedAddDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { pword, cword, score -> viewModel.addRelated(pword, cword, score) },
            onErrorDismissed = { viewModel.clearError() }
        )
    }

    uiState.editingItem?.let { item ->
        RelatedEditDialog(
            item = item,
            onDismiss = { viewModel.hideEditDialog() },
            onUpdate = { pword, cword, score ->
                viewModel.updateRelated(item.id, pword, cword, score)
            },
            onRemove = { viewModel.removeRelated(item.id) }
        )
    }
}

@Composable
private fun RelatedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searched by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                searched = false
            },
            label = { Text(stringResource(R.string.manage_related_search)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = {
            if (searched || query.isBlank()) {
                onReset()
                searched = false
            } else {
                onSearch()
                searched = true
            }
        }) {
            Text(
                if (searched) stringResource(R.string.manage_related_reset)
                else stringResource(R.string.manage_related_search)
            )
        }
    }
}

@Composable
private fun RelatedItemCard(item: Related, onClick: () -> Unit) {
    val word = (item.pword ?: "") + (item.cword ?: "")
    val display = if (word.length > 12) word.substring(0, 10) + "…" else word

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.basescore.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelatedAddDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (pword: String, cword: String, score: Int) -> Unit,
    onErrorDismissed: () -> Unit
) {
    var wordInput by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(1) }
    var inputError by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_related_dialog_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = {
                        wordInput = it
                        inputError = false
                        onErrorDismissed()
                    },
                    label = {
                        Text(
                            stringResource(R.string.manage_related_pword) +
                                " + " + stringResource(R.string.manage_related_cword)
                        )
                    },
                    isError = inputError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (inputError) {
                    Text(
                        text = stringResource(R.string.insert_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ScoreStepper(
                    label = stringResource(R.string.manage_related_score),
                    score = score,
                    onScoreChange = { score = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (wordInput.trim().length < 2) {
                    inputError = true
                } else {
                    showConfirm = true
                }
            }) {
                Text(stringResource(R.string.manage_related_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.manage_related_cancel))
            }
        }
    )

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.manage_related_dialog_add)) },
            text = { Text(stringResource(R.string.manage_related_dialog_add_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    val trimmed = wordInput.trim()
                    onSave(trimmed.substring(0, 1), trimmed.substring(1), score)
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun RelatedEditDialog(
    item: Related,
    onDismiss: () -> Unit,
    onUpdate: (pword: String, cword: String, score: Int) -> Unit,
    onRemove: () -> Unit
) {
    var wordInput by remember { mutableStateOf((item.pword ?: "") + (item.cword ?: "")) }
    var score by remember { mutableStateOf(item.basescore) }
    var inputError by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_related_dialog_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = {
                        wordInput = it
                        inputError = false
                    },
                    label = {
                        Text(
                            stringResource(R.string.manage_related_pword) +
                                " + " + stringResource(R.string.manage_related_cword)
                        )
                    },
                    isError = inputError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (inputError) {
                    Text(
                        text = stringResource(R.string.update_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ScoreStepper(
                    label = stringResource(R.string.manage_related_score),
                    score = score,
                    onScoreChange = { score = it }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    if (wordInput.trim().length < 2) {
                        inputError = true
                    } else {
                        showUpdateConfirm = true
                    }
                }) { Text(stringResource(R.string.manage_related_update)) }

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.manage_related_remove)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.manage_related_cancel))
            }
        }
    )

    if (showUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            title = { Text(stringResource(R.string.manage_related_dialog_edit)) },
            text = { Text(stringResource(R.string.manage_related_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateConfirm = false
                    val trimmed = wordInput.trim()
                    onUpdate(trimmed.substring(0, 1), trimmed.substring(1), score)
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.manage_related_dialog_delete)) },
            text = { Text(stringResource(R.string.manage_related_dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onRemove()
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun ScoreStepper(
    label: String,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (score > 0) onScoreChange(score - 1) }) {
            Text("-", style = MaterialTheme.typography.titleMedium)
        }
        Text(score.toString(), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onScoreChange(score + 1) }) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
