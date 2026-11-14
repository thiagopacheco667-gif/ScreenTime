package app.screentime.domain.usecase

import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AppDetailTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-11-10T15:00:00Z")
    private val end = LocalDate.of(2026, 11, 10)
    private val min = 60_000L

    private fun day(offset: Long, appMinutes: Long?, totalMinutes: Long = appMinutes ?: 0, available: Boolean = true) =
        DayUsage(
            date = end.minusDays(offset),
            zoneId = zone,
            status = if (available) DataStatus.Available else DataStatus.Unavailable,
            totalMillis = totalMinutes * min,
            apps = if (appMinutes == null) emptyList() else listOf(
                AppUsage("A", "App A", appMinutes * min, 1, now),
            ),
            hourlyMillis = List(24) { 0L },
            isFinal = offset > 0,
            computedAt = now,
        )

    @Test
    fun `media ignora dias sem dados e conta dias sem uso como zero`() {
        val days = listOf(
            day(6, null, available = false),
            day(5, null, available = false),
            day(4, 30),
            day(3, null), // sem uso do app: zero
            day(2, 60),
            day(1, 30),
            day(0, 30, totalMinutes = 120),
        )
        val d = GetAppDetailUseCase.build("A", "App A", end, days)
        assertEquals(listOf(null, null, 30L, 0L, 60L, 30L, 30L), d.lastDays.map { it.millis?.div(min) })
        assertEquals(30 * min, d.averageMillis) // (30+0+60+30+30)/5
        assertEquals(0.25f, d.shareOfDay!!, 0.0001f)
    }

    @Test
    fun `app nao usado no dia nao tem fatia`() {
        val days = (6L downTo 0L).map { day(it, null, totalMinutes = 50) }
        val d = GetAppDetailUseCase.build("A", null, end, days)
        assertNull(d.usage)
        assertNull(d.shareOfDay)
        assertEquals(0L, d.averageMillis)
        assertEquals("A", d.displayName)
    }

    @Test
    fun `sem nenhum dia com dados a media e nula`() {
        val days = (6L downTo 0L).map { day(it, null, available = false) }
        val d = GetAppDetailUseCase.build("A", null, end, days)
        assertNull(d.averageMillis)
        assertEquals(DataStatus.Unavailable, d.dayStatus)
    }
}
