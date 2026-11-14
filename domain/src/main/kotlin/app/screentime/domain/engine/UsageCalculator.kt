package app.screentime.domain.engine

import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.UsageEvent
import java.time.Instant

/**
 * Calcula o uso de um dia a partir dos eventos brutos.
 * Não acessa o Android: recebe tudo por parâmetro e pode ser testado com eventos escritos à mão.
 */
class UsageCalculator(
    private val sessionBuilder: SessionBuilder = SessionBuilder(),
) {

    /**
     * @param events eventos entre [DayWindow.queryStart] e [DayWindow.queryEnd].
     * @param excludedPackages apps que não contam (launcher, interface do sistema).
     * @param labelOf nome exibido de cada pacote.
     * @param upTo corta o dia neste instante; usado para comparar "ontem até o mesmo horário".
     */
    fun computeDay(
        window: DayWindow,
        events: List<UsageEvent>,
        now: Instant,
        excludedPackages: Set<String> = emptySet(),
        labelOf: (String) -> String? = { null },
        upTo: Instant? = null,
    ): DayUsage {
        if (window.isFuture) return DayUsage.unavailable(window.date, window.zone, now)

        val dayStart = window.dayStart.toEpochMilli()
        val dayEnd = window.dayEnd.toEpochMilli()
        val isPastDay = dayEnd <= now.toEpochMilli()
        val hasEventsInDay = events.any { it.timestamp in dayStart until dayEnd }

        // Dia passado sem nenhum evento: o sistema já apagou (ou o celular ficou desligado).
        if (isPastDay && !hasEventsInDay) {
            return DayUsage.unavailable(window.date, window.zone, now)
        }

        val limitEnd = minOf(
            dayEnd,
            window.queryEnd.toEpochMilli(),
            upTo?.toEpochMilli() ?: Long.MAX_VALUE,
        )

        val clipped = sessionBuilder
            .build(events, window.queryStart.toEpochMilli(), window.queryEnd.toEpochMilli())
            .asSequence()
            .filter { it.packageName !in excludedPackages }
            .mapNotNull { s ->
                val start = maxOf(s.start, dayStart)
                val end = minOf(s.end, limitEnd)
                if (end > start) Session(s.packageName, start, end) else null
            }
            .toList()

        val apps = clipped
            .groupBy { it.packageName }
            .map { (pkg, sessions) ->
                AppUsage(
                    packageName = pkg,
                    label = labelOf(pkg),
                    foregroundMillis = sessions.sumOf { it.durationMillis },
                    sessionCount = sessions.size,
                    lastUsedAt = Instant.ofEpochMilli(sessions.maxOf { it.end }),
                )
            }
            .sortedByDescending { it.foregroundMillis }

        val union = union(clipped)

        return DayUsage(
            date = window.date,
            zoneId = window.zone,
            status = DataStatus.Available,
            totalMillis = union.sumOf { it.second - it.first },
            apps = apps,
            hourlyMillis = hourly(union, window, limitEnd),
            isFinal = isPastDay && upTo == null,
            computedAt = now,
        )
    }

    /** Junta intervalos sobrepostos para não contar o mesmo tempo duas vezes. */
    private fun union(sessions: List<Session>): List<Pair<Long, Long>> {
        val out = ArrayList<Pair<Long, Long>>()
        for (s in sessions.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null && s.start <= last.second) {
                out[out.lastIndex] = last.first to maxOf(last.second, s.end)
            } else {
                out += s.start to s.end
            }
        }
        return out
    }

    /** Distribui o tempo de tela pelas 24 horas locais do dia. */
    private fun hourly(union: List<Pair<Long, Long>>, window: DayWindow, limitEnd: Long): List<Long> {
        val buckets = LongArray(24)
        var boundary = window.dayStart.atZone(window.zone)
        while (boundary.toInstant().toEpochMilli() < limitEnd) {
            val next = boundary.plusHours(1)
            val hourStart = boundary.toInstant().toEpochMilli()
            val hourEnd = next.toInstant().toEpochMilli()
            for ((start, end) in union) {
                val overlap = minOf(hourEnd, end) - maxOf(hourStart, start)
                if (overlap > 0) buckets[boundary.hour] += overlap
            }
            boundary = next
        }
        return buckets.toList()
    }
}
