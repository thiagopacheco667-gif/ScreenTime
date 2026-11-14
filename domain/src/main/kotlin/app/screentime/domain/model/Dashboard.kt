package app.screentime.domain.model

import kotlin.math.roundToInt

/**
 * Dados do painel.
 *
 * @param yesterdaySameTime ontem cortado no mesmo horário de agora; é a base da comparação.
 * @param yesterday ontem inteiro.
 */
data class Dashboard(
    val today: DayUsage,
    val yesterdaySameTime: DayUsage,
    val yesterday: DayUsage,
    val comparison: Comparison,
)

/** Resultado da comparação de hoje com ontem até o mesmo horário. */
sealed interface Comparison {
    /** Variação percentual arredondada; positiva = mais uso hoje. */
    data class Percent(val value: Int) : Comparison

    /** Ontem não teve uso até este horário: não há base para porcentagem. */
    data object NoBaseline : Comparison

    /** O sistema não tem dados de ontem. */
    data object Unavailable : Comparison

    companion object {
        fun of(today: DayUsage, baseline: DayUsage): Comparison = when {
            baseline.status == DataStatus.Unavailable -> Unavailable
            baseline.totalMillis <= 0L -> NoBaseline
            else -> Percent(
                ((today.totalMillis - baseline.totalMillis) * 100.0 / baseline.totalMillis).roundToInt(),
            )
        }
    }
}
