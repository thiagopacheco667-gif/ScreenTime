package app.screentime.widget.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import app.screentime.domain.model.AccentColor
import app.screentime.domain.model.WidgetStyle
import app.screentime.widget.R
import app.screentime.widget.data.WidgetData
import app.screentime.widget.data.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.glance.unit.ColorProvider as GlanceColor

private fun fixed(c: Color): GlanceColor = ColorProvider(day = c, night = c)

/** Cores de um estilo de widget (Ajustes › Aparência › Widgets). */
internal class WidgetPalette(
    @param:DrawableRes val background: Int?,
    text: Color,
    muted: Color,
    accent: Color,
    track: Color,
    divider: Color,
    better: Color,
    worse: Color,
    val bar: Int,
    val barNow: Int,
    val grid: Int,
    val label: Int,
) {
    val text: GlanceColor = fixed(text)
    val muted: GlanceColor = fixed(muted)
    val accent: GlanceColor = fixed(accent)
    val track: GlanceColor = fixed(track)
    val divider: GlanceColor = fixed(divider)
    val better: GlanceColor = fixed(better)
    val worse: GlanceColor = fixed(worse)
    val tile: GlanceColor = fixed(Color(0x1FFFFFFF))
    val betterBg: GlanceColor = fixed(better.copy(alpha = 0.30f))
    val worseBg: GlanceColor = fixed(worse.copy(alpha = 0.30f))

    fun style(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: GlanceColor = text) =
        TextStyle(color = color, fontSize = size, fontWeight = weight)
}

private fun accentOnDark(a: AccentColor) = when (a) {
    AccentColor.Gold -> Color(0xFFE3C896)
    AccentColor.Blue -> Color(0xFF8FB4FF)
    AccentColor.Green -> Color(0xFF8FE0C7)
    AccentColor.Coral -> Color(0xFFF2A27C)
    AccentColor.Lilac -> Color(0xFFC9B6F2)
}

private fun accentOnLight(a: AccentColor) = when (a) {
    AccentColor.Gold -> Color(0xFF8A6A2E)
    AccentColor.Blue -> Color(0xFF2F5DC8)
    AccentColor.Green -> Color(0xFF1F7F66)
    AccentColor.Coral -> Color(0xFFB4532A)
    AccentColor.Lilac -> Color(0xFF6B4FB8)
}

/** Paleta do estilo efetivo do widget. "Imagem" usa a do transparente (a imagem já vem escurecida). */
internal fun paletteFor(style: WidgetStyle, accent: AccentColor): WidgetPalette = when (style) {
    WidgetStyle.Navy -> WidgetPalette(
        background = R.drawable.widget_dark_bg,
        text = Color(0xFFF2F4FF), muted = Color(0xFFA9B0D6), accent = accentOnDark(accent),
        track = Color(0x2EFFFFFF), divider = Color(0x26FFFFFF),
        better = Color(0xFF8FE0C7), worse = Color(0xFFF2A27C),
        bar = Color(0xFFB7BCEB).toArgb(), barNow = Color.White.toArgb(),
        grid = Color(0x2EFFFFFF).toArgb(), label = Color(0xFFA9B0D6).toArgb(),
    )
    WidgetStyle.Light -> WidgetPalette(
        background = R.drawable.widget_light_bg,
        text = Color(0xFF15181D), muted = Color(0xFF5D6470), accent = accentOnLight(accent),
        track = Color(0xFFE6E8F0), divider = Color(0xFFE1E4EC),
        better = Color(0xFF1F7F66), worse = Color(0xFFB4532A),
        bar = Color(0xFFC7CBE8).toArgb(), barNow = accentOnLight(accent).toArgb(),
        grid = Color(0xFFE3E5EE).toArgb(), label = Color(0xFF5D6470).toArgb(),
    )
    WidgetStyle.Transparent, WidgetStyle.Image -> WidgetPalette(
        background = if (style == WidgetStyle.Transparent) R.drawable.widget_transparent_bg else null,
        text = Color.White, muted = Color(0xFFE2E4F2), accent = accentOnDark(accent),
        track = Color(0x40FFFFFF), divider = Color(0x33FFFFFF),
        better = Color(0xFF9BEBD2), worse = Color(0xFFF7B593),
        bar = Color(0xB3FFFFFF).toArgb(), barNow = Color.White.toArgb(),
        grid = Color(0x33FFFFFF).toArgb(), label = Color(0xFFE2E4F2).toArgb(),
    )
}

/** Mesmo formato do app: 45 s · 12 min · 2 h · 2 h 05 min */
internal fun formatDuration(millis: Long): String {
    if (millis < 60_000L) return "${(millis / 1000L).coerceAtLeast(0)} s"
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "$minutes min"
        minutes == 0L -> "$hours h"
        else -> "$hours h ${minutes.toString().padStart(2, '0')} min"
    }
}

/** Versão curta para listas estreitas: 36 min · 1 h 24 */
internal fun formatShort(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        millis < 60_000L -> "${(millis / 1000L).coerceAtLeast(0)} s"
        hours == 0L -> "$minutes min"
        else -> "$hours h ${minutes.toString().padStart(2, '0')}"
    }
}

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun formatClock(instant: Instant, zone: ZoneId): String = HOUR_MINUTE.format(instant.atZone(zone))

private fun entryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

internal suspend fun loadWidgetData(context: Context): WidgetData =
    entryPoint(context).widgetDataSource().load()

internal suspend fun runSync(context: Context) {
    entryPoint(context).syncUsage().invoke()
}

/** Abre o app (tela inicial ou explicação da permissão, conforme o caso). */
internal fun launchIntent(context: Context): Intent =
    (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

/**
 * Abre o app direto nos Ajustes. O endereço (data) diferente faz o Android tratar
 * este atalho como distinto do toque no resto do widget.
 */
internal fun settingsIntent(context: Context): Intent =
    launchIntent(context).setData(android.net.Uri.parse("screentime://settings"))

/** Texto do selo de comparação: curto para caber em qualquer widget. */
internal fun comparisonText(percent: Int, long: Boolean, time: String): String = when {
    percent < 0 -> if (long) "▼ ${-percent}% vs ontem até $time" else "▼ ${-percent}% vs ontem"
    percent > 0 -> if (long) "▲ $percent% vs ontem até $time" else "▲ $percent% vs ontem"
    else -> if (long) "= ontem até $time" else "= ontem"
}
