package app.screentime.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.screentime.presentation.dashboard.DashboardRoute
import app.screentime.presentation.diagnostics.DiagnosticsRoute
import kotlinx.serialization.Serializable

@Serializable
data object DashboardDestination

@Serializable
data object DiagnosticsDestination

/** Navegação entre as telas do app (com o acesso ao uso já concedido). */
@Composable
fun AppNavHost(
    onOpenUsageSettings: () -> Unit,
    onPermissionLost: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = DashboardDestination) {
        composable<DashboardDestination> {
            DashboardRoute(
                viewModel = hiltViewModel(),
                onOpenDiagnostics = { navController.navigate(DiagnosticsDestination) },
                onPermissionLost = onPermissionLost,
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
