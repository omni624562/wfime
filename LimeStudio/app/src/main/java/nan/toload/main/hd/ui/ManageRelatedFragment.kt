/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package nan.toload.main.hd.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import nan.toload.main.hd.MainActivity
import nan.toload.main.hd.data.Related
import nan.toload.main.hd.ui.compose.managerelated.ManageRelatedScreen
import nan.toload.main.hd.ui.compose.managerelated.ManageRelatedViewModel
import nan.toload.main.hd.ui.compose.managerelated.ManageRelatedViewModelFactory

class ManageRelatedFragment : Fragment() {

    private lateinit var viewModel: ManageRelatedViewModel

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): ManageRelatedFragment {
            return ManageRelatedFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as? MainActivity)?.onSectionAttached(
            arguments?.getInt(ARG_SECTION_NUMBER) ?: 0
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            this,
            ManageRelatedViewModelFactory(requireContext().applicationContext)
        )[ManageRelatedViewModel::class.java]

        return ComposeView(requireContext()).apply {
            // Set ViewTree owners so internal Compose components walking the tree also find them.
            // This prevents "ViewTreeLifecycleOwner not found" errors.
            setViewTreeLifecycleOwner(viewLifecycleOwner)

            // Robust lookup for SavedStateRegistryOwner from activity context
            var currentContext: Context? = context
            while (currentContext is ContextWrapper) {
                if (currentContext is SavedStateRegistryOwner) {
                    setViewTreeSavedStateRegistryOwner(currentContext)
                    break
                }
                currentContext = currentContext.baseContext
            }

            setViewTreeViewModelStoreOwner(this@ManageRelatedFragment)

            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                MaterialTheme(colorScheme = colorScheme) {
                    ManageRelatedScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Legacy stubs — kept so ManageRelatedHandler.java continues to compile
    @Deprecated("Legacy Handler API — not used by Compose implementation")
    fun showProgress() {}

    @Deprecated("Legacy Handler API — not used by Compose implementation")
    fun addRelated(pword: String?, cword: String?, score: Int) {}

    @Deprecated("Legacy Handler API — not used by Compose implementation")
    fun updateRelated(id: Int, pword: String?, cword: String?, score: Int) {}

    @Deprecated("Legacy Handler API — not used by Compose implementation")
    fun removeRelated(id: Int) {}

    @Deprecated("Legacy Handler API — not used by Compose implementation")
    fun updateGridView(relatedlist: List<Related>?) {}
}
