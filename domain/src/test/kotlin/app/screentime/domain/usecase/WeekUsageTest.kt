package app.screentime.domain.usecase

import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeekUsageTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-11-12T15:00:00Z")
    private val min = 60_000L
    private val monday = LocalDate.of(2026, 11, 9)
    private val thursday = monday.plusDays(3)

    private fun day(date: LocalDate, apps: Map<String, Long>, available: Boolean = true) = DayUsage(
        date = date,
        zoneId = zone,
        status = if (available) DataStatus.Available else DataStatus.Unavailable,
        totalMillis = apps.values.sum() * min,
        apps = apps.map { (pkg, m) -> AppUsage(pkg, "App $pkg", m * min, 1, now) },
        hourlyMillis = List(24) { 0L },
        isFinal = true,
        computedAt = now,
    )

    @Test
    fun `segunda da semana`() {
        assertEquals(monday, GetWeekUsageUseCase.mondayOf(LocalDate.of(2026, 11, 15))) // domingo
        assertEquals(monday, GetWeekUsageUseCase.mondayOf(monday))
        assertEquals(DayOfWeek.MONDAY, GetWeekUsageUseCase.mondayOf(thursday).dayOfWeek)
    }

    @Test
    fun `total media pico e dias futuros`() {
        val usages = mapOf(
            monday to day(monday, emptyMap(), available = false),
            monday.plusDays(1) to day(monday.plusDays(1), mapOf("A" to 60, "B" to 30)),
            monday.plusDays(2) to day(monday.plusDays(2), mapOf("A" to 120)),
            thursday to day(thursday, mapOf("B" to 30)),
        )
        val w = GetWeekUsageUseCase.build(monday, today = thursday, usages = usages)

        assertEquals(7, w.days.size)
        assertNull(w.days[0].millis) // sem dados
        assertTrue(w.days.drop(4).all { it.isFuture && it.millis == null })
        assertEquals(240 * min, w.totalMillis)
        assertEquals(3, w.daysWithData)
        assertEquals(80 * min, w.averageMillis)
        assertEquals(monday.plusDays(2), w.peakDay?.date)
        assertEquals(listOf("A", "B"), w.topApps.map { it.packageName })
        assertEquals(180 * min, w.topApps.first().foregroundMillis)
        assertEquals(2, w.topApps.first().sessionCount)
    }

    @Test
    fun `semana sem dados nao tem media nem pico`() {
        val w = GetWeekUsageUseCase.build(monday, today = monday.plusDays(6), usages = emptyMap())
        assertNull(w.averageMillis)
        assertNull(w.peakDay)
        assertEquals(0, w.daysWithData)
    }
}
