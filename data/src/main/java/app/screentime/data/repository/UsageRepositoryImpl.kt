package app.screentime.data.repository

import app.screentime.data.source.AppInfoSource
import app.screentime.data.source.UsageEventsSource
import app.screentime.domain.engine.DayWindow
import app.screentime.domain.engine.UsageCalculator
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calcula o uso direto dos eventos do sistema.
 * Na etapa de persistência, os dias encerrados passam a ser lidos e gravados no Room.
 */
@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val events: UsageEventsSource,
    private val appInfo: AppInfoSource,
    private val accessChecker: UsageAccessChecker,
    private val time: TimeSource,
    private val calculator: UsageCalculator,
) : UsageRepository {

    override suspend fun dayUsage(date: LocalDate, upTo: Instant?): DayUsage =
        withContext(Dispatchers.IO) {
            ensureAccess()
            val now = time.now()
            val window = DayWindow.of(date, time.zone(), now)
            val raw = if (window.isFuture) emptyList() else events.query(
                window.queryStart.toEpochMilli(),
                window.queryEnd.toEpochMilli(),
            )
            calculator.computeDay(
                window = window,
                events = raw,
                now = now,
                excludedPackages = appInfo.excludedPackages(),
                labelOf = appInfo::labelOf,
                upTo = upTo,
            )
        }

    override suspend fun dayEvents(date: LocalDate): List<UsageEvent> =
        withContext(Dispatchers.IO) {
            ensureAccess()
            val window = DayWindow.of(date, time.zone(), time.now())
            events.query(window.dayStart.toEpochMilli(), window.queryEnd.toEpochMilli())
        }

    override suspend fun appLabel(packageName: String): String? =
        withContext(Dispatchers.IO) { appInfo.labelOf(packageName) }

    private fun ensureAccess() {
        if (!accessChecker.hasUsageAccess()) throw UsageAccessMissingException()
    }
}
