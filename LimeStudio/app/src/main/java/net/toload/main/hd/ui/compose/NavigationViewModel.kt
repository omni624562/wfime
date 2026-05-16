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

package net.toload.main.hd.ui.compose

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
import net.toload.main.hd.limedb.LimeDB

/**
 * UI state for the navigation drawer.
 *
 * @param menuItems List of navigation menu items
 * @param selectedPosition Currently selected menu item position
 */
data class NavigationUiState(
    val menuItems: List<NavigationMenuItem> = emptyList(),
    val selectedPosition: Int = 0
)

/**
 * ViewModel for managing navigation drawer state.
 *
 * Loads menu items from the database and tracks selection state.
 * Menu structure:
 * - Position 0: "設定輸入法" (Initial/Setup)
 * - Position 1: "管理關聯字庫" (Manage Related)
 * - Position 2+: Dynamic input method names from database
 *
 * @param context Application context for accessing resources and database
 */
class NavigationViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private val limeDb: LimeDB = LimeDB(context)

    init {
        loadMenuItems()
    }

    /**
     * Loads menu items from the database.
     * Executes on IO dispatcher to avoid blocking the main thread.
     */
    fun loadMenuItems() {
        viewModelScope.launch {
            val menuItems = withContext(Dispatchers.IO) {
                buildMenuItems()
            }
            _uiState.update { it.copy(menuItems = menuItems) }
        }
    }

    /**
     * Builds the menu items list.
     * - First 2 items are fixed (Initial, Related)
     * - Remaining items are loaded from database (Input Methods)
     */
    private fun buildMenuItems(): List<NavigationMenuItem> {
        val items = mutableListOf<NavigationMenuItem>()

        // Add fixed menu items
        items.add(
            NavigationMenuItem(
                title = context.getString(R.string.default_menu_initial),
                position = 0
            )
        )
        items.add(
            NavigationMenuItem(
                title = context.getString(R.string.default_menu_related),
                position = 1
            )
        )

        // Add dynamic input method items from database
        val imList = limeDb.getIm(null, Lime.IM_TYPE_NAME)
        imList.forEachIndexed { index, im ->
            items.add(
                NavigationMenuItem(
                    title = im.desc,
                    position = index + 2  // Offset by 2 for fixed items
                )
            )
        }

        return items
    }

    /**
     * Updates the selected menu item position.
     *
     * @param position The new selected position
     */
    fun selectItem(position: Int) {
        _uiState.update { it.copy(selectedPosition = position) }
    }

    /**
     * Gets the currently selected position.
     *
     * @return Current selected position
     */
    fun getSelectedPosition(): Int = _uiState.value.selectedPosition
}

/**
 * Factory for creating NavigationViewModel instances.
 *
 * @param context Application context
 */
class NavigationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NavigationViewModel::class.java)) {
            return NavigationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
