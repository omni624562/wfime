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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Data class representing a navigation drawer menu item.
 *
 * @param title Display text for the menu item
 * @param position Position in the menu (used for selection tracking)
 */
data class NavigationMenuItem(
    val title: String,
    val position: Int
)

/**
 * Composable function for the navigation drawer screen.
 *
 * Displays a list of menu items with Material3 NavigationDrawerItem components.
 * Supports selection tracking and click callbacks.
 *
 * @param menuItems List of menu items to display
 * @param selectedPosition Currently selected menu item position
 * @param onMenuItemClick Callback when a menu item is clicked, receives position
 */
@Composable
fun NavigationDrawerScreen(
    menuItems: List<NavigationMenuItem>,
    selectedPosition: Int,
    onMenuItemClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            itemsIndexed(menuItems) { index, item ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    selected = index == selectedPosition,
                    onClick = { onMenuItemClick(index) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
