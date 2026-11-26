package com.safeassist.maramag.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

            val needsNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            val needsInitialLocation = !initialSettings.hasRequestedInitialPermissions &&
                !app.locationProvider.hasLocationPermission()

            if (needsInitialLocation || needsNotifications) {
                if (needsInitialLocation) {
                    lifecycleScope.launch {
                        app.repository.setHasRequestedInitialPermissions(true)
                    }
                }
                val permissions = buildList {
                    if (needsInitialLocation) {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    if (needsNotifications) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                initialLocationPermissionLauncher.launch(permissions)
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

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            (application as SafeAssistApplication).sirenManager.stopSiren()
        }
    }
}
