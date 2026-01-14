package nan.toload.main.hd

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.platform.AndroidUiDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.setViewTreeViewModelStoreOwner
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
        android.util.Log.d("EMOJI_DEBUG", "=== ComposeBridge: createEmojiPickerView() called ===")

        // Create lifecycle owner for Compose
        val lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        android.util.Log.d("EMOJI_DEBUG", "Lifecycle owner created")

        // Create a custom coroutine scope with AndroidUiDispatcher (includes frame clock)
        val coroutineScope = CoroutineScope(AndroidUiDispatcher.Main)

        return ComposeView(context).apply {
            android.util.Log.d("EMOJI_DEBUG", "Creating ComposeView for emoji picker")

            // Set custom WindowRecomposerFactory that doesn't need ViewTreeLifecycleOwner
            setViewCompositionStrategy(object : ViewCompositionStrategy {
                override fun installFor(view: androidx.compose.ui.platform.AbstractComposeView): () -> Unit {
                    // Create our own Recomposer that doesn't rely on view tree lifecycle
                    val recomposer = Recomposer(coroutineScope.coroutineContext)
                    view.setParentCompositionContext(recomposer)

                    // Start the recomposer
                    coroutineScope.launch {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    // Return cleanup function
                    return {
                        recomposer.cancel()
                        coroutineScope.cancel()
                    }
                }
            })

            // Set lifecycle owner for other Compose features that might need it
            android.util.Log.d("EMOJI_DEBUG", "Setting lifecycle owners on ComposeView")
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            // Set explicit layout parameters with height matching Gboard
            val heightDp = 400  // Adjusted height as per user request
            val density = context.resources.displayMetrics.density
            val heightPx = (heightDp * density).toInt()
            android.util.Log.d("EMOJI_DEBUG", "Setting layout params: height=${heightDp}dp (${heightPx}px)")
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )

            // Set background to ensure visibility (for debugging)
            android.util.Log.d("EMOJI_DEBUG", "Setting background color to #1F1F1F")
            setBackgroundColor(android.graphics.Color.parseColor("#1F1F1F"))

            android.util.Log.d("EMOJI_DEBUG", "Setting Compose content with EmojiPicker")
            setContent {
                android.util.Log.d("EMOJI_DEBUG", "Composing EmojiPicker UI")
                MaterialTheme {
                    EmojiPicker(
                        onEmojiClick = { emoji ->
                            android.util.Log.d("EMOJI_DEBUG", "Emoji clicked: $emoji")
                            service.onText(emoji)
                        },
                        onBackClick = {
                            android.util.Log.d("EMOJI_DEBUG", "Back button clicked")
                            service.closeEmojiPicker()
                        },
                        onBackspaceClick = {
                            android.util.Log.d("EMOJI_DEBUG", "Backspace clicked")
                            // Simulate Backspace
                            service.handleComposeBackspace()
                        }
                    )
                }
            }
            android.util.Log.d("EMOJI_DEBUG", "ComposeView created successfully")
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

// Custom LifecycleOwner for Compose in Service
class ComposeLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
