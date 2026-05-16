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

package net.toload.main.hd.ui.compose.manageword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.toload.main.hd.R
import net.toload.main.hd.data.Word
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Composable screen for managing input method words.
 *
 * Features:
 * - Search functionality with query input
 * - Grid display of words (3 columns)
 * - Pagination controls
 * - Add/Edit/Delete operations (via callbacks)
 * - Loading indicator
 *
 * @param uiState Current UI state containing words, pagination info, etc.
 * @param onSearchQueryChange Callback when search query changes
 * @param onSearchClick Callback when search button is clicked
 * @param onWordClick Callback when a word item is clicked (for editing)
 * @param onAddClick Callback when add button is clicked
 * @param onPreviousPageClick Callback for previous page navigation
 * @param onNextPageClick Callback for next page navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageImScreen(
    viewModel: ManageImViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_im_management)) },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add word")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Keyboard selection button
            OutlinedButton(
                onClick = { viewModel.showKeyboardDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (uiState.currentKeyboard.isNotEmpty()) {
                        uiState.currentKeyboard
                    } else {
                        stringResource(R.string.manage_im_keyboard)
                    }
                )
            }

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Loading indicator or content
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
                // Word grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.words) { word ->
                        WordItem(
                            word = word,
                            onClick = { viewModel.showEditDialog(word) }
                        )
                    }
                }
            }

            // Pagination controls
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

    // Add Word Dialog
    if (uiState.showAddDialog) {
        WordDialog(
            mode = WordDialogMode.ADD,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { code, word, score ->
                viewModel.addWord(code, word, score)
            }
        )
    }

    // Edit Word Dialog
    uiState.editingWord?.let { word ->
        WordDialog(
            mode = WordDialogMode.EDIT,
            word = word,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { code, wordText, score ->
                viewModel.updateWord(word.id, code, wordText, score)
            },
            onDelete = {
                viewModel.removeWord(word.id)
            }
        )
    }

    // Keyboard Selection Dialog
    if (uiState.showKeyboardDialog) {
        KeyboardSelectionDialog(
            keyboards = uiState.availableKeyboards,
            onKeyboardSelected = { keyboard ->
                viewModel.selectKeyboard(keyboard)
            },
            onDismiss = { viewModel.hideKeyboardDialog() }
        )
    }
}

/**
 * Search bar composable for word search.
 *
 * @param query Current search query
 * @param onQueryChange Callback when query changes
 * @param onSearch Callback when search is triggered
 * @param modifier Modifier for styling
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.manage_im_search)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}
