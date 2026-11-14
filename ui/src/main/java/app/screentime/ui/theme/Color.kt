package app.screentime.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** As cinco cores de destaque de Ajustes › Aparência. */
enum class Accent(
    /** Versão para fundo escuro (marinho). */
    val onDark: Color,
    /** Versão mais escura, com contraste suficiente em fundo claro. */
    val onLight: Color,
) {
    Gold(Color(0xFFE3C896), Color(0xFF8A6A2E)),
    Blue(Color(0xFF8FB4FF), Color(0xFF2F5DC8)),
    Green(Color(0xFF8FE0C7), Color(0xFF1F7F66)),
    Coral(Color(0xFFF2A27C), Color(0xFFB4532A)),
    Lilac(Color(0xFFC9B6F2), Color(0xFF6B4FB8)),
}

/**
 * Cores além do Material: o cartão de destaque, os gráficos e a comparação.
 * Valores da maquete aprovada (marinho e dourado).
 */
@Immutable
data class ScreenTimeColors(
    val heroStart: Color,
    val heroEnd: Color,
    val heroBorder: Color,
    val card: Color,
    val cardBorder: Color,
    val inset: Color,
    val divider: Color,
    val track: Color,
    val chartBar: Color,
    val chartBarNow: Color,
    val chartGrid: Color,
    val chartAxis: Color,
    val chartGhost: Color,
    /** Menos uso que ontem (bom). */
    val better: Color,
    val betterContainer: Color,
    /** Mais uso que ontem. */
    val worse: Color,
    val worseContainer: Color,
    val navBar: Color,
    val navIndicator: Color,
    /** Fundos das iniciais quando o app não tem ícone. */
    val tiles: List<Color>,
    val isDark: Boolean,
)

internal val DarkExtended = ScreenTimeColors(
    heroStart = Color(0xFF1E2547),
    heroEnd = Color(0xFF151A33),
    heroBorder = Color(0xFF2A3156),
    card = Color(0xFF161A2C),
    cardBorder = Color(0xFF252B47),
    inset = Color(0xFF1E2338),
    divider = Color(0xFF1F2440),
    track = Color(0xFF262C47),
    chartBar = Color(0xFFAEB4EC),
    chartBarNow = Color(0xFFD9DCFA),
    chartGrid = Color(0xFF2A3050),
    chartAxis = Color(0xFF343B5E),
    chartGhost = Color(0xFF8C93B5),
    better = Color(0xFF8FE0C7),
    betterContainer = Color(0xFF12332E),
    worse = Color(0xFFF2A27C),
    worseContainer = Color(0xFF3A2420),
    navBar = Color(0xFF12152A),
    navIndicator = Color(0xFF2A3156),
    tiles = listOf(Color(0xFFE3C896), Color(0xFFF2A27C), Color(0xFF8FE0C7), Color(0xFFF28B8B), Color(0xFF8FB4FF), Color(0xFFC9B6F2)),
    isDark = true,
)

internal val LightExtended = ScreenTimeColors(
    heroStart = Color(0xFFFFFFFF),
    heroEnd = Color(0xFFEEF0F8),
    heroBorder = Color(0xFFDDE0EA),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE1E4EC),
    inset = Color(0xFFF1F2F7),
    divider = Color(0xFFE8EAF0),
    track = Color(0xFFE6E8F0),
    chartBar = Color(0xFF9EA5D8),
    chartBarNow = Color(0xFF5E66A8),
    chartGrid = Color(0xFFE3E5EE),
    chartAxis = Color(0xFFCDD1DE),
    chartGhost = Color(0xFF8A90A8),
    better = Color(0xFF1F7F66),
    betterContainer = Color(0xFFDDF3EC),
    worse = Color(0xFFB4532A),
    worseContainer = Color(0xFFFBE7DD),
    navBar = Color(0xFFFFFFFF),
    navIndicator = Color(0xFFE7EAF6),
    tiles = listOf(Color(0xFFE3C896), Color(0xFFF2A27C), Color(0xFF8FE0C7), Color(0xFFF28B8B), Color(0xFF8FB4FF), Color(0xFFC9B6F2)),
    isDark = false,
)

// Base do esquema Material.
internal val NavyBackground = Color(0xFF0E1120)
internal val NavySurface = Color(0xFF161A2C)
internal val NavySurfaceHigh = Color(0xFF1E2338)
internal val NavyText = Color(0xFFEEF0FA)
internal val NavyMuted = Color(0xFF9BA2C4)
internal val NavyOutline = Color(0xFF252B47)

internal val LightBackground = Color(0xFFF4F5F8)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceHigh = Color(0xFFECEEF2)
internal val LightText = Color(0xFF15181D)
internal val LightMuted = Color(0xFF5D6470)
internal val LightOutline = Color(0xFFDDE0E6)

internal val ErrorLight = Color(0xFFB3261E)
internal val ErrorDark = Color(0xFFF2B8B5)
