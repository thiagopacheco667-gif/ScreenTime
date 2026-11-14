package app.screentime.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.screentime.domain.model.FontChoice
import app.screentime.domain.model.ThemeMode
import app.screentime.presentation.appearance.AppearanceViewModel
import app.screentime.presentation.appearance.toUi
import app.screentime.ui.theme.ScreenTimeTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.permissions.UsageAccessSettings
import app.screentime.presentation.navigation.AppNavHost
import app.screentime.presentation.permission.PermissionScreen
import app.screentime.presentation.permission.PermissionViewModel

/**
 * Ponto de entrada das telas. Decide entre a explicação da permissão e o app.
 * A checagem roda de novo a cada retorno ao app (ON_RESUME), então conceder ou
 * revogar o acesso nas configurações muda a tela sozinho.
 */
@Composable
fun ScreenTimeRoot(
    openSettingsRequest: Int = 0,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    permissionViewModel: PermissionViewModel = hiltViewModel(),
) {
    val appearance by appearanceViewModel.settings.collectAsStateWithLifecycle()
    val dark = when (appearance.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemBarsAppearance(dark)
    ScreenTimeTheme(
        darkTheme = dark,
        accent = appearance.accent.toUi(),
        useAppFont = appearance.font == FontChoice.App,
        animations = appearance.animations,
    ) {
        RootContent(openSettingsRequest, permissionViewModel)
    }
}

@Composable
private fun RootContent(
    openSettingsRequest: Int,
    permissionViewModel: PermissionViewModel,
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
            AppNavHost(
                openSettingsRequest = openSettingsRequest,
                onOpenUsageSettings = { UsageAccessSettings.open(context) },
                onPermissionLost = permissionViewModel::refresh,
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

/** Ícones da barra de status e de navegação claros no tema escuro e escuros no claro. */
@Composable
private fun SystemBarsAppearance(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
