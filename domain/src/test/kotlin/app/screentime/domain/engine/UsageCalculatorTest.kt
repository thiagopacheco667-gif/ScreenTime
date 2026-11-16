package app.screentime.domain.engine

import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.model.UsageEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Testes do cálculo com sequências de eventos escritas à mão.
 * Estes eventos existem só aqui, para verificar as regras; nunca aparecem no app.
 */
class UsageCalculatorTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day = LocalDate.of(2026, 11, 10)
    private val yesterday = day.minusDays(1)
    private val calc = UsageCalculator()

    private fun t(h: Int, m: Int, s: Int = 0, d: LocalDate = day, z: ZoneId = zone): Long =
        d.atTime(h, m, s).atZone(z).toInstant().toEpochMilli()

    private fun ev(ts: Long, type: UsageEventType, pkg: String = "android", cls: String? = null) =
        UsageEvent(ts, pkg, cls, type)

    private fun resumed(ts: Long, pkg: String, cls: String = "Main") = ev(ts, UsageEventType.ActivityResumed, pkg, cls)
    private fun paused(ts: Long, pkg: String, cls: String = "Main") = ev(ts, UsageEventType.ActivityPaused, pkg, cls)
    private fun stopped(ts: Long, pkg: String, cls: String = "Main") = ev(ts, UsageEventType.ActivityStopped, pkg, cls)

    private fun compute(
        events: List<UsageEvent>,
        now: Long,
        date: LocalDate = day,
        excluded: Set<String> = emptySet(),
        upTo: Long? = null,
        z: ZoneId = zone,
    ): DayUsage {
        val nowInstant = Instant.ofEpochMilli(now)
        return calc.computeDay(
            window = DayWindow.of(date, z, nowInstant),
            events = events,
            now = nowInstant,
            excludedPackages = excluded,
            upTo = upTo?.let(Instant::ofEpochMilli),
        )
    }

    private fun DayUsage.minutes(pkg: String): Long =
        apps.first { it.packageName == pkg }.foregroundMillis / MIN

    private fun DayUsage.sessions(pkg: String): Int =
        apps.first { it.packageName == pkg }.sessionCount

    private val DayUsage.totalMinutes: Long get() = totalMillis / MIN

    @Test
    fun `uma sessao simples`() {
        val r = compute(listOf(resumed(t(10, 0), "A"), paused(t(10, 30), "A")), now = t(12, 0))
        assertEquals(30, r.minutes("A"))
        assertEquals(1, r.sessions("A"))
        assertEquals(30, r.totalMinutes)
    }

    @Test
    fun `trocar de tela dentro do app conta como uma sessao`() {
        val events = listOf(
            resumed(t(10, 0), "A", "Main"),
            paused(t(10, 10), "A", "Main"),
            resumed(t(10, 10) + 200, "A", "Detail"),
            stopped(t(10, 10, 1), "A", "Main"),
            paused(t(10, 20), "A", "Detail"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(20, r.minutes("A"))
        assertEquals(1, r.sessions("A"))
    }

    @Test
    fun `outro app abrindo fecha o anterior mesmo sem pausa registrada`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            resumed(t(10, 15), "B"),
            paused(t(10, 45), "B"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(15, r.minutes("A"))
        assertEquals(30, r.minutes("B"))
        assertEquals(45, r.totalMinutes)
    }

    @Test
    fun `app aberto antes da meia-noite sem evento de abertura conta desde 00h00`() {
        val r = compute(listOf(paused(t(0, 30), "A")), now = t(12, 0))
        assertEquals(30, r.minutes("A"))
    }

    @Test
    fun `sessao orfa termina quando a tela desliga`() {
        val events = listOf(
            ev(t(0, 10), UsageEventType.ScreenNonInteractive),
            paused(t(0, 12), "A"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(10, r.minutes("A"))
    }

    @Test
    fun `sessao que atravessa a meia-noite e dividida entre os dois dias`() {
        val events = listOf(
            resumed(t(23, 50, d = yesterday), "A"),
            paused(t(0, 20), "A"),
        )
        val today = compute(events, now = t(12, 0))
        val previous = compute(events, now = t(12, 0), date = yesterday)
        assertEquals(20, today.minutes("A"))
        assertEquals(10, previous.minutes("A"))
        assertTrue(previous.isFinal)
        assertFalse(today.isFinal)
    }

    @Test
    fun `tela desligada fecha a sessao e desbloquear abre outra`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            ev(t(10, 10), UsageEventType.ScreenNonInteractive),
            paused(t(10, 10, 1), "A"),
            ev(t(10, 30), UsageEventType.ScreenInteractive),
            ev(t(10, 30, 5), UsageEventType.KeyguardHidden),
            resumed(t(10, 31), "A"),
            paused(t(10, 41), "A"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(20, r.minutes("A"))
        assertEquals(2, r.sessions("A"))
    }

    @Test
    fun `tela de bloqueio fecha a sessao`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            ev(t(10, 5), UsageEventType.KeyguardShown),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(5, r.minutes("A"))
    }

    @Test
    fun `sessao ainda aberta termina agora`() {
        val r = compute(listOf(resumed(t(11, 0), "A")), now = t(11, 25))
        assertEquals(25, r.minutes("A"))
    }

    @Test
    fun `celular que ligou sem registrar desligamento fecha no ultimo evento`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            ev(t(10, 30), UsageEventType.KeyguardHidden),
            ev(t(12, 0), UsageEventType.DeviceStartup),
            resumed(t(12, 5), "B"),
            paused(t(12, 10), "B"),
        )
        val r = compute(events, now = t(13, 0))
        assertEquals(30, r.minutes("A"))
        assertEquals(5, r.minutes("B"))
    }

    @Test
    fun `desligamento registrado fecha a sessao`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            ev(t(10, 40), UsageEventType.DeviceShutdown),
            ev(t(11, 0), UsageEventType.DeviceStartup),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(40, r.minutes("A"))
    }

    @Test
    fun `launcher excluido nao conta no total nem na lista`() {
        val events = listOf(
            resumed(t(10, 0), "A"), paused(t(10, 10), "A"),
            resumed(t(10, 10), "L"), paused(t(10, 20), "L"),
            resumed(t(10, 20), "B"), paused(t(10, 30), "B"),
        )
        val r = compute(events, now = t(12, 0), excluded = setOf("L"))
        assertEquals(20, r.totalMinutes)
        assertEquals(setOf("A", "B"), r.apps.map { it.packageName }.toSet())
    }

    @Test
    fun `pausa e parada da mesma tela nao criam sessao extra`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            paused(t(10, 10), "A"),
            stopped(t(10, 10) + 500, "A"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(10, r.minutes("A"))
        assertEquals(1, r.sessions("A"))
    }

    @Test
    fun `apps ficam ordenados do mais usado para o menos usado`() {
        val events = listOf(
            resumed(t(9, 0), "A"), paused(t(9, 5), "A"),
            resumed(t(9, 5), "B"), paused(t(9, 50), "B"),
            resumed(t(9, 50), "C"), paused(t(10, 10), "C"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(listOf("B", "C", "A"), r.apps.map { it.packageName })
    }

    @Test
    fun `distribuicao por hora soma o total`() {
        val r = compute(listOf(resumed(t(9, 50), "A"), paused(t(10, 20), "A")), now = t(12, 0))
        assertEquals(24, r.hourlyMillis.size)
        assertEquals(10 * MIN, r.hourlyMillis[9])
        assertEquals(20 * MIN, r.hourlyMillis[10])
        assertEquals(r.totalMillis, r.hourlyMillis.sum())
    }

    @Test
    fun `upTo corta o dia no horario pedido`() {
        val events = listOf(resumed(t(10, 0), "A"), paused(t(11, 0), "A"))
        val r = compute(events, now = t(23, 0), upTo = t(10, 30))
        assertEquals(30, r.minutes("A"))
        assertFalse(r.isFinal)
    }

    @Test
    fun `dia passado sem eventos aparece como sem dados`() {
        val r = compute(emptyList(), now = t(12, 0), date = day.minusDays(3))
        assertEquals(DataStatus.Unavailable, r.status)
    }

    @Test
    fun `hoje sem eventos aparece como zero disponivel`() {
        val r = compute(emptyList(), now = t(0, 5))
        assertEquals(DataStatus.Available, r.status)
        assertEquals(0, r.totalMillis)
    }

    @Test
    fun `dia futuro aparece como sem dados`() {
        val r = compute(emptyList(), now = t(12, 0), date = day.plusDays(1))
        assertEquals(DataStatus.Unavailable, r.status)
    }

    @Test
    fun `eventos fora de ordem sao ordenados antes do calculo`() {
        val events = listOf(paused(t(10, 30), "A"), resumed(t(10, 0), "A"))
        val r = compute(events, now = t(12, 0))
        assertEquals(30, r.minutes("A"))
    }

    @Test
    fun `dia com 25 horas no fim do horario de verao soma o tempo real`() {
        // Nova York, 1º de novembro de 2026: 01:00–01:59 acontece duas vezes.
        val ny = ZoneId.of("America/New_York")
        val dstDay = LocalDate.of(2026, 11, 1)
        val start = t(0, 30, d = dstDay, z = ny)
        val end = t(3, 30, d = dstDay, z = ny)
        val r = compute(
            listOf(resumed(start, "A"), paused(end, "A")),
            now = t(12, 0, d = dstDay, z = ny),
            date = dstDay,
            z = ny,
        )
        assertEquals(240, r.minutes("A"))
        assertEquals(120 * MIN, r.hourlyMillis[1])
        assertEquals(r.totalMillis, r.hourlyMillis.sum())
    }

    @Test
    fun `video em janela flutuante continua contando para o app`() {
        // YouTube entra em picture-in-picture às 10:10 e o usuário abre o B.
        // A janela some às 10:40 (ActivityStopped). Caso real de 16/11.
        val events = listOf(
            resumed(t(10, 0), "yt"),
            paused(t(10, 10), "yt"),
            resumed(t(10, 10) + 300, "B"),
            paused(t(10, 30), "B"),
            stopped(t(10, 30) + 400, "B"),
            resumed(t(10, 30) + 200, "C"),
            stopped(t(10, 40), "yt"),
            paused(t(10, 50), "C"),
            stopped(t(10, 50) + 400, "C"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(40, r.minutes("yt"))
        assertEquals(1, r.sessions("yt"))
        assertEquals(20, r.minutes("B"))
        // O total é o tempo de tela, sem contar a sobreposição duas vezes.
        assertEquals(50, r.totalMinutes)
    }

    @Test
    fun `sair normalmente nao deixa tempo sobrando`() {
        val events = listOf(
            resumed(t(10, 0), "A"),
            paused(t(10, 10), "A"),
            resumed(t(10, 10) + 100, "launcher"),
            stopped(t(10, 10) + 500, "A"),
        )
        val r = compute(events, now = t(12, 0), excluded = setOf("launcher"))
        assertEquals(10 * MIN + 500, r.apps.first { it.packageName == "A" }.foregroundMillis)
    }

    @Test
    fun `tela desligada encerra a janela flutuante`() {
        val events = listOf(
            resumed(t(10, 0), "yt"),
            paused(t(10, 10), "yt"),
            resumed(t(10, 10) + 300, "B"),
            stopped(t(10, 11), "B"),
            ev(t(10, 20), UsageEventType.ScreenNonInteractive),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(20, r.minutes("yt"))
    }

    @Test
    fun `sem eventos de saida (Android 9) a pausa encerra o app`() {
        val events = listOf(
            resumed(t(10, 0), "yt"),
            paused(t(10, 10), "yt"),
            resumed(t(10, 10) + 300, "B"),
            paused(t(10, 30), "B"),
        )
        val r = compute(events, now = t(12, 0))
        assertEquals(10, r.minutes("yt"))
    }

    private companion object {
        const val MIN = 60_000L
    }
}
