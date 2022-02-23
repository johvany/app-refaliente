package com.di.refaliente.shared

import android.view.View
import com.di.refaliente.databinding.LoadingWindowBinding

/**
 * @param binding
 * Binding of the loading window layout.
 *
 * @param viewsToDisable
 * Array of views that will be disabled when the loading window is showing (when calling
 * LoadingWindow.show() function). The views will be enabled after hide loading (when calling
 * "LoadingWindow.show()" function).
 *
 * @author Spiderman E22.
 */
class LoadingWindow(
    private val binding: LoadingWindowBinding,
    private val viewsToDisable: Array<View>
) {
    /**
     * Show the loading window by making visible their container (binding.root). This will disable
     * all views of the "viewsToDisable" array, to prevent the user interacting with them.
     */
    fun show() {
        viewsToDisable.forEach { view -> view.isEnabled = false }
        binding.root.visibility = View.VISIBLE
    }

    /**
     * Hide the loading window by making invisible their container (binding.root). This will enable
     * all views of the "viewsToDisable" array, to let the user interact with them.
     */
    fun hide() {
        viewsToDisable.forEach { view -> view.isEnabled = true }
        binding.root.visibility = View.INVISIBLE
    }

    /**
     * Set the message that will be shown in the loading window when this is visible.
     */
    fun setMessage(text: String) {
        binding.loadingText.text = text
    }
}