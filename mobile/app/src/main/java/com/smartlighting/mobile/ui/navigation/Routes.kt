package com.smartlighting.mobile.ui.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Main : Routes("main")
}
