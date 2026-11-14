package app.screentime.data.repository

import android.content.SharedPreferences
import app.screentime.data.db.AppMetadataEntity
import app.screentime.data.db.AppUsageDailyEntity
import app.screentime.data.db.DaySummaryEntity
import app.screentime.data.db.UsageDao
import app.screentime.data.source.AppInfoSource
import app.screentime.data.source.UsageEventsSource
import app.screentime.domain.engine.DayPersistencePolicy
import app.screentime.domain.engine.DayWindow
import app.screentime.domain.engine.UsageCalculator
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.StorageInfo
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Junta os eventos do sistema e o banco local.
 * Regra de qual fonte vale em cada caso: [DayPersistencePolicy].
 */
@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val events: UsageEventsSource,
    private val appInfo: AppInfoSource,
    private val accessChecker: UsageAccessChecker,
    private val time: TimeSource,
    private val calculator: UsageCalculator,
    private val dao: UsageDao,
    private val prefs: SharedPreferences,
) : UsageRepository {

    // Evita que o worker e a tela gravem o mesmo dia ao mesmo tempo.
    private val writeLock = Mutex()

    override suspend fun dayUsage(date: LocalDate, upTo: Instant?): DayUsage =
        withContext(Dispatchers.IO) {
            val now = time.now()
            val zone = time.zone()

            if (upTo != null) {
                ensureAccess()
                return@withContext computeLive(date, zone, now, upTo)
            }

            val stored = loadStored(date)
            if (stored != null && !DayPersistencePolicy.needsLive(stored)) {
                return@withContext stored
            }
            if (!accessChecker.hasUsageAccess()) {
                return@withContext stored ?: throw UsageAccessMissingException()
            }

            val live = computeLive(date, zone, now, null)
            val decision = DayPersistencePolicy.choose(stored, live)
            if (decision.save) save(decision.result)
            decision.result
        }

    override suspend fun cachedDayUsage(date: LocalDate): DayUsage? =
        withContext(Dispatchers.IO) { loadStored(date) }

    override suspend fun dayEvents(date: LocalDate): List<UsageEvent> =
        withContext(Dispatchers.IO) {
            ensureAccess()
            val window = DayWindow.of(date, time.zone(), time.now())
            events.query(window.dayStart.toEpochMilli(), window.queryEnd.toEpochMilli())
        }

    override suspend fun appLabel(packageName: String): String? =
        withContext(Dispatchers.IO) {
            appInfo.labelOf(packageName) ?: dao.labels(listOf(packageName)).firstOrNull()?.label
        }

    override suspend fun syncRecentDays(): Int {
        ensureAccess()
        val today = time.today()
        for (offset in 0 until SYNC_DAYS) {
            dayUsage(today.minusDays(offset.toLong()))
        }
        return SYNC_DAYS
    }

    override suspend fun recordBackgroundSync(at: Instant) {
        withContext(Dispatchers.IO) {
            prefs.edit().putLong(KEY_LAST_SYNC, at.toEpochMilli()).apply()
        }
    }

    override suspend fun storageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        StorageInfo(
            daysStored = dao.countDays(),
            oldestDate = dao.oldestDay()?.let(LocalDate::ofEpochDay),
            lastBackgroundSyncAt = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli),
        )
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            writeLock.withLock { dao.clearAll() }
            prefs.edit().remove(KEY_LAST_SYNC).apply()
        }
    }

    // --- internos ---

    private fun computeLive(date: LocalDate, zone: ZoneId, now: Instant, upTo: Instant?): DayUsage {
        val window = DayWindow.of(date, zone, now)
        val raw = if (window.isFuture) {
            emptyList()
        } else {
            events.query(window.queryStart.toEpochMilli(), window.queryEnd.toEpochMilli())
        }
        return calculator.computeDay(
            window = window,
            events = raw,
            now = now,
            excludedPackages = appInfo.excludedPackages(),
            labelOf = appInfo::labelOf,
            upTo = upTo,
        )
    }

    private suspend fun loadStored(date: LocalDate): DayUsage? {
        val epochDay = date.toEpochDay()
        val summary = dao.day(epochDay) ?: return null
        val apps = dao.apps(epochDay)
        val savedLabels = dao.labels(apps.map { it.packageName }).associate { it.packageName to it.label }
        return DayUsage(
            date = date,
            zoneId = runCatching { ZoneId.of(summary.zoneId) }.getOrDefault(time.zone()),
            status = DataStatus.Available,
            totalMillis = summary.totalMillis,
            apps = apps.map { a ->
                AppUsage(
                    packageName = a.packageName,
                    label = appInfo.labelOf(a.packageName) ?: savedLabels[a.packageName],
                    foregroundMillis = a.foregroundMillis,
                    sessionCount = a.sessionCount,
                    lastUsedAt = a.lastUsedAt?.let(Instant::ofEpochMilli),
                )
            },
            hourlyMillis = parseHourly(summary.hourlyCsv),
            isFinal = summary.isFinal,
            computedAt = Instant.ofEpochMilli(summary.computedAt),
        )
    }

    private suspend fun save(day: DayUsage) {
        val epochDay = day.date.toEpochDay()
        writeLock.withLock {
            dao.replaceDay(
                day = DaySummaryEntity(
                    epochDay = epochDay,
                    totalMillis = day.totalMillis,
                    hourlyCsv = day.hourlyMillis.joinToString(","),
                    isFinal = day.isFinal,
                    zoneId = day.zoneId.id,
                    computedAt = day.computedAt.toEpochMilli(),
                ),
                apps = day.apps.map {
                    AppUsageDailyEntity(
                        epochDay = epochDay,
                        packageName = it.packageName,
                        foregroundMillis = it.foregroundMillis,
                        sessionCount = it.sessionCount,
                        lastUsedAt = it.lastUsedAt?.toEpochMilli(),
                    )
                },
            )
            val labels = day.apps.mapNotNull { app ->
                app.label?.let { AppMetadataEntity(app.packageName, it, day.computedAt.toEpochMilli()) }
            }
            if (labels.isNotEmpty()) dao.upsertLabels(labels)
        }
    }

    private fun parseHourly(csv: String): List<Long> {
        val values = csv.split(',').mapNotNull { it.trim().toLongOrNull() }
        return List(24) { values.getOrElse(it) { 0L } }
    }

    private fun ensureAccess() {
        if (!accessChecker.hasUsageAccess()) throw UsageAccessMissingException()
    }

    private companion object {
        /** O Android costuma guardar eventos detalhados por cerca de uma semana; 10 dias dá folga. */
        const val SYNC_DAYS = 10
        const val KEY_LAST_SYNC = "last_background_sync"
    }
}
