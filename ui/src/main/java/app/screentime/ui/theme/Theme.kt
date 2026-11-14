package app.screentime.ui.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val ScreenTimeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalScreenTimeColors = staticCompositionLocalOf { DarkExtended }

/** false quando o usuário desligou as animações no app ou no Android. */
val LocalMotionEnabled = staticCompositionLocalOf { true }

/** Atalhos: `ScreenTimeTheme.colors.heroStart`, `ScreenTimeTheme.motion`. */
object ScreenTimeTheme {
    val colors: ScreenTimeColors
        @Composable get() = LocalScreenTimeColors.current
    val motion: Boolean
        @Composable get() = LocalMotionEnabled.current
}

/**
 * Tema do app, montado a partir de Ajustes › Aparência.
 *
 * @param darkTheme já resolvido (Sistema/Claro/Escuro) por quem chama.
 * @param useAppFont true = Manrope; false = fonte do sistema.
 * @param animations preferência do app; o Android com animações desligadas também desliga.
 */
@Composable
fun ScreenTimeTheme(
    darkTheme: Boolean = true,
    accent: Accent = Accent.Gold,
    useAppFont: Boolean = true,
    animations: Boolean = true,
    content: @Composable () -> Unit,
) {
    val accentColor = if (darkTheme) accent.onDark else accent.onLight
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = NavyBackground,
            primaryContainer = DarkExtended.navIndicator,
            onPrimaryContainer = NavyText,
            secondary = DarkExtended.chartBar,
            background = NavyBackground,
            onBackground = NavyText,
            surface = NavySurface,
            onSurface = NavyText,
            surfaceVariant = NavySurfaceHigh,
            onSurfaceVariant = NavyMuted,
            surfaceContainerLowest = NavyBackground,
            surfaceContainerLow = NavySurface,
            surfaceContainer = NavySurface,
            surfaceContainerHigh = NavySurfaceHigh,
            surfaceContainerHighest = NavySurfaceHigh,
            outline = NavyOutline,
            outlineVariant = DarkExtended.divider,
            error = ErrorDark,
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = LightExtended.navIndicator,
            onPrimaryContainer = LightText,
            secondary = LightExtended.chartBar,
            background = LightBackground,
            onBackground = LightText,
            surface = LightSurface,
            onSurface = LightText,
            surfaceVariant = LightSurfaceHigh,
            onSurfaceVariant = LightMuted,
            surfaceContainerLowest = LightSurface,
            surfaceContainerLow = LightSurface,
            surfaceContainer = LightBackground,
            surfaceContainerHigh = LightSurfaceHigh,
            surfaceContainerHighest = LightSurfaceHigh,
            outline = LightOutline,
            outlineVariant = LightExtended.divider,
            error = ErrorLight,
        )
    }
    val family = if (useAppFont) ManropeFamily else FontFamily.Default
    val typography = remember(family) { screenTimeTypography(family) }
    val systemMotion = systemAnimationsEnabled()

    CompositionLocalProvider(
        LocalScreenTimeColors provides if (darkTheme) DarkExtended else LightExtended,
        LocalMotionEnabled provides (animations && systemMotion),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = ScreenTimeShapes,
            content = content,
        )
    }
}

/** Respeita "Remover animações" / escala de animação 0 do Android. */
@Composable
private fun systemAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
