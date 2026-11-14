package app.screentime.domain.engine

import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayPersistencePolicyTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val date = LocalDate.of(2026, 11, 10)
    private val now = Instant.parse("2026-11-12T15:00:00Z")

    private fun day(minutes: Long, final: Boolean, available: Boolean = true) = DayUsage(
        date = date,
        zoneId = zone,
        status = if (available) DataStatus.Available else DataStatus.Unavailable,
        totalMillis = minutes * 60_000L,
        apps = emptyList(),
        hourlyMillis = List(24) { 0L },
        isFinal = final,
        computedAt = now,
    )

    @Test
    fun `dia salvo encerrado nao e recalculado`() {
        val stored = day(120, final = true)
        assertFalse(DayPersistencePolicy.needsLive(stored))
        val d = DayPersistencePolicy.choose(stored, day(30, final = true))
        assertSame(stored, d.result)
        assertFalse(d.save)
    }

    @Test
    fun `dia parcial salvo e substituido pelo recalculo completo`() {
        val stored = day(60, final = false)
        assertTrue(DayPersistencePolicy.needsLive(stored))
        val live = day(90, final = true)
        val d = DayPersistencePolicy.choose(stored, live)
        assertSame(live, d.result)
        assertTrue(d.save)
    }

    @Test
    fun `eventos apagados usam o parcial salvo`() {
        val stored = day(60, final = false)
        val d = DayPersistencePolicy.choose(stored, day(0, final = false, available = false))
        assertSame(stored, d.result)
        assertFalse(d.save)
    }

    @Test
    fun `sem nada salvo e sem eventos fica sem dados`() {
        assertTrue(DayPersistencePolicy.needsLive(null))
        val d = DayPersistencePolicy.choose(null, day(0, final = false, available = false))
        assertEquals(DataStatus.Unavailable, d.result.status)
        assertFalse(d.save)
    }

    @Test
    fun `hoje e sempre recalculado e salvo`() {
        val d = DayPersistencePolicy.choose(day(10, final = false), day(25, final = false))
        assertEquals(25 * 60_000L, d.result.totalMillis)
        assertTrue(d.save)
    }
}
