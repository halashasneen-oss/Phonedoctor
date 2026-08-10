package com.screenlens.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasVisitedAResultScreen = false

    companion object {
        const val EXTRA_ACTION = "com.screenlens.app.extra.ACTION"
        const val ACTION_START_SCAN = "start_scan"

        private val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.scanFragment,
            R.id.historyFragment,
            R.id.toolsFragment,
            R.id.settingsFragment
        )

        // Screens whose completion is a natural, non-disruptive point to show an
        // interstitial once the user navigates back to Home — never mid-scan/OCR/translation.
        private val RESULT_DESTINATIONS = setOf(
            R.id.ocrResultFragment,
            R.id.qrResultFragment,
            R.id.translatorFragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
        if (ServiceLocator.adManager.isEnabled) {
            ServiceLocator.adManager.preloadInterstitial()
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in TOP_LEVEL_DESTINATIONS) View.VISIBLE else View.GONE

            if (destination.id in RESULT_DESTINATIONS) {
                hasVisitedAResultScreen = true
            } else if (destination.id == R.id.homeFragment && hasVisitedAResultScreen) {
                hasVisitedAResultScreen = false
                ServiceLocator.adManager.showInterstitialIfAvailable(this) { }
            }
        }

        if (intent.getStringExtra(EXTRA_ACTION) == ACTION_START_SCAN) {
            navController.navigate(R.id.scanFragment)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra(EXTRA_ACTION) == ACTION_START_SCAN) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.scanFragment)
        }
    }
}
