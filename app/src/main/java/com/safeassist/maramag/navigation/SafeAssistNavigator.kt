package com.safeassist.maramag.navigation

import androidx.navigation.NavController
import com.safeassist.maramag.R

object SafeAssistNavigator {

    fun navigateToSos(navController: NavController) {
        if (navController.currentDestination?.id == R.id.homeFragment) {
            navController.navigate(R.id.action_home_to_sosReadyToSend)
        }
    }

    fun navigateToWhereAndNeeds(navController: NavController) {
        if (navController.currentDestination?.id == R.id.homeFragment) {
            navController.navigate(R.id.action_home_to_whereAndNeeds)
        }
    }

    fun navigateToReviewAndShare(navController: NavController) {
        if (navController.currentDestination?.id == R.id.whereAndNeedsFragment) {
            navController.navigate(R.id.action_whereAndNeeds_to_reviewAndShare)
        }
    }

    fun navigateHomeFromSos(navController: NavController) {
        navController.popBackStack(R.id.homeFragment, false)
    }
}
