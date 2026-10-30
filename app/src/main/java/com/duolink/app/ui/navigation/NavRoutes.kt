package com.duolink.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object UsernameSetup : Screen("username_setup")
    object Permissions : Screen("permissions")
    object Onboarding : Screen("onboarding")
    object Pairing : Screen("pairing")
    object Home : Screen("home")
    object Chat : Screen("chat")
    object Call : Screen("call")
    object CallHistory : Screen("call_history")
    object MediaViewer : Screen("media_viewer")
    object Settings : Screen("settings")
    object Diagnostics : Screen("diagnostics")
    object About : Screen("about")
}
