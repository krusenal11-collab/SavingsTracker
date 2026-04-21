package com.savings.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.savings.tracker.ui.screens.*
import com.savings.tracker.ui.theme.AppColors
import com.savings.tracker.ui.theme.SavingsTrackerTheme
import com.savings.tracker.ui.viewmodel.SavingsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifPermIfNeeded()
        setContent {
            SavingsTrackerTheme {
                SavingsTrackerApp(viewModel)
            }
        }
    }

    private fun requestNotifPermIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun SavingsTrackerApp(viewModel: SavingsViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    // Show bottom bar only on the three top-level tabs
    val topLevelRoutes = setOf("home", "summary", "settings")
    val showBottomBar  = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavBar(currentRoute = currentRoute, navController = navController)
            }
        },
        containerColor = AppColors.SystemBg
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = "home",
            modifier         = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel           = viewModel,
                    onAccountClick      = { id -> navController.navigate("account/$id") },
                    onTotalSavingsClick = {
                        navController.navigate("summary") {
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
            composable("summary") {
                SummaryScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                route     = "account/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) { back ->
                AccountDetailScreen(
                    accountId = back.arguments!!.getInt("accountId"),
                    viewModel = viewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavBar(currentRoute: String?, navController: NavController) {
    NavigationBar(
        containerColor = Color(0xFFF9F9F9),
        tonalElevation = 0.dp
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor   = AppColors.Primary,
            selectedTextColor   = AppColors.Primary,
            unselectedIconColor = AppColors.LabelSecondary,
            unselectedTextColor = AppColors.LabelSecondary,
            indicatorColor      = AppColors.Primary.copy(alpha = 0.10f)
        )

        NavigationBarItem(
            icon     = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label    = { Text("Home") },
            selected = currentRoute == "home",
            onClick  = {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            },
            colors = navItemColors
        )
        NavigationBarItem(
            icon     = { Icon(Icons.Default.TrendingUp, contentDescription = "Insights") },
            label    = { Text("Insights") },
            selected = currentRoute == "summary",
            onClick  = {
                navController.navigate("summary") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            },
            colors = navItemColors
        )
        NavigationBarItem(
            icon     = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label    = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick  = {
                navController.navigate("settings") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            },
            colors = navItemColors
        )
    }
}
