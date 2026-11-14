package app.screentime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ComparisonTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-11-10T15:00:00Z")

    private fun day(totalMinutes: Long, status: DataStatus = DataStatus.Available) = DayUsage(
        date = LocalDate.of(2026, 11, 10),
        zoneId = zone,
        status = status,
        totalMillis = totalMinutes * 60_000L,
        apps = emptyList(),
        hourlyMillis = List(24) { 0L },
        isFinal = false,
        computedAt = now,
    )

    @Test
    fun `mais uso hoje da porcentagem positiva`() {
        assertEquals(Comparison.Percent(50), Comparison.of(day(90), day(60)))
    }

    @Test
    fun `menos uso hoje da porcentagem negativa`() {
        assertEquals(Comparison.Percent(-25), Comparison.of(day(45), day(60)))
    }

    @Test
    fun `mesmo uso da zero`() {
        assertEquals(Comparison.Percent(0), Comparison.of(day(60), day(60)))
    }

    @Test
    fun `porcentagem e arredondada`() {
        // 70/60 = +16,67% → 17%
        assertEquals(Comparison.Percent(17), Comparison.of(day(70), day(60)))
    }

    @Test
    fun `ontem sem uso ate este horario nao tem base`() {
        assertEquals(Comparison.NoBaseline, Comparison.of(day(30), day(0)))
    }

    @Test
    fun `ontem sem dados fica indisponivel`() {
        assertEquals(Comparison.Unavailable, Comparison.of(day(30), day(0, DataStatus.Unavailable)))
    }
}
