package com.screenlens.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.screenlens.app.BuildConfig
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentSettingsBinding
import com.screenlens.app.domain.AppLanguage
import com.screenlens.app.domain.ThemeMode
import com.screenlens.app.ui.common.FloatingLensController
import com.screenlens.app.ui.onboarding.OnboardingActivity
import com.screenlens.app.util.AppearanceManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val floatingLensController = FloatingLensController(this)
    private var suppressCallbacks = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            val mode = when (checkedId) {
                binding.themeLight.id -> ThemeMode.LIGHT
                binding.themeDark.id -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            AppearanceManager.applyThemeMode(mode)
            lifecycleScope.launch { ServiceLocator.settingsDataStore.setThemeMode(mode) }
        }

        binding.languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            val language = if (checkedId == binding.languageArabic.id) AppLanguage.ARABIC else AppLanguage.ENGLISH
            AppearanceManager.applyAppLanguage(language)
            lifecycleScope.launch { ServiceLocator.settingsDataStore.setAppLanguage(language) }
        }

        binding.floatingLensSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            floatingLensController.setEnabled(isChecked)
        }

        binding.autoSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            lifecycleScope.launch { ServiceLocator.settingsDataStore.setAutoSaveHistory(isChecked) }
        }

        binding.historyLimitSlider.addOnChangeListener { _, value, fromUser ->
            binding.historyLimitValue.text = formatLimit(value.toInt())
            if (fromUser) {
                lifecycleScope.launch { ServiceLocator.settingsDataStore.setHistoryLimit(value.toInt()) }
            }
        }

        binding.languageManagerRow.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_languageManager)
        }
        binding.clearHistoryRow.setOnClickListener { confirmClearHistory() }
        binding.notificationsRow.setOnClickListener { openNotificationSettings() }
        binding.privacyRow.setOnClickListener { findNavController().navigate(R.id.action_settings_to_privacy) }
        binding.premiumRow.setOnClickListener { findNavController().navigate(R.id.action_settings_to_premium) }
        binding.onboardingRow.setOnClickListener {
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }
        binding.rateRow.setOnClickListener { openStoreListing() }
        binding.shareRow.setOnClickListener { shareApp() }
        binding.aboutRow.setOnClickListener { showAbout() }

        observeSettings()
    }

    private fun formatLimit(limit: Int): String =
        if (limit <= 0) getString(R.string.history_limit_unlimited) else getString(R.string.history_limit_items, limit)

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ServiceLocator.settingsDataStore.settings.collect { settings ->
                    suppressCallbacks = true
                    binding.themeRadioGroup.check(
                        when (settings.themeMode) {
                            ThemeMode.LIGHT -> binding.themeLight.id
                            ThemeMode.DARK -> binding.themeDark.id
                            ThemeMode.SYSTEM -> binding.themeSystem.id
                        }
                    )
                    binding.languageRadioGroup.check(
                        if (settings.appLanguage == AppLanguage.ARABIC) binding.languageArabic.id else binding.languageEnglish.id
                    )
                    binding.floatingLensSwitch.isChecked = settings.floatingLensEnabled
                    binding.autoSaveSwitch.isChecked = settings.autoSaveHistory
                    binding.historyLimitSlider.value = settings.historyLimit.coerceIn(0, 500).toFloat()
                    binding.historyLimitValue.text = formatLimit(settings.historyLimit)
                    suppressCallbacks = false
                }
            }
        }
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_clear_history_title)
            .setMessage(R.string.dialog_clear_history_body)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch { ServiceLocator.historyRepository.clearAll() }
            }
            .show()
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:${requireContext().packageName}".toUri())
        }
        startActivity(intent)
    }

    private fun openStoreListing() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shareApp() {
        val packageName = requireContext().packageName
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.settings_share_app)))
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_version, BuildConfig.VERSION_NAME) + "\n\n" + getString(R.string.app_tagline))
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
