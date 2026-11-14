package app.screentime.domain.usecase

import app.screentime.domain.model.AppDetail
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.DayValue
import app.screentime.domain.repository.UsageRepository
import java.time.LocalDate

class GetAppDetailUseCase(private val repository: UsageRepository) {

    suspend operator fun invoke(packageName: String, date: LocalDate): AppDetail {
        val days: List<DayUsage> = (DAYS - 1 downTo 0).map { repository.dayUsage(date.minusDays(it.toLong())) }
        val label = repository.appLabel(packageName)
            ?: days.firstNotNullOfOrNull { d -> d.apps.firstOrNull { it.packageName == packageName }?.label }
        return build(packageName, label, date, days)
    }

    companion object {
        const val DAYS = 7

        /** Parte pura do cálculo, separada para os testes. */
        fun build(packageName: String, label: String?, date: LocalDate, days: List<DayUsage>): AppDetail {
            val selected = days.last()
            val lastDays = days.map { d ->
                DayValue(
                    date = d.date,
                    millis = if (d.status == DataStatus.Unavailable) {
                        null
                    } else {
                        d.apps.firstOrNull { it.packageName == packageName }?.foregroundMillis ?: 0L
                    },
                )
            }
            val withData = lastDays.mapNotNull { it.millis }
            return AppDetail(
                packageName = packageName,
                label = label,
                date = date,
                dayStatus = selected.status,
                usage = selected.apps.firstOrNull { it.packageName == packageName },
                dayTotalMillis = selected.totalMillis,
                lastDays = lastDays,
                averageMillis = if (withData.isEmpty()) null else withData.sum() / withData.size,
            )
        }
    }
}
