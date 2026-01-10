package nan.toload.main.hd

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import nan.toload.main.hd.ui.EmojiPicker
import nan.toload.main.hd.ui.compose.NavigationDrawerScreen
import nan.toload.main.hd.ui.compose.NavigationViewModel
import nan.toload.main.hd.ui.compose.NavigationViewModelFactory
import nan.toload.main.hd.ui.compose.manageword.ManageImScreen
import nan.toload.main.hd.ui.compose.manageword.ManageImViewModel
import nan.toload.main.hd.ui.compose.manageword.ManageImViewModelFactory
import nan.toload.main.hd.data.Word
import android.view.KeyEvent

/**
 * Bridge object for creating Compose-based views that can be integrated into Java code.
 */
object ComposeBridge {
    /**
     * Creates an emoji picker view using Jetpack Compose.
     *
     * @param context Android context
     * @param service IME service for handling emoji input
     * @return View containing the emoji picker UI
     */
    fun createEmojiPickerView(context: Context, service: LIMEService): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                EmojiPicker(
                    onEmojiClick = { emoji ->
                        service.onText(emoji)
                    },
                    onBackClick = {
                        service.closeEmojiPicker()
                    },
                    onBackspaceClick = {
                        // Simulate Backspace
                         service.handleComposeBackspace()
                    }
                )
            }
        }
    }

    /**
     * Creates a navigation drawer view using Jetpack Compose.
     *
     * @param context Android context
     * @param viewModelStoreOwner Owner of the ViewModelStore (typically the Activity)
     * @param callbacks Callbacks for handling navigation drawer events
     * @return View containing the navigation drawer UI
     */
    fun createNavigationDrawerView(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        callbacks: NavigationDrawerCallbacks
    ): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // Create ViewModel
                val factory = NavigationViewModelFactory(context)
                val viewModel = ViewModelProvider(viewModelStoreOwner, factory)[NavigationViewModel::class.java]

                // Observe UI state
                val uiState by viewModel.uiState.collectAsState()

                MaterialTheme {
                    NavigationDrawerScreen(
                        menuItems = uiState.menuItems,
                        selectedPosition = uiState.selectedPosition,
                        onMenuItemClick = { position ->
                            viewModel.selectItem(position)
                            callbacks.onNavigationDrawerItemSelected(position)
                        }
                    )
                }
            }
        }
    }

    /**
     * Creates a Manage IM view using Jetpack Compose.
     *
     * @param context Android context
     * @param viewModelStoreOwner Owner of the ViewModelStore (typically the Activity or Fragment)
     * @param table Database table name for the input method
     * @param callbacks Callbacks for handling word management events
     * @return View containing the Manage IM UI
     */
    fun createManageImView(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        table: String,
        callbacks: ManageImCallbacks
    ): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // Create ViewModel
                val factory = ManageImViewModelFactory(context, table)
                val viewModel = ViewModelProvider(viewModelStoreOwner, factory)[ManageImViewModel::class.java]

                // Observe UI state
                val uiState by viewModel.uiState.collectAsState()

                MaterialTheme {
                    ManageImScreen(
                        uiState = uiState,
                        onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                        onSearchClick = { viewModel.performSearch() },
                        onWordClick = { word -> callbacks.onWordClick(word) },
                        onAddClick = { callbacks.onAddClick(table) },
                        onPreviousPageClick = { viewModel.previousPage() },
                        onNextPageClick = { viewModel.nextPage() }
                    )
                }
            }
        }
    }

    /**
     * Callback interface for navigation drawer events.
     */
    interface NavigationDrawerCallbacks {
        /**
         * Called when a navigation drawer item is selected.
         *
         * @param position The position of the selected item
         */
        fun onNavigationDrawerItemSelected(position: Int)
    }

    /**
     * Callback interface for Manage IM events.
     */
    interface ManageImCallbacks {
        /**
         * Called when a word is clicked (for editing).
         *
         * @param word The word that was clicked
         */
        fun onWordClick(word: Word)

        /**
         * Called when the add button is clicked.
         *
         * @param table The table name
         */
        fun onAddClick(table: String)
    }
}
