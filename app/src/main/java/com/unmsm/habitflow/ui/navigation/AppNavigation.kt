package com.unmsm.habitflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unmsm.habitflow.ui.screens.*

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Onboarding : Route("onboarding")
    data object Login : Route("login")
    data object Register : Route("register")
    data object Recover : Route("recover")
    data object VerifyEmail : Route("verify_email")
    data object Home : Route("home")
    data object Stats : Route("stats")
    data object History : Route("history")
    data object Notifications : Route("notifications")
    data object Profile : Route("profile")
    data object EditProfile : Route("edit_profile")
    data object Settings : Route("settings")
    data object Achievements : Route("achievements")
    data object DeleteAccount : Route("delete_account")
    data object Voice : Route("voice")
    data object ManualHabit : Route("manual_habit")
    data object HabitDetail : Route("habit/{habitId}") {
        fun create(habitId: String) = "habit/$habitId"
    }
}

private data class BottomItem(val route: Route, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Route.Home, "Inicio", Icons.Default.Home),
    BottomItem(Route.Stats, "Stats", Icons.Default.BarChart),
    BottomItem(Route.History, "Historial", Icons.Default.History),
    BottomItem(Route.Profile, "Perfil", Icons.Default.Person),
    BottomItem(Route.Settings, "Ajustes", Icons.Default.Settings)
)

@Composable
fun HabitFlowApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBottom = bottomItems.any { item -> current?.hierarchy?.any { it.route == item.route.path } == true }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == item.route.path } == true,
                            onClick = { navController.navigateBottom(item.route.path) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Route.Splash.path) {
            composable(Route.Splash.path) { SplashScreen(padding) { navController.navigate(Route.Onboarding.path) } }
            composable(Route.Onboarding.path) { OnboardingScreen(padding, onFinish = { navController.navigate(Route.Login.path) }) }
            composable(Route.Login.path) {
                LoginScreen(
                    padding = padding,
                    onLogin = { navController.navigate(Route.Home.path) },
                    onRegister = { navController.navigate(Route.Register.path) },
                    onRecover = { navController.navigate(Route.Recover.path) }
                )
            }
            composable(Route.Register.path) { RegisterScreen(padding, onDone = { navController.navigate(Route.VerifyEmail.path) }) }
            composable(Route.Recover.path) { RecoverPasswordScreen(padding) { navController.popBackStack() } }
            composable(Route.VerifyEmail.path) { VerifyEmailScreen(padding) { navController.navigate(Route.Home.path) } }
            composable(Route.Home.path) {
                HomeScreen(
                    padding = padding,
                    onHabit = { navController.navigate(Route.HabitDetail.create(it)) },
                    onVoice = { navController.navigate(Route.Voice.path) },
                    onManual = { navController.navigate(Route.ManualHabit.path) },
                    onNotifications = { navController.navigate(Route.Notifications.path) }
                )
            }
            composable(Route.HabitDetail.path) { HabitDetailScreen(padding) }
            composable(Route.Stats.path) { StatsScreen(padding) }
            composable(Route.History.path) { HistoryScreen(padding) }
            composable(Route.Notifications.path) { NotificationsScreen(padding) }
            composable(Route.Profile.path) {
                ProfileScreen(
                    padding = padding,
                    onEdit = { navController.navigate(Route.EditProfile.path) },
                    onAchievements = { navController.navigate(Route.Achievements.path) }
                )
            }
            composable(Route.EditProfile.path) { EditProfileScreen(padding) { navController.popBackStack() } }
            composable(Route.Settings.path) {
                SettingsScreen(
                    padding = padding,
                    onDelete = { navController.navigate(Route.DeleteAccount.path) },
                    onLogout = {
                        navController.navigate(Route.Login.path) {
                            popUpTo(Route.Home.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Route.Achievements.path) { AchievementsScreen(padding) }
            composable(Route.DeleteAccount.path) { DeleteAccountScreen(padding) { navController.popBackStack(Route.Login.path, false) } }
            composable(Route.Voice.path) { VoiceScreen(padding, onManual = { navController.navigate(Route.ManualHabit.path) }) }
            composable(Route.ManualHabit.path) { ManualHabitScreen(padding, onDone = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateBottom(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
