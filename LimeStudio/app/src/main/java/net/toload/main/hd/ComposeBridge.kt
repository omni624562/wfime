package net.toload.main.hd

import android.content.Context
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import net.toload.main.hd.ui.EmojiPicker
import net.toload.main.hd.ui.compose.settings.SettingsScreen
import net.toload.main.hd.ui.compose.settings.SettingsViewModel
import net.toload.main.hd.ui.compose.settings.SettingsViewModelFactory
import net.toload.main.hd.ui.compose.theme.LimeTheme

/**
 * Bridge object for creating Compose-based views that can be integrated into Java code.
 */
object ComposeBridge {
    /**
     * Creates an emoji picker view using Jetpack Compose.
     *
     * Uses the same Recomposer + wrapper-FrameLayout pattern as CandidateView.kt so that
     * the Recomposer survives detach/re-attach cycles (ON_PAUSE on detach, never ON_DESTROY).
     * A fresh wrapper is created on each onCreateInputView call.
     *
     * @param context Android context
     * @param service IME service for handling emoji input
     * @return View containing the emoji picker UI
     */
    fun createEmojiPickerView(context: Context, service: LIMEService): View? {
        if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "=== ComposeBridge: createEmojiPickerView() called ===")

        return try {
            // Create Recomposer before the ComposeView — matches CandidateView.kt pattern.
            // Using AndroidUiDispatcher.Main avoids tying the Recomposer lifetime to a
            // CoroutineScope Job, so it is NOT cancelled when the wrapper detaches from the window.
            val coroutineScope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
            val recomposer = Recomposer(AndroidUiDispatcher.Main)
            coroutineScope.launch {
                recomposer.runRecomposeAndApplyChanges()
            }

            // Lifecycle owner: ON_CREATE + ON_START here; ON_RESUME fires in onAttachedToWindow.
            val composeLifecycleOwner = ComposeLifecycleOwner().apply {
                performRestore(null)
                handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                handleLifecycleEvent(Lifecycle.Event.ON_START)
            }

            val heightDp = 300
            val density = context.resources.displayMetrics.density
            val heightPx = (heightDp * density).toInt()
            val systemBarPaddingDp = 0 // Set to 0 to remove redundant bottom spacing as the system already handles IME navigation bar

            val composeView = ComposeView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx
                )
                // Bypass WindowRecomposer lookup with our own Recomposer — same as CandidateView.kt.
                setParentCompositionContext(recomposer)
                // Set ViewTree owners so internal Compose components walking the tree also find them.
                setViewTreeLifecycleOwner(composeLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(composeLifecycleOwner)
                setViewTreeViewModelStoreOwner(composeLifecycleOwner)
                // No explicit setViewCompositionStrategy — the default
                // (DisposeOnDetachedFromWindowOrReleasedFromPool) is installed by the constructor.
                setContent {
                    LimeTheme {
                        EmojiPicker(
                            onEmojiClick = { emoji ->
                                if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Emoji clicked: $emoji")
                                service.onText(emoji)
                            },
                            onBackClick = {
                                if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Back button clicked")
                                service.closeEmojiPicker()
                            },
                            onBackspaceClick = {
                                if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Backspace clicked")
                                service.handleComposeBackspace()
                            },
                            bottomPaddingDp = systemBarPaddingDp
                        )
                    }
                }
            }

            // Thin wrapper that drives lifecycle across attach/detach cycles.
            // Lifecycle is PAUSED (not DESTROYED) on detach so the Recomposer stays alive for reuse.
            object : android.widget.FrameLayout(context) {
                init {
                    if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Creating emoji wrapper FrameLayout")
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        heightPx
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#1F1F1F"))
                    
                    try {
                        addView(composeView)
                        if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Successfully added composeView to wrapper")
                    } catch (e: Exception) {
                        android.util.Log.e("EMOJI_DEBUG", "FAILED to add composeView to wrapper: ${e.message}", e)
                    }
                }

                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    composeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Emoji wrapper attached — lifecycle RESUMED")
                }

                override fun onDetachedFromWindow() {
                    composeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    if (BuildConfig.DEBUG) android.util.Log.d("EMOJI_DEBUG", "Emoji wrapper detached — lifecycle PAUSED")
                    super.onDetachedFromWindow()
                }

                // Suppress hover events reaching AndroidComposeView.
                // Compose BOM 2024.02 (UI 1.6.1) has a bug where a Handler-deferred
                // sendHoverExitEvent lambda throws IllegalStateException when hoveredItems
                // is empty. In an IME context hover events are irrelevant, so consuming
                // them here prevents the crash without any functional impact.
                override fun dispatchHoverEvent(event: android.view.MotionEvent): Boolean = true
            }
        } catch (e: Exception) {
            android.util.Log.e("EMOJI_DEBUG", "FAILED to create Emoji Picker View: ${e.message}", e)
            null
        }
    }

    /**
     * Creates a Settings view using Jetpack Compose.
     *
     * @param context Android context
     * @param viewModelStoreOwner Owner of the ViewModelStore (typically the Activity)
     * @return View containing the Settings UI
     */
    fun createSettingsView(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner
    ): View? {
        return try {
            ComposeView(context).apply {
                // Set ViewTree owners so internal Compose components walking the tree also find them.
                if (viewModelStoreOwner is LifecycleOwner) {
                    setViewTreeLifecycleOwner(viewModelStoreOwner)
                }
                if (viewModelStoreOwner is SavedStateRegistryOwner) {
                    setViewTreeSavedStateRegistryOwner(viewModelStoreOwner)
                }
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)

                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    val factory = SettingsViewModelFactory(context)
                    val viewModel = ViewModelProvider(viewModelStoreOwner, factory)[SettingsViewModel::class.java]

                    LimeTheme {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("COMPOSE_BRIDGE", "Failed to create Settings View: ${e.message}", e)
            null
        }
    }
}

// Custom LifecycleOwner for Compose in Service contexts (no built-in LifecycleOwner).
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
