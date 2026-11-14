package app.screentime.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCounterTest {

    private val counter = OpenCounter()
    private val s = 1_000L
    private val min = 60_000L

    private fun session(pkg: String, start: Long, end: Long) = Session(pkg, start, end)

    @Test
    fun `voltar em ate 30 s sem nada no meio conta uma vez`() {
        val r = counter.count(listOf(session("A", 0, 5 * min), session("A", 5 * min + 10 * s, 8 * min)))
        assertEquals(1, r["A"])
    }

    @Test
    fun `voltar depois de 30 s conta de novo`() {
        val r = counter.count(listOf(session("A", 0, 5 * min), session("A", 5 * min + 45 * s, 8 * min)))
        assertEquals(2, r["A"])
    }

    @Test
    fun `compartilhar e voltar nao conta como abertura nova`() {
        val r = counter.count(
            listOf(
                session("A", 0, 2 * min),
                session("com.android.intentresolver", 2 * min, 2 * min + 8 * s),
                session("A", 2 * min + 9 * s, 4 * min),
            ),
        )
        assertEquals(1, r["A"])
    }

    @Test
    fun `seletor de fotos e login do Google nao interrompem`() {
        val r = counter.count(
            listOf(
                session("A", 0, min),
                session("com.google.android.providers.media.module", min, min + 15 * s),
                session("A", min + 16 * s, 2 * min),
                session("com.google.android.gms", 2 * min, 2 * min + 20 * s),
                session("A", 2 * min + 21 * s, 3 * min),
            ),
        )
        assertEquals(1, r["A"])
    }

    @Test
    fun `outro app de verdade no meio conta abertura nova`() {
        val r = counter.count(
            listOf(
                session("A", 0, min),
                session("B", min, min + 10 * s),
                session("A", min + 11 * s, 2 * min),
            ),
        )
        assertEquals(2, r["A"])
        assertEquals(1, r["B"])
    }

    @Test
    fun `espiar os recentes por 1 s e voltar conta uma vez`() {
        val r = counter.count(
            listOf(
                session("A", 0, min),
                session("launcher", min, min + 1 * s),
                session("A", min + 2 * s, 2 * min),
            ),
        )
        assertEquals(1, r["A"])
    }

    @Test
    fun `ficar na tela inicial 10 s e voltar conta de novo`() {
        val r = counter.count(
            listOf(
                session("A", 0, min),
                session("launcher", min, min + 10 * s),
                session("A", min + 11 * s, 2 * min),
            ),
        )
        assertEquals(2, r["A"])
    }

    @Test
    fun `tela apaga rapido e volta conta uma vez, apagada por minutos conta duas`() {
        val quick = counter.count(listOf(session("A", 0, min), session("A", min + 12 * s, 2 * min)))
        val long = counter.count(listOf(session("A", 0, min), session("A", 6 * min, 7 * min)))
        assertEquals(1, quick["A"])
        assertEquals(2, long["A"])
    }

    @Test
    fun `muitas idas e vindas rapidas nao inflam a contagem`() {
        // Parecido com o caso real: o app "pisca" 40 vezes com pausas curtas.
        val sessions = (0 until 40).map { i -> session("A", i * 20 * s, i * 20 * s + 15 * s) }
        assertEquals(1, counter.count(sessions)["A"])
    }
}
