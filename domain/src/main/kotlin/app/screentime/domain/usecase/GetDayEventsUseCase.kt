package app.screentime.domain.usecase

import app.screentime.domain.model.UsageEvent
import app.screentime.domain.repository.UsageRepository
import java.time.LocalDate

class GetDayEventsUseCase(private val repository: UsageRepository) {
    suspend operator fun invoke(date: LocalDate): List<UsageEvent> = repository.dayEvents(date)
}
