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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nan.toload.main.hd.Lime
import nan.toload.main.hd.R

/**
 * Pagination controls for navigating through word pages.
 *
 * Displays:
 * - Previous button (disabled on first page)
 * - Page info (e.g., "1-100 of 500")
 * - Next button (disabled on last page)
 *
 * @param currentPage Zero-based current page index
 * @param totalPages Total number of pages
 * @param totalRecords Total number of records
 * @param pageSize Number of records per page
 * @param onPreviousClick Callback for previous button
 * @param onNextClick Callback for next button
 * @param modifier Modifier for styling
 */
@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    totalRecords: Int,
    pageSize: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startRecord = if (totalRecords > 0) currentPage * pageSize + 1 else 0
    val endRecord = minOf((currentPage + 1) * pageSize, totalRecords)

    val pageInfo = if (totalRecords > 0) {
        "${Lime.format(startRecord)}-${Lime.format(endRecord)} of ${Lime.format(totalRecords)}"
    } else {
        "0"
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            Button(
                onClick = onPreviousClick,
                enabled = currentPage > 0,
                modifier = Modifier.width(120.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.manage_im_previous))
            }

            // Page info
            Text(
                text = pageInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Next button
            Button(
                onClick = onNextClick,
                enabled = currentPage < totalPages - 1 && totalRecords > 0,
                modifier = Modifier.width(120.dp)
            ) {
                Text(stringResource(R.string.manage_im_next))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
