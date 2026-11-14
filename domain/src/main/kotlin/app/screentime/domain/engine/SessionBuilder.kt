package app.screentime.domain.engine

import app.screentime.domain.model.UsageEvent
import app.screentime.domain.model.UsageEventType

/**
 * Transforma eventos brutos em sessões de primeiro plano.
 *
 * Regras (ver também a tela "Como calculamos"):
 * 1. Um app entra em primeiro plano no primeiro ActivityResumed e sai quando
 *    todas as telas dele pausam ou param. Trocar de tela dentro do mesmo app
 *    não abre uma sessão nova.
 * 2. Só um app conta por vez: quando outro app abre, o anterior é fechado,
 *    mesmo que o sistema não tenha registrado a pausa dele. Isso evita que um
 *    evento perdido conte horas a mais. Em tela dividida, conta o app em foco.
 * 3. Tela desligada, tela de bloqueio e desligamento fecham tudo.
 * 4. Celular que ligou sem registrar o desligamento: as sessões abertas
 *    fecham no último evento conhecido antes de ligar.
 * 5. Se o primeiro evento de um app na janela for uma pausa, ele já estava
 *    aberto antes da janela: a sessão começa no início da janela.
 * 6. O que ainda estiver aberto no fim da janela fecha no fim da janela
 *    (normalmente "agora").
 * 7. Sessões do mesmo app separadas por até [mergeGapMillis] viram uma só.
 */
class SessionBuilder(
    private val mergeGapMillis: Long = DEFAULT_MERGE_GAP_MILLIS,
) {

    fun build(events: List<UsageEvent>, windowStart: Long, windowEnd: Long): List<Session> {
        if (windowEnd <= windowStart) return emptyList()

        // sortedBy é estável: eventos com o mesmo horário mantêm a ordem do sistema.
        val sorted = events
            .filter { it.timestamp in windowStart..windowEnd }
            .sortedBy { it.timestamp }

        val activeClasses = HashMap<String, MutableSet<String>>()
        val openSince = LinkedHashMap<String, Long>()
        val seen = HashSet<String>()
        val result = ArrayList<Session>()

        // Primeiro momento em que o app em primeiro plano certamente mudou
        // (outro app abriu ou a tela desligou). Limita sessões "órfãs" da regra 5.
        var firstBoundary: Long? = null
        var previousTs = windowStart

        fun close(pkg: String, at: Long) {
            activeClasses.remove(pkg)
            val start = openSince.remove(pkg) ?: return
            if (at > start) result += Session(pkg, start, at)
        }

        fun closeAll(at: Long) {
            for (pkg in openSince.keys.toList()) close(pkg, at)
            activeClasses.clear()
        }

        for (event in sorted) {
            val ts = event.timestamp
            val pkg = event.packageName
            val cls = event.className.orEmpty()

            when (event.type) {
                UsageEventType.ActivityResumed -> {
                    if (firstBoundary == null) firstBoundary = ts
                    for (other in openSince.keys.toList()) {
                        if (other != pkg) close(other, ts)
                    }
                    activeClasses.getOrPut(pkg) { HashSet() }.add(cls)
                    openSince.putIfAbsent(pkg, ts)
                    seen += pkg
                }

                UsageEventType.ActivityPaused,
                UsageEventType.ActivityStopped -> {
                    val classes = activeClasses[pkg]
                    if (classes != null && classes.remove(cls)) {
                        if (classes.isEmpty()) close(pkg, ts)
                    } else if (pkg !in seen) {
                        val end = minOf(ts, firstBoundary ?: ts)
                        if (end > windowStart) result += Session(pkg, windowStart, end)
                    }
                    seen += pkg
                }

                UsageEventType.ScreenNonInteractive,
                UsageEventType.KeyguardShown,
                UsageEventType.DeviceShutdown -> {
                    if (firstBoundary == null) firstBoundary = ts
                    closeAll(ts)
                }

                UsageEventType.DeviceStartup -> {
                    if (firstBoundary == null) firstBoundary = previousTs
                    closeAll(previousTs)
                }

                UsageEventType.ScreenInteractive,
                UsageEventType.KeyguardHidden -> Unit
            }
            previousTs = ts
        }

        closeAll(windowEnd)
        return merge(result)
    }

    private fun merge(sessions: List<Session>): List<Session> {
        val out = ArrayList<Session>(sessions.size)
        for (s in sessions.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null &&
                last.packageName == s.packageName &&
                s.start - last.end <= mergeGapMillis
            ) {
                out[out.lastIndex] = last.copy(end = maxOf(last.end, s.end))
            } else {
                out += s
            }
        }
        return out
    }

    companion object {
        const val DEFAULT_MERGE_GAP_MILLIS: Long = 2_000L
    }
}
