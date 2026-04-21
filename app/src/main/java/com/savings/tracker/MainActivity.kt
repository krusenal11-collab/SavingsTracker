package com.savings.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifPermIfNeeded()
        window.statusBarColor = Color(0xFF1B3A6B).toArgb()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            SavingsTrackerTheme { SavingsTrackerApp(viewModel) }
        }
    }

    private fun requestNotifPermIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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

    // Fix #1/#28/#33: 4 tabs — Home, Insights, Goals, Tithe. Settings is NOT a tab.
    val tabRoutes = setOf("home", "summary", "goals", "tithe")
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = { if (showBottomBar) AppBottomNavBar(currentRoute, navController) },
        containerColor = AppColors.SystemBg
    ) { padding ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {

            composable("home") {
                HomeScreen(
                    viewModel           = viewModel,
                    onAccountClick      = { id -> navController.navigate("account/$id") },
                    onTotalSavingsClick = { navController.navigate("summary") },
                    // Fix #1: Settings via gear icon → push navigation
                    onSettingsClick     = { navController.navigate("settings") }
                )
            }

            composable("summary") {
                val canGoBack = navController.previousBackStackEntry != null
                SummaryScreen(viewModel = viewModel, onBack = if (canGoBack) { { navController.popBackStack() } } else null)
            }

            // Fix #10/#28: Goals and Tithe routes now registered
            composable("goals") {
                GoalsScreen(viewModel = viewModel)
            }

            composable("tithe") {
                TitheScreen(viewModel = viewModel)
            }

            // Fix #1/#12: Settings is push navigation — shows bottom bar hidden, has back button
            composable("settings") {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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

data class TabItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun AppBottomNavBar(currentRoute: String?, navController: NavController) {
    val tabs = listOf(
        TabItem("home",    Icons.Default.Home,       "Home"),
        TabItem("summary", Icons.Default.TrendingUp, "Insights"),
        TabItem("goals",   Icons.Default.Star,       "Goals"),
        TabItem("tithe",   Icons.Default.Favorite,   "Tithe")
    )

    NavigationBar(containerColor = Color(0xFFF9F9F9), tonalElevation = 0.dp) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = AppColors.Primary,
            selectedTextColor   = AppColors.Primary,
            unselectedIconColor = AppColors.LabelSecondary,
            unselectedTextColor = AppColors.LabelSecondary,
            indicatorColor      = AppColors.Primary.copy(alpha = 0.10f)
        )
        tabs.forEach { tab ->
            NavigationBarItem(
                icon     = { Icon(tab.icon, contentDescription = tab.label) },
                label    = { Text(tab.label) },
                selected = currentRoute == tab.route,
                onClick  = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                colors = colors
            )
        }
    }
}
