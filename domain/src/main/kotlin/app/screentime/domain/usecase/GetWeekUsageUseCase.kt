package app.screentime.domain.usecase

import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.WeekDay
import app.screentime.domain.model.WeekUsage
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Semana de segunda a domingo.
 * @param weekOffset 0 = semana atual, -1 = semana passada, e assim por diante.
 */
class GetWeekUsageUseCase(
    private val repository: UsageRepository,
    private val time: TimeSource,
) {
    suspend operator fun invoke(weekOffset: Int): WeekUsage {
        val today = time.today()
        val start = mondayOf(today).plusWeeks(weekOffset.toLong())
        val usages = (0L..6L).map { start.plusDays(it) }
            .filter { !it.isAfter(today) }
            .associateWith { repository.dayUsage(it) }
        return build(start, today, usages)
    }

    companion object {
        fun mondayOf(date: LocalDate): LocalDate =
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        /** Parte pura, separada para os testes. Dias ausentes em [usages] são tratados como futuros. */
        fun build(start: LocalDate, today: LocalDate, usages: Map<LocalDate, DayUsage>): WeekUsage {
            val days = (0L..6L).map { offset ->
                val date = start.plusDays(offset)
                val usage = usages[date]
                WeekDay(
                    date = date,
                    millis = usage?.takeIf { it.status == DataStatus.Available }?.totalMillis,
                    isFuture = date.isAfter(today),
                )
            }
            val withData = days.filter { it.millis != null }
            val total = withData.sumOf { it.millis ?: 0L }
            val peak = withData.filter { (it.millis ?: 0L) > 0L }.maxByOrNull { it.millis ?: 0L }

            val topApps = usages.values
                .filter { it.status == DataStatus.Available }
                .flatMap { it.apps }
                .groupBy { it.packageName }
                .map { (pkg, list) ->
                    AppUsage(
                        packageName = pkg,
                        label = list.firstNotNullOfOrNull { it.label },
                        foregroundMillis = list.sumOf { it.foregroundMillis },
                        sessionCount = list.sumOf { it.sessionCount },
                        lastUsedAt = list.mapNotNull { it.lastUsedAt }.maxOrNull(),
                    )
                }
                .sortedByDescending { it.foregroundMillis }

            return WeekUsage(
                start = start,
                days = days,
                totalMillis = total,
                averageMillis = if (withData.isEmpty()) null else total / withData.size,
                daysWithData = withData.size,
                peakDay = peak,
                topApps = topApps,
            )
        }
    }
}
