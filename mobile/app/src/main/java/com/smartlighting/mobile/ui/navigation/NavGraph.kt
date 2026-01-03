package com.smartlighting.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartlighting.mobile.data.local.TokenManager
import com.smartlighting.mobile.ui.screens.auth.LoginScreen
import com.smartlighting.mobile.ui.screens.main.MainScreen

/**
 * Navigation graph for the app
 * Determines start destination based on authentication state
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    tokenManager: TokenManager
) {
    // Determine start destination based on authentication state
    val startDestination = if (tokenManager.isAuthenticated()) {
        Routes.Main.route
    } else {
        Routes.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login screen
        composable(route = Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to main screen and clear back stack
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Main screen with logout callback
        composable(route = Routes.Main.route) {
            MainScreen(
                onLogout = {
                    // Navigate to login and clear entire back stack
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
