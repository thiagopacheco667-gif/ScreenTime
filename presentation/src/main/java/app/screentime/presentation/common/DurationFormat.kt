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

private val PT_BR: java.util.Locale = java.util.Locale.forLanguageTag("pt-BR")
private val WEEKDAY_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", PT_BR)
private val WEEKDAY_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", PT_BR)
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR)

/** "qui 12" (sem o ponto da abreviação). */
fun formatWeekdayDay(date: java.time.LocalDate): String = WEEKDAY_DAY.format(date).replace(".", "")

/** "qui" (sem o ponto), para rótulos de gráfico. */
fun formatWeekdayShort(date: java.time.LocalDate): String = WEEKDAY_SHORT.format(date).replace(".", "")

/** "12 de novembro" */
fun formatDayMonth(date: java.time.LocalDate): String = DAY_MONTH.format(date)

private val WEEKDAY_LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", PT_BR)

/** "quarta-feira" */
fun formatWeekdayLong(date: java.time.LocalDate): String = WEEKDAY_LONG.format(date)

/** "9 a 15 de novembro" ou "27 de outubro a 2 de novembro" */
fun formatRange(start: java.time.LocalDate, end: java.time.LocalDate): String =
    if (start.month == end.month && start.year == end.year) {
        "${start.dayOfMonth} a ${formatDayMonth(end)}"
    } else {
        "${formatDayMonth(start)} a ${formatDayMonth(end)}"
    }
