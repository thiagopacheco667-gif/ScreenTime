package app.screentime.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formata durações no padrão brasileiro curto:
 * 45 s · 12 min · 2 h · 2 h 05 min
 */
fun formatDuration(millis: Long): String {
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
private val HOUR_MINUTE_SECOND: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun formatClock(instant: Instant, zone: ZoneId): String =
    HOUR_MINUTE.format(instant.atZone(zone))

fun formatClockSeconds(epochMillis: Long, zone: ZoneId): String =
    HOUR_MINUTE_SECOND.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

private val LONG_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", java.util.Locale.forLanguageTag("pt-BR"))

/** "Sábado, 14 de novembro" */
fun formatLongDate(date: java.time.LocalDate): String =
    LONG_DATE.format(date).replaceFirstChar { it.titlecase(java.util.Locale.forLanguageTag("pt-BR")) }
