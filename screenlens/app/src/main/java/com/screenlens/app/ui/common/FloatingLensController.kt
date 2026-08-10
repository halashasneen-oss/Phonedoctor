package com.screenlens.app.ui.common

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.overlay.FloatingLensService
import com.screenlens.app.util.OverlayPermissionHelper
import kotlinx.coroutines.launch

/**
 * Shared "turn Floating Lens on/off" flow used from both Settings and Tools:
 * explain what the overlay permission is for, request it if missing, then
 * start/stop [FloatingLensService] and persist the choice. Must be constructed
 * while the host Fragment can still call registerForActivityResult (e.g. as a
 * field initializer), not lazily inside a click handler.
 */
class FloatingLensController(private val fragment: Fragment) {

    private val requestOverlayPermission = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        applyState(OverlayPermissionHelper.hasPermission(fragment.requireContext()))
    }

    fun setEnabled(enabled: Boolean) {
        val context = fragment.requireContext()
        if (!enabled) {
            applyState(false)
            return
        }
        if (OverlayPermissionHelper.hasPermission(context)) {
            applyState(true)
            return
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.floating_lens_title)
            .setMessage(R.string.overlay_permission_body)
            .setPositiveButton(R.string.action_enable) { _, _ ->
                requestOverlayPermission.launch(OverlayPermissionHelper.requestPermissionIntent(context))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun applyState(enabled: Boolean) {
        val context = fragment.requireContext()
        fragment.lifecycleScope.launch {
            ServiceLocator.settingsDataStore.setFloatingLensEnabled(enabled)
        }
        if (enabled) FloatingLensService.start(context) else FloatingLensService.stop(context)
    }
}
