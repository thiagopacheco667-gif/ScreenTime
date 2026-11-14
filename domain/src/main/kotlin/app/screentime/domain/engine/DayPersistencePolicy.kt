package app.screentime.domain.engine

import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage

/**
 * Decide entre o dia salvo no banco e o recalculado a partir dos eventos.
 *
 * - Dia salvo e encerrado (isFinal): é a fonte definitiva. Não recalcula,
 *   porque o sistema apaga eventos antigos e o recálculo daria menos tempo.
 * - Caso contrário, recalcula. Se o recálculo tiver dados, ele vence e é salvo
 *   (assim um dia parcial salvo ontem à noite vira o dia completo hoje).
 * - Se o recálculo não tiver dados (eventos já apagados), usa o que estiver salvo,
 *   mesmo parcial; sem nada salvo, o dia fica "Sem dados".
 */
object DayPersistencePolicy {

    data class Decision(val result: DayUsage, val save: Boolean)

    fun needsLive(stored: DayUsage?): Boolean = stored?.isFinal != true

    fun choose(stored: DayUsage?, live: DayUsage): Decision = when {
        stored?.isFinal == true -> Decision(stored, save = false)
        live.status == DataStatus.Available -> Decision(live, save = true)
        stored != null -> Decision(stored, save = false)
        else -> Decision(live, save = false)
    }
}
