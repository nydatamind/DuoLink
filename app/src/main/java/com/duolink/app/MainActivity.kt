package com.duolink.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }

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

    fun navigateTo(screen: Screen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    fun navigateBack() {
        currentScreen = previousScreen ?: Screen.Home
    }

    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onNavigateToHome = { navigateTo(Screen.Home) },
                onNavigateToOnboarding = {
                    if (app.authManager.isAuthenticated()) {
                        if (!app.authManager.hasUsername()) {
                            navigateTo(Screen.UsernameSetup)
                        } else {
                            navigateTo(Screen.Permissions)
                        }
                    } else {
                        navigateTo(Screen.Login)
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
                        navigateTo(Screen.Permissions)
                    }
                }
            )
        }
        Screen.UsernameSetup -> {
            UsernameSetupScreen(
                onUsernameCreated = {
                    navigateTo(Screen.Permissions)
                }
            )
        }
        Screen.Permissions -> {
            PermissionsSetupScreen(
                onContinue = {
                    scope.launch {
                        if (app.pairingRepository.isPaired()) {
                            navigateTo(Screen.Home)
                        } else {
                            navigateTo(Screen.Pairing)
                        }
                    }
                },
                onBackClick = if (previousScreen != null && previousScreen != Screen.Login && previousScreen != Screen.Splash) { { navigateBack() } } else null
            )
        }
        Screen.Onboarding -> {
            OnboardingScreen(
                onStartPairing = { navigateTo(Screen.Pairing) }
            )
        }
        Screen.Pairing -> {
            PairingScreen(
                onPairingSuccess = { navigateTo(Screen.Home) },
                onBackClick = if (previousScreen != null) { { navigateBack() } } else null
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
                onBackClick = { navigateTo(Screen.Home) },
                onNavigateToCall = { navigateTo(Screen.Call) }
            )
        }
        Screen.Call -> {
            CallScreen(
                onCallEnded = { navigateTo(Screen.Home) }
            )
        }
        Screen.CallHistory -> {
            CallHistoryScreen(
                onBackClick = { navigateTo(Screen.Home) }
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                onBackClick = { navigateTo(Screen.Home) },
                onNavigateToDiagnostics = { navigateTo(Screen.Diagnostics) },
                onNavigateToAbout = { navigateTo(Screen.About) },
                onNavigateToPermissions = { navigateTo(Screen.Permissions) },
                onUnpaired = { navigateTo(Screen.Onboarding) }
            )
        }
        Screen.Diagnostics -> {
            DiagnosticsScreen(
                onBackClick = { navigateTo(Screen.Settings) }
            )
        }
        Screen.About -> {
            AboutScreen(
                onBackClick = { navigateTo(Screen.Settings) }
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
