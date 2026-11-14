package app.screentime.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.permissions.UsageAccessSettings
import app.screentime.presentation.permission.AccessGrantedScreen
import app.screentime.presentation.permission.PermissionScreen
import app.screentime.presentation.permission.PermissionViewModel

/**
 * Ponto de entrada das telas. Decide entre a explicação da permissão e o app.
 * A checagem roda de novo a cada retorno ao app (ON_RESUME), então conceder ou
 * revogar o acesso nas configurações muda a tela sozinho.
 */
@Composable
fun ScreenTimeRoot(
    permissionViewModel: PermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val hasAccess by permissionViewModel.hasAccess.collectAsStateWithLifecycle()
    var settingsFailed by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(permissionViewModel) {
        permissionViewModel.refresh()
        onPauseOrDispose { }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (hasAccess) {
            AccessGrantedScreen(
                onOpenSettings = { UsageAccessSettings.open(context) },
            )
        } else {
            PermissionScreen(
                settingsFailed = settingsFailed,
                onOpenSettings = { settingsFailed = !UsageAccessSettings.open(context) },
                onOpenAppInfo = { UsageAccessSettings.openAppDetails(context) },
            )
        }
    }
}
