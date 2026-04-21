package com.savings.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.savings.tracker.ui.screens.AccountDetailScreen
import com.savings.tracker.ui.screens.HomeScreen
import com.savings.tracker.ui.screens.SettingsScreen
import com.savings.tracker.ui.theme.SavingsTrackerTheme
import com.savings.tracker.ui.viewmodel.SavingsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    // Launcher for the POST_NOTIFICATIONS permission dialog (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied — handled gracefully by the OS */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            SavingsTrackerTheme {
                SavingsTrackerApp(viewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// ── Navigation ─────────────────────────────────────────────────────────────────

@Composable
fun SavingsTrackerApp(viewModel: SavingsViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                viewModel      = viewModel,
                onAccountClick = { accountId -> navController.navigate("account/$accountId") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable(
            route     = "account/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.IntType })
        ) { backStack ->
            val accountId = backStack.arguments!!.getInt("accountId")
            AccountDetailScreen(
                accountId = accountId,
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
