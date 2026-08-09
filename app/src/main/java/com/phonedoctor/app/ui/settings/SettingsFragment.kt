package com.phonedoctor.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.phonedoctor.app.R
import com.phonedoctor.app.data.datastore.AppLanguage
import com.phonedoctor.app.data.datastore.AppSettings
import com.phonedoctor.app.data.datastore.AppThemeMode
import com.phonedoctor.app.databinding.FragmentSettingsBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import com.phonedoctor.app.work.WorkScheduler
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    private val viewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(serviceLocator()) as T
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.rowAppearance.setOnClickListener { showAppearanceDialog() }
        binding.rowLanguage.setOnClickListener { showLanguageDialog() }
        binding.rowDeviceInfo.setOnClickListener { findNavController().navigate(R.id.action_settings_to_device_info) }
        binding.rowClearHistory.setOnClickListener { confirmClearHistory() }
        binding.rowPrivacy.setOnClickListener { findNavController().navigate(R.id.action_settings_to_privacy) }
        binding.rowHelp.setOnClickListener { findNavController().navigate(R.id.action_settings_to_help) }
        binding.rowAbout.setOnClickListener { findNavController().navigate(R.id.action_settings_to_about) }

        binding.switchNotifications.setOnCheckedChangeListener { switchView, isChecked ->
            if (!switchView.isPressed) return@setOnCheckedChangeListener
            if (isChecked) requestNotificationPermissionIfNeeded()
            viewModel.setNotificationsEnabled(isChecked)
        }
        binding.switchAutoCheck.setOnCheckedChangeListener { switchView, isChecked ->
            if (!switchView.isPressed) return@setOnCheckedChangeListener
            viewModel.setAutoHealthCheckEnabled(isChecked)
            if (isChecked) {
                WorkScheduler.scheduleAutoHealthCheck(requireContext().applicationContext)
            } else {
                WorkScheduler.cancelAutoHealthCheck(requireContext().applicationContext)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { render(it) }
            }
        }
    }

    private fun render(settings: AppSettings) {
        binding.textAppearanceValue.setText(
            when (settings.themeMode) {
                AppThemeMode.SYSTEM -> R.string.settings_appearance_system
                AppThemeMode.LIGHT -> R.string.settings_appearance_light
                AppThemeMode.DARK -> R.string.settings_appearance_dark
            }
        )
        binding.textLanguageValue.setText(
            when (settings.language) {
                AppLanguage.ARABIC -> R.string.settings_language_arabic
                else -> R.string.settings_language_english
            }
        )
        binding.switchNotifications.isChecked = settings.notificationsEnabled
        binding.switchAutoCheck.isChecked = settings.autoHealthCheckEnabled
    }

    private fun showAppearanceDialog() {
        val options = arrayOf(
            getString(R.string.settings_appearance_system),
            getString(R.string.settings_appearance_light),
            getString(R.string.settings_appearance_dark)
        )
        val modes = arrayOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK)
        val current = modes.indexOf(viewModel.settings.value.themeMode).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_appearance)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setThemeMode(modes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showLanguageDialog() {
        val options = arrayOf(getString(R.string.settings_language_english), getString(R.string.settings_language_arabic))
        val languages = arrayOf(AppLanguage.ENGLISH, AppLanguage.ARABIC)
        val current = languages.indexOf(viewModel.settings.value.language).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setLanguage(languages[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_clear_confirm_title)
            .setMessage(R.string.history_clear_confirm_message)
            .setPositiveButton(R.string.common_delete) { _, _ -> viewModel.clearHistory() }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
