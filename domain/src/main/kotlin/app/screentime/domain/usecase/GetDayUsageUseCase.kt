package app.screentime.domain.usecase

import app.screentime.domain.model.DayUsage
import app.screentime.domain.repository.UsageRepository
import java.time.LocalDate

class GetDayUsageUseCase(private val repository: UsageRepository) {
    suspend operator fun invoke(date: LocalDate): DayUsage = repository.dayUsage(date)
}
