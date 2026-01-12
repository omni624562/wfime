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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nan.toload.main.hd.Lime
import nan.toload.main.hd.data.Keyboard
import nan.toload.main.hd.data.Word
import nan.toload.main.hd.limedb.LimeDB

/**
 * UI state for the Manage IM screen.
 *
 * @param words List of words to display in the current page
 * @param searchQuery Current search query text
 * @param currentPage Zero-based current page index
 * @param totalPages Total number of pages
 * @param totalRecords Total number of records matching the query
 * @param pageSize Number of records per page
 * @param isLoading Whether data is currently being loaded
 * @param searchRoot Whether to search only word roots (true) or full words (false)
 * @param showAddDialog Whether to show the add word dialog
 * @param editingWord Word being edited (null if not editing)
 * @param showKeyboardDialog Whether to show the keyboard selection dialog
 * @param availableKeyboards List of available keyboard layouts
 * @param currentKeyboard Currently selected keyboard description
 */
data class ManageImUiState(
    val words: List<Word> = emptyList(),
    val searchQuery: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalRecords: Int = 0,
    val pageSize: Int = Lime.IM_MANAGE_DISPLAY_AMOUNT,
    val isLoading: Boolean = false,
    val searchRoot: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingWord: Word? = null,
    val showKeyboardDialog: Boolean = false,
    val availableKeyboards: List<Keyboard> = emptyList(),
    val currentKeyboard: String = ""
)

/**
 * ViewModel for managing input method words.
 *
 * Handles:
 * - Loading words from database with pagination
 * - Search functionality
 * - Add/Update/Delete operations
 * - State management
 *
 * @param context Application context for database access
 * @param table Database table name for the input method
 */
class ManageImViewModel(
    private val context: Context,
    private val table: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageImUiState())
    val uiState: StateFlow<ManageImUiState> = _uiState.asStateFlow()

    private val limeDb: LimeDB = LimeDB(context)

    init {
        // Load initial data
        loadWords()
        loadKeyboards()
    }

    /**
     * Updates the search query text.
     * Does not trigger search automatically - call performSearch() to execute.
     *
     * @param query New search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Performs search with the current query.
     * Resets to page 0 and reloads data.
     */
    fun performSearch() {
        _uiState.update { it.copy(currentPage = 0) }
        loadWords()
    }

    /**
     * Navigates to the previous page.
     */
    fun previousPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage > 0) {
            _uiState.update { it.copy(currentPage = currentPage - 1) }
            loadWords()
        }
    }

    /**
     * Navigates to the next page.
     */
    fun nextPage() {
        val currentPage = _uiState.value.currentPage
        val totalPages = _uiState.value.totalPages
        if (currentPage < totalPages - 1) {
            _uiState.update { it.copy(currentPage = currentPage + 1) }
            loadWords()
        }
    }

    /**
     * Toggles search root mode.
     * When true, searches only word roots. When false, searches full words.
     */
    fun toggleSearchRoot() {
        _uiState.update { state ->
            state.copy(
                searchRoot = !state.searchRoot,
                currentPage = 0,
                searchQuery = ""
            )
        }
        loadWords()
    }

    /**
     * Loads words from database based on current state.
     * Executes on IO dispatcher to avoid blocking main thread.
     */
    private fun loadWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = withContext(Dispatchers.IO) {
                val query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
                val searchRoot = _uiState.value.searchRoot
                val pageSize = _uiState.value.pageSize
                val currentPage = _uiState.value.currentPage
                val offset = currentPage * pageSize

                // Get total count
                val totalRecords = limeDb.getWordSize(table, query, searchRoot)
                val totalPages = if (totalRecords > 0) {
                    (totalRecords + pageSize - 1) / pageSize
                } else {
                    0
                }

                // Load words for current page
                val words = limeDb.loadWord(table, query, searchRoot, pageSize, offset)

                Triple(words, totalRecords, totalPages)
            }

            _uiState.update {
                it.copy(
                    words = result.first,
                    totalRecords = result.second,
                    totalPages = result.third,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Shows the add word dialog.
     */
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    /**
     * Hides the add word dialog.
     */
    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    /**
     * Shows the edit word dialog for a specific word.
     *
     * @param word The word to edit
     */
    fun showEditDialog(word: Word) {
        _uiState.update { it.copy(editingWord = word) }
    }

    /**
     * Hides the edit word dialog.
     */
    fun hideEditDialog() {
        _uiState.update { it.copy(editingWord = null) }
    }

    /**
     * Adds a new word to the database.
     *
     * @param code Input code
     * @param word Word text
     * @param score Word score
     */
    fun addWord(code: String, word: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            limeDb.addOrUpdateMappingRecord(table, code, word.trim(), score)
            // Reload current page
            withContext(Dispatchers.Main) {
                hideAddDialog()
                loadWords()
            }
        }
    }

    /**
     * Updates an existing word in the database.
     *
     * @param id Word ID
     * @param code Input code
     * @param word Word text
     * @param score Word score
     */
    fun updateWord(id: Int, code: String, word: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            limeDb.addOrUpdateMappingRecord(table, code, word.trim(), score)
            // Reload current page
            withContext(Dispatchers.Main) {
                hideEditDialog()
                loadWords()
            }
        }
    }

    /**
     * Removes a word from the database.
     *
     * @param id Word ID
     */
    fun removeWord(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            limeDb.removeById(table, id.toString())
            // Reload current page
            withContext(Dispatchers.Main) {
                hideEditDialog()
                loadWords()
            }
        }
    }

    /**
     * Loads available keyboard layouts from database.
     */
    private fun loadKeyboards() {
        viewModelScope.launch(Dispatchers.IO) {
            val keyboards = limeDb.getKeyboard()
            // Get current keyboard for this IM
            val currentKb = limeDb.getImKeyboard(table)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        availableKeyboards = keyboards,
                        currentKeyboard = currentKb?.desc ?: ""
                    )
                }
            }
        }
    }

    /**
     * Shows the keyboard selection dialog.
     */
    fun showKeyboardDialog() {
        _uiState.update { it.copy(showKeyboardDialog = true) }
    }

    /**
     * Hides the keyboard selection dialog.
     */
    fun hideKeyboardDialog() {
        _uiState.update { it.copy(showKeyboardDialog = false) }
    }

    /**
     * Selects a keyboard layout for this input method.
     *
     * @param keyboard Selected keyboard layout
     */
    fun selectKeyboard(keyboard: Keyboard) {
        viewModelScope.launch(Dispatchers.IO) {
            limeDb.setImKeyboard(table, keyboard)

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(currentKeyboard = keyboard.desc) }
                hideKeyboardDialog()
            }
        }
    }

    /**
     * Updates the keyboard for this input method.
     *
     * @param keyboardCode Keyboard code
     */
    fun updateKeyboard(keyboardCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyboards = limeDb.getKeyboard()
            val keyboard = keyboards.find { it.code == keyboardCode }
            if (keyboard != null) {
                limeDb.setImKeyboard(table, keyboard)
            }
        }
    }
}

/**
 * Factory for creating ManageImViewModel instances.
 *
 * @param context Application context
 * @param table Database table name
 */
class ManageImViewModelFactory(
    private val context: Context,
    private val table: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageImViewModel::class.java)) {
            return ManageImViewModel(context, table) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
