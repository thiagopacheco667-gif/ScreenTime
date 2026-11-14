package app.screentime.domain.engine

import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage

/**
 * Decide entre o dia salvo no banco e o recalculado a partir dos eventos.
 *
 * - Dia salvo, encerrado e calculado com as regras atuais ([CURRENT_CALC_VERSION]):
 *   é a fonte definitiva. Não recalcula, porque o sistema apaga eventos antigos.
 * - Dia salvo e encerrado, mas com regras antigas: tenta recalcular. Só troca se o
 *   recálculo estiver completo (dia encerrado e com pelo menos 95% do tempo salvo,
 *   sinal de que os eventos ainda existem); senão mantém o salvo e o marca como
 *   conferido, para não tentar de novo a cada abertura.
 * - Caso contrário, recalcula. Se o recálculo tiver dados, ele vence e é salvo.
 * - Se o recálculo não tiver dados, usa o que estiver salvo; sem nada salvo, "Sem dados".
 */
object DayPersistencePolicy {

    /** Suba ao mudar regras de cálculo que alteram dias já salvos. 2 = contagem de aberturas revisada. */
    const val CURRENT_CALC_VERSION = 2

    /**
     * @param save gravar [result] como dia novo.
     * @param markChecked só atualizar a versão do dia salvo (dados mantidos).
     */
    data class Decision(val result: DayUsage, val save: Boolean, val markChecked: Boolean = false)

    fun needsLive(stored: DayUsage?, storedVersion: Int = CURRENT_CALC_VERSION): Boolean =
        stored?.isFinal != true || storedVersion < CURRENT_CALC_VERSION

    fun choose(stored: DayUsage?, live: DayUsage, storedVersion: Int = CURRENT_CALC_VERSION): Decision = when {
        stored?.isFinal == true && storedVersion >= CURRENT_CALC_VERSION -> Decision(stored, save = false)
        stored?.isFinal == true -> {
            val complete = live.status == DataStatus.Available &&
                live.isFinal &&
                live.totalMillis * 100 >= stored.totalMillis * 95
            if (complete) Decision(live, save = true) else Decision(stored, save = false, markChecked = true)
        }
        live.status == DataStatus.Available -> Decision(live, save = true)
        stored != null -> Decision(stored, save = false)
        else -> Decision(live, save = false)
    }
}
