package com.safeassist.maramag.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val initialLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission outcome is handled gracefully when SOS/GPS is used
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val app = application as SafeAssistApplication
        if (savedInstanceState == null) {
            val initialSettings = runBlocking { app.repository.appSettings.first() }
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(
                if (initialSettings.hasCompletedSetup) R.id.homeFragment else R.id.setupFragment
            )
            navController.graph = navGraph

            if (!initialSettings.hasRequestedInitialPermissions &&
                !app.locationProvider.hasLocationPermission()
            ) {
                lifecycleScope.launch {
                    app.repository.setHasRequestedInitialPermissions(true)
                }
                initialLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        binding.bottomNav.setupWithNavController(navController)

        // Hide bottom bar on full-screen SOS & form preparation screens to prevent accidental tab switching
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.emergencyContactsFragment,
            R.id.profileFragment,
            R.id.settingsFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in topLevelDestinations) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop any active siren when the app goes into the background unless user explicitly returned
        // Keeping lifecycle clean prevents stuck audio if the app is backgrounded/interrupted
        if (!isChangingConfigurations) {
            (application as SafeAssistApplication).sirenManager.stopSiren()
        }
    }
}
