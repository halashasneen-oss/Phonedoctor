package com.phonedoctor.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.phonedoctor.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startWithOnboarding = intent.getBooleanExtra(EXTRA_START_WITH_ONBOARDING, false)
        if (startWithOnboarding) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(R.id.onboardingFragment)
            navController.graph = graph
        }
    }

    companion object {
        const val EXTRA_START_WITH_ONBOARDING = "start_with_onboarding"
    }
}
