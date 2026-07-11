package com.unmsm.habitflow.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unmsm.habitflow.R
import com.unmsm.habitflow.ui.screens.*
import com.unmsm.habitflow.ui.components.HabitFlowNavigationBar
import com.unmsm.habitflow.ui.components.HabitFlowNavigationItem
import com.unmsm.habitflow.ui.viewmodel.SessionViewModel

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Onboarding : Route("onboarding")
    data object Login : Route("login")
    data object ProfileSetup : Route("profile_setup")
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
    BottomItem(Route.Stats, "Progreso", Icons.Default.BarChart),
    BottomItem(Route.History, "Historial", Icons.Default.History),
    BottomItem(Route.Profile, "Perfil", Icons.Default.Person)
)

@Composable
fun HabitFlowApp() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBottom = bottomItems.any { item -> current?.hierarchy?.any { it.route == item.route.path } == true }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showBottom) {
                FloatingActionButton(
                    onClick = { navController.navigate(Route.Voice.path) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = stringResource(R.string.open_voice_assistant)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottom) {
                HabitFlowNavigationBar(
                    items = bottomItems.map { HabitFlowNavigationItem(it.route.path, it.label, it.icon) },
                    selectedRoute = bottomItems.firstOrNull { item -> current?.hierarchy?.any { it.route == item.route.path } == true }?.route?.path,
                    onSelected = { route -> navController.navigateBottom(route) }
                )
            }
        }
    ) { innerPadding ->
        val screenPadding = PaddingValues(0.dp)
        NavHost(
            navController = navController,
            startDestination = Route.Splash.path,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Route.Splash.path) {
                SplashScreen(screenPadding) {
                    val startRoute = when {
                        !sessionViewModel.isLoggedIn() -> Route.Onboarding.path
                        sessionViewModel.needsProfileSetup() -> Route.ProfileSetup.path
                        else -> Route.Home.path
                    }
                    navController.navigateClearingBackStack(startRoute)
                }
            }
            composable(Route.Onboarding.path) { OnboardingScreen(screenPadding, onFinish = { navController.navigate(Route.Login.path) }) }
            composable(Route.Login.path) {
                LoginScreen(
                    padding = screenPadding,
                    onLogin = { navController.navigateClearingBackStack(Route.Home.path) },
                    onProfileSetup = { navController.navigateClearingBackStack(Route.ProfileSetup.path) },
                    onRegister = { navController.navigate(Route.Register.path) },
                    onRecover = { navController.navigate(Route.Recover.path) }
                )
            }
            composable(Route.ProfileSetup.path) { ProfileSetupScreen(screenPadding, onDone = { navController.navigateClearingBackStack(Route.Home.path) }) }
            composable(Route.Register.path) { RegisterScreen(screenPadding, onDone = { navController.navigate(Route.VerifyEmail.path) }) }
            composable(Route.Recover.path) { RecoverPasswordScreen(screenPadding) { navController.popBackStack() } }
            composable(Route.VerifyEmail.path) { VerifyEmailScreen(screenPadding) { navController.navigateClearingBackStack(Route.Home.path) } }
            composable(Route.Home.path) {
                HomeScreen(
                    padding = screenPadding,
                    onHabit = { navController.navigate(Route.HabitDetail.create(it)) },
                    onVoice = { navController.navigate(Route.Voice.path) },
                    onManual = { navController.navigate(Route.ManualHabit.path) },
                    onNotifications = { navController.navigate(Route.Notifications.path) }
                )
            }
            composable(Route.HabitDetail.path) { HabitDetailScreen(screenPadding) }
            composable(Route.Stats.path) { StatsScreen(screenPadding) }
            composable(Route.History.path) { HistoryScreen(screenPadding) }
            composable(Route.Notifications.path) { NotificationsScreen(screenPadding) }
            composable(Route.Profile.path) {
                ProfileScreen(
                    padding = screenPadding,
                    onEdit = { navController.navigate(Route.EditProfile.path) },
                    onAchievements = { navController.navigate(Route.Achievements.path) },
                    onSettings = { navController.navigate(Route.Settings.path) }
                )
            }
            composable(Route.EditProfile.path) {
                EditProfileScreen(padding = screenPadding, onDone = { navController.popBackStack() })
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    padding = screenPadding,
                    onDelete = { navController.navigate(Route.DeleteAccount.path) },
                    onLogout = {
                        navController.navigateClearingBackStack(Route.Login.path)
                    }
                )
            }
            composable(Route.Achievements.path) { AchievementsScreen(screenPadding) }
            composable(Route.DeleteAccount.path) { DeleteAccountScreen(screenPadding) { navController.popBackStack(Route.Login.path, false) } }
            composable(Route.Voice.path) { VoiceScreen(screenPadding, onManual = { navController.navigate(Route.ManualHabit.path) }) }
            composable(Route.ManualHabit.path) { ManualHabitScreen(screenPadding, onDone = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBottom(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
