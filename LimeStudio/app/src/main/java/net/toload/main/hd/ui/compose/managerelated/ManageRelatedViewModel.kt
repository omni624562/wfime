/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package net.toload.main.hd.ui.compose.managerelated

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
import net.toload.main.hd.Lime
import net.toload.main.hd.R
import net.toload.main.hd.data.Related
import net.toload.main.hd.limedb.LimeDB

data class ManageRelatedUiState(
    val items: List<Related> = emptyList(),
    val searchQuery: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalRecords: Int = 0,
    val pageSize: Int = Lime.IM_MANAGE_DISPLAY_AMOUNT,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingItem: Related? = null,
    val errorMessage: String? = null
)

class ManageRelatedViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageRelatedUiState())
    val uiState: StateFlow<ManageRelatedUiState> = _uiState.asStateFlow()

    private val limeDb = LimeDB(context)

    init {
        loadPage()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch() {
        _uiState.update { it.copy(currentPage = 0) }
        loadPage()
    }

    fun resetSearch() {
        _uiState.update { it.copy(searchQuery = "", currentPage = 0) }
        loadPage()
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
            loadPage()
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages - 1) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
            loadPage()
        }
    }

    private fun loadPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (items, total, pages) = withContext(Dispatchers.IO) {
                val state = _uiState.value
                val query = state.searchQuery.takeIf { it.isNotBlank() }
                val offset = state.currentPage * state.pageSize
                val total = limeDb.getRelatedSize(query)
                val pages = if (total > 0) (total + state.pageSize - 1) / state.pageSize else 0
                Triple(limeDb.loadRelated(query, state.pageSize, offset), total, pages)
            }
            _uiState.update {
                it.copy(items = items, totalRecords = total, totalPages = pages, isLoading = false)
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, errorMessage = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, errorMessage = null) }
    }

    fun showEditDialog(item: Related) {
        _uiState.update { it.copy(editingItem = item, errorMessage = null) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(editingItem = null, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun addRelated(pword: String, cword: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val check = limeDb.hasRelated(pword, cword)
            if (check == 0) {
                val obj = Related().apply {
                    setPword(pword)
                    setCword(cword)
                    setBasescore(score)
                    setUserscore(0)
                }
                limeDb.insert(Related.getInsertQuery(obj))
                withContext(Dispatchers.Main) {
                    hideAddDialog()
                    _uiState.update { it.copy(currentPage = 0) }
                    loadPage()
                }
            } else {
                val msg = if (check == 9999999) {
                    context.getString(R.string.manage_related_format_error)
                } else {
                    context.getString(R.string.manage_related_duplicated)
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = msg) }
                }
            }
        }
    }

    fun updateRelated(id: Int, pword: String, cword: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val sql = "UPDATE ${Lime.DB_RELATED} SET " +
                "${Lime.DB_RELATED_COLUMN_PWORD} = \"${Lime.formatSqlValue(pword)}\", " +
                "${Lime.DB_RELATED_COLUMN_CWORD} = \"${Lime.formatSqlValue(cword)}\", " +
                "${Lime.DB_RELATED_COLUMN_BASESCORE} = \"$score\" " +
                "WHERE ${Lime.DB_RELATED_COLUMN_ID} = \"$id\""
            limeDb.update(sql)
            withContext(Dispatchers.Main) {
                hideEditDialog()
                loadPage()
            }
        }
    }

    fun removeRelated(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            limeDb.removeById(Lime.DB_RELATED, id.toString())
            withContext(Dispatchers.Main) {
                hideEditDialog()
                val state = _uiState.value
                val newTotal = (state.totalRecords - 1).coerceAtLeast(0)
                val newPages = if (newTotal > 0) (newTotal + state.pageSize - 1) / state.pageSize else 0
                val newPage = state.currentPage.coerceAtMost((newPages - 1).coerceAtLeast(0))
                _uiState.update { it.copy(currentPage = newPage) }
                loadPage()
            }
        }
    }
}

class ManageRelatedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageRelatedViewModel::class.java)) {
            return ManageRelatedViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
