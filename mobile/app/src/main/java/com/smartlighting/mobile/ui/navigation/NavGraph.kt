package com.smartlighting.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartlighting.mobile.ui.screens.auth.LoginScreen
import com.smartlighting.mobile.ui.screens.main.MainScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login screen
        composable(route = Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Routes.Main.route) {
            MainScreen()
        }
    }
}
