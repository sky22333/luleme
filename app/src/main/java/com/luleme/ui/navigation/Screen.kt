package com.luleme.ui.navigation

sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Home : Screen("home")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}
