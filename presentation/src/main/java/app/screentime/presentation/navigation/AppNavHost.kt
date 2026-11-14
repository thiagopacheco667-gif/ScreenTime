package app.screentime.presentation.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.screentime.presentation.R
import app.screentime.presentation.appearance.AppearanceRoute
import app.screentime.presentation.apps.AppsRoute
import app.screentime.presentation.apps.DayAppsRoute
import app.screentime.presentation.dashboard.DashboardRoute
import app.screentime.presentation.detail.AppDetailRoute
import app.screentime.presentation.diagnostics.DiagnosticsRoute
import app.screentime.presentation.settings.SettingsRoute
import app.screentime.presentation.week.WeekRoute
import app.screentime.ui.icons.ScreenTimeIcons
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.reflect.KClass

@Serializable
data object DashboardDestination

@Serializable
data object AppsDestination

@Serializable
data object WeekDestination

@Serializable
data object DiagnosticsDestination

@Serializable
data object SettingsDestination

@Serializable
data object AppearanceDestination

/** Apps de um dia ([epochDay] = LocalDate.toEpochDay()). */
@Serializable
data class DayAppsDestination(val epochDay: Long)

/** Detalhe de um app em um dia ([epochDay] = LocalDate.toEpochDay()). */
@Serializable
data class AppDetailDestination(val packageName: String, val epochDay: Long)

private data class TopLevel(val route: Any, val type: KClass<*>, val label: Int, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(DashboardDestination, DashboardDestination::class, R.string.nav_today, ScreenTimeIcons.Today),
    TopLevel(AppsDestination, AppsDestination::class, R.string.nav_apps, ScreenTimeIcons.Apps),
    TopLevel(WeekDestination, WeekDestination::class, R.string.nav_week, ScreenTimeIcons.Week),
)

/** Navegação entre as telas do app (com o acesso ao uso já concedido). */
@Composable
fun AppNavHost(
    onOpenUsageSettings: () -> Unit,
    onPermissionLost: () -> Unit,
    openSettingsRequest: Int = 0,
) {
    val navController = rememberNavController()

    // Engrenagem do widget: abre os Ajustes por cima do painel.
    LaunchedEffect(openSettingsRequest) {
        if (openSettingsRequest > 0) {
            navController.navigate(SettingsDestination) { launchSingleTop = true }
        }
    }
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = topLevels.any { destination?.hasRoute(it.type) == true }

    val openApp: (String, LocalDate) -> Unit = { pkg, date ->
        navController.navigate(AppDetailDestination(pkg, date.toEpochDay()))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    topLevels.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(item.type) == true,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = DashboardDestination,
            modifier = Modifier
                .padding(inner)
                .consumeWindowInsets(inner),
        ) {
            composable<DashboardDestination> {
                DashboardRoute(
                    viewModel = hiltViewModel(),
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                    onOpenApp = openApp,
                    onPermissionLost = onPermissionLost,
                )
            }
            composable<AppsDestination> {
                AppsRoute(
                    viewModel = hiltViewModel(),
                    onOpenApp = openApp,
                    onPermissionLost = onPermissionLost,
                )
            }
            composable<WeekDestination> {
                WeekRoute(
                    viewModel = hiltViewModel(),
                    onOpenDay = { date -> navController.navigate(DayAppsDestination(date.toEpochDay())) },
                    onOpenApp = openApp,
                    onPermissionLost = onPermissionLost,
                )
            }
            composable<DayAppsDestination> {
                DayAppsRoute(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                    onOpenApp = openApp,
                    onPermissionLost = onPermissionLost,
                )
            }
            composable<AppDetailDestination> {
                AppDetailRoute(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                    onPermissionLost = onPermissionLost,
                )
            }
            composable<SettingsDestination> {
                SettingsRoute(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                    onOpenDiagnostics = { navController.navigate(DiagnosticsDestination) },
                    onOpenAppearance = { navController.navigate(AppearanceDestination) },
                )
            }
            composable<AppearanceDestination> {
                AppearanceRoute(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable<DiagnosticsDestination> {
                DiagnosticsRoute(
                    viewModel = hiltViewModel(),
                    onOpenSettings = onOpenUsageSettings,
                    onPermissionLost = onPermissionLost,
                )
            }
        }
    }
}

/** Troca de aba sem empilhar telas e mantendo o estado de cada aba. */
private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
