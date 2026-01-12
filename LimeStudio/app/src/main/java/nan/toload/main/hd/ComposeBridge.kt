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
import nan.toload.main.hd.ui.compose.settings.SettingsScreen
import nan.toload.main.hd.ui.compose.settings.SettingsViewModel
import nan.toload.main.hd.ui.compose.settings.SettingsViewModelFactory
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
     * All word management operations (add, edit, delete) are handled within the Compose UI.
     *
     * @param context Android context
     * @param viewModelStoreOwner Owner of the ViewModelStore (typically the Activity or Fragment)
     * @param table Database table name for the input method
     * @return View containing the Manage IM UI
     */
    fun createManageImView(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        table: String
    ): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // Create ViewModel
                val factory = ManageImViewModelFactory(context, table)
                val viewModel = ViewModelProvider(viewModelStoreOwner, factory)[ManageImViewModel::class.java]

                MaterialTheme {
                    ManageImScreen(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Creates a Settings view using Jetpack Compose.
     *
     * All preference operations are handled within the Compose UI with reactive state management.
     *
     * @param context Android context
     * @param viewModelStoreOwner Owner of the ViewModelStore (typically the Activity)
     * @return View containing the Settings UI
     */
    fun createSettingsView(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner
    ): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // Create ViewModel
                val factory = SettingsViewModelFactory(context)
                val viewModel = ViewModelProvider(viewModelStoreOwner, factory)[SettingsViewModel::class.java]

                MaterialTheme {
                    SettingsScreen(viewModel = viewModel)
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
}
