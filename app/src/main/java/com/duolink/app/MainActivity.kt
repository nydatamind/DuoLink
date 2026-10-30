package com.duolink.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.duolink.app.data.updater.UpdateChecker
import com.duolink.app.data.updater.UpdateInfo
import com.duolink.app.ui.navigation.Screen
import com.duolink.app.ui.screens.AboutScreen
import com.duolink.app.ui.screens.CallHistoryScreen
import com.duolink.app.ui.screens.CallScreen
import com.duolink.app.ui.screens.ChatScreen
import com.duolink.app.ui.screens.DiagnosticsScreen
import com.duolink.app.ui.screens.HomeScreen
import com.duolink.app.ui.screens.LoginScreen
import com.duolink.app.ui.screens.OnboardingScreen
import com.duolink.app.ui.screens.PairingScreen
import com.duolink.app.ui.screens.PermissionsSetupScreen
import com.duolink.app.ui.screens.SettingsScreen
import com.duolink.app.ui.screens.SplashScreen
import com.duolink.app.ui.screens.UsernameSetupScreen
import com.duolink.app.ui.theme.DuoLinkTheme
import com.duolink.app.ui.updater.UpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val deepLinkTrigger = mutableStateOf<Boolean>(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLink(intent)

        setContent {
            DuoLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DuoLinkAppNavigation(deepLinkTrigger.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "duolink" && uri.host == "auth") {
            lifecycleScope.launch {
                val result = DuoLinkApp.instance.authManager.handleOAuthCallback(uri)
                if (result.isSuccess) {
                    deepLinkTrigger.value = !deepLinkTrigger.value
                }
            }
        }
    }
}

@Composable
fun DuoLinkAppNavigation(deepLinkSignal: Boolean) {
    val app = DuoLinkApp.instance
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Splash

    var startupUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val updateChecker = remember { UpdateChecker(context) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            updateChecker.checkForUpdate(ignoreDismissed = false)
        }
        val update = result.getOrNull()
        if (update != null) {
            startupUpdate = update
        }
    }

    fun navigateTo(screen: Screen, clearStack: Boolean = false) {
        if (clearStack) {
            backStack.clear()
            backStack.add(screen)
        } else {
            if (backStack.lastOrNull() != screen) {
                backStack.add(screen)
            }
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        } else {
            when (currentScreen) {
                Screen.Pairing -> {
                    scope.launch {
                        if (app.pairingRepository.isPaired()) {
                            navigateTo(Screen.Home, clearStack = true)
                        } else {
                            navigateTo(Screen.Login, clearStack = true)
                        }
                    }
                }
                Screen.Chat, Screen.Call, Screen.CallHistory, Screen.Settings -> {
                    navigateTo(Screen.Home, clearStack = true)
                }
                Screen.Diagnostics, Screen.About, Screen.Permissions -> {
                    navigateTo(Screen.Settings)
                }
                Screen.UsernameSetup -> {
                    navigateTo(Screen.Login, clearStack = true)
                }
                Screen.Home, Screen.Login -> {
                    (context as? android.app.Activity)?.moveTaskToBack(true)
                }
                else -> {
                    (context as? android.app.Activity)?.finish()
                }
            }
        }
    }

    // Official Android Back Gesture / Button Handler
    BackHandler(enabled = true) {
        navigateBack()
    }

    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onNavigateToHome = {
                    navigateTo(Screen.Home, clearStack = true)
                },
                onNavigateToOnboarding = {
                    backStack.clear()
                    if (!app.authManager.isAuthenticated()) {
                        backStack.add(Screen.Login)
                    } else if (!app.authManager.hasUsername()) {
                        backStack.add(Screen.Login)
                        backStack.add(Screen.UsernameSetup)
                    } else {
                        backStack.add(Screen.Login)
                        backStack.add(Screen.Pairing)
                    }
                }
            )
        }
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = { isNewSignup ->
                    if (isNewSignup || !app.authManager.hasUsername()) {
                        navigateTo(Screen.UsernameSetup)
                    } else {
                        scope.launch {
                            if (app.pairingRepository.isPaired()) {
                                navigateTo(Screen.Home, clearStack = true)
                            } else {
                                navigateTo(Screen.Pairing)
                            }
                        }
                    }
                }
            )
        }
        Screen.UsernameSetup -> {
            UsernameSetupScreen(
                onUsernameCreated = {
                    navigateTo(Screen.Pairing)
                }
            )
        }
        Screen.Permissions -> {
            PermissionsSetupScreen(
                onContinue = {
                    if (backStack.size > 1) {
                        navigateBack()
                    } else {
                        scope.launch {
                            if (app.pairingRepository.isPaired()) {
                                navigateTo(Screen.Home, clearStack = true)
                            } else {
                                navigateTo(Screen.Pairing)
                            }
                        }
                    }
                },
                onBackClick = { navigateBack() }
            )
        }
        Screen.Onboarding -> {
            OnboardingScreen(
                onStartPairing = { navigateTo(Screen.Pairing) }
            )
        }
        Screen.Pairing -> {
            PairingScreen(
                onPairingSuccess = {
                    navigateTo(Screen.Home, clearStack = true)
                },
                onBackClick = { navigateBack() }
            )
        }
        Screen.Home -> {
            HomeScreen(
                onNavigateToChat = { navigateTo(Screen.Chat) },
                onNavigateToCall = { navigateTo(Screen.Call) },
                onNavigateToCallHistory = { navigateTo(Screen.CallHistory) },
                onNavigateToSettings = { navigateTo(Screen.Settings) },
                onNavigateToPairing = { navigateTo(Screen.Pairing) }
            )
        }
        Screen.Chat -> {
            ChatScreen(
                onBackClick = { navigateBack() },
                onNavigateToCall = { navigateTo(Screen.Call) }
            )
        }
        Screen.Call -> {
            CallScreen(
                onCallEnded = { navigateBack() }
            )
        }
        Screen.CallHistory -> {
            CallHistoryScreen(
                onBackClick = { navigateBack() }
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                onBackClick = { navigateBack() },
                onNavigateToDiagnostics = { navigateTo(Screen.Diagnostics) },
                onNavigateToAbout = { navigateTo(Screen.About) },
                onNavigateToPermissions = { navigateTo(Screen.Permissions) },
                onUnpaired = {
                    navigateTo(Screen.Login, clearStack = true)
                }
            )
        }
        Screen.Diagnostics -> {
            DiagnosticsScreen(
                onBackClick = { navigateBack() }
            )
        }
        Screen.About -> {
            AboutScreen(
                onBackClick = { navigateBack() }
            )
        }
        else -> {
            HomeScreen(
                onNavigateToChat = { navigateTo(Screen.Chat) },
                onNavigateToCall = { navigateTo(Screen.Call) },
                onNavigateToCallHistory = { navigateTo(Screen.CallHistory) },
                onNavigateToSettings = { navigateTo(Screen.Settings) },
                onNavigateToPairing = { navigateTo(Screen.Pairing) }
            )
        }
    }

    startupUpdate?.let { update ->
        UpdateDialog(
            updateInfo = update,
            onDismiss = { startupUpdate = null }
        )
    }
}
