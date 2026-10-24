package com.example.timezonex.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "Splash")
    data object Home : Screen("home", "Home")
    data object Cities : Screen("cities", "Cities")
    data object Settings : Screen("settings", "Settings")
    data object About : Screen("about", "About")
}
