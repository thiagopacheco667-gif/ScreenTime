package app.screentime.domain.usecase

import app.screentime.domain.model.Comparison
import app.screentime.domain.model.Dashboard
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageRepository

/**
 * Monta o painel: hoje, ontem até o mesmo horário e ontem inteiro.
 * Comparar com o mesmo horário evita a falsa "queda" de manhã, quando hoje
 * ainda está no começo e ontem já terminou.
 */
class GetDashboardUseCase(
    private val repository: UsageRepository,
    private val time: TimeSource,
) {
    suspend operator fun invoke(): Dashboard {
        val zone = time.zone()
        val nowLocal = time.now().atZone(zone)
        val today = nowLocal.toLocalDate()
        val yesterday = today.minusDays(1)
        // atZone ajusta sozinho se esse horário não existir ontem (horário de verão).
        val sameTimeYesterday = yesterday.atTime(nowLocal.toLocalTime()).atZone(zone).toInstant()

        val todayUsage = repository.dayUsage(today)
        val yesterdaySameTime = repository.dayUsage(yesterday, upTo = sameTimeYesterday)
        val yesterdayFull = repository.dayUsage(yesterday)

        return Dashboard(
            today = todayUsage,
            yesterdaySameTime = yesterdaySameTime,
            yesterday = yesterdayFull,
            comparison = Comparison.of(todayUsage, yesterdaySameTime),
        )
    }
}
