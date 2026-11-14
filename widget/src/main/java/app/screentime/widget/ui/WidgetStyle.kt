package app.screentime.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import app.screentime.widget.data.WidgetData
import app.screentime.widget.data.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Mesma paleta do app, com versão clara e escura (segue o modo escuro do sistema). */
internal object WidgetColors {
    val background = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF171A1F))
    val onSurface = ColorProvider(day = Color(0xFF15181D), night = Color(0xFFE8EAEE))
    val muted = ColorProvider(day = Color(0xFF5D6470), night = Color(0xFF9AA1AD))
    val accent = ColorProvider(day = Color(0xFF2B5FD9), night = Color(0xFF7EA2FF))
    val track = ColorProvider(day = Color(0xFFECEEF2), night = Color(0xFF2A2F37))
}

internal object WidgetText {
    val label = TextStyle(color = WidgetColors.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val totalLarge = TextStyle(color = WidgetColors.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Normal)
    val totalMedium = TextStyle(color = WidgetColors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Normal)
    val body = TextStyle(color = WidgetColors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val bodyMuted = TextStyle(color = WidgetColors.muted, fontSize = 12.sp)
    val small = TextStyle(color = WidgetColors.muted, fontSize = 11.sp)
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

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun formatClock(instant: Instant, zone: ZoneId): String = HOUR_MINUTE.format(instant.atZone(zone))

internal suspend fun loadWidgetData(context: Context, topCount: Int): WidgetData =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .widgetDataSource()
        .load(topCount)

/** Abre o app (tela inicial ou explicação da permissão, conforme o caso). */
internal fun launchIntent(context: Context): Intent =
    (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
