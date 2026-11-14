package app.screentime.domain.engine

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Limites de tempo de um dia local e da consulta de eventos correspondente.
 *
 * A consulta começa [LOOKBACK] antes da meia-noite, para descobrir quais apps
 * já estavam abertos quando o dia começou, e termina no fim do dia ou agora,
 * o que vier primeiro.
 */
data class DayWindow(
    val date: LocalDate,
    val zone: ZoneId,
    val dayStart: Instant,
    val dayEnd: Instant,
    val queryStart: Instant,
    val queryEnd: Instant,
) {
    /** true se o dia ainda não começou. */
    val isFuture: Boolean get() = queryEnd <= dayStart

    companion object {
        val LOOKBACK: Duration = Duration.ofHours(4)

        fun of(date: LocalDate, zone: ZoneId, now: Instant): DayWindow {
            // atStartOfDay respeita horário de verão (se a meia-noite não existir, usa a 1ª hora válida).
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            return DayWindow(
                date = date,
                zone = zone,
                dayStart = start,
                dayEnd = end,
                queryStart = start.minus(LOOKBACK),
                queryEnd = minOf(end, now),
            )
        }
    }
}
