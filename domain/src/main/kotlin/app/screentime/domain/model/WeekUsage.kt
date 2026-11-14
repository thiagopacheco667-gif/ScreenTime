package app.screentime.domain.model

import java.time.LocalDate

/**
 * Um dia da semana.
 * @param millis tempo de tela; nulo quando não há dados (dia futuro ou apagado pelo sistema).
 */
data class WeekDay(
    val date: LocalDate,
    val millis: Long?,
    val isFuture: Boolean,
)

/**
 * Semana de segunda a domingo.
 *
 * @param averageMillis média dos dias com dados (inclui hoje até agora); nulo se nenhum dia tem dados.
 * @param peakDay dia de maior uso; nulo se nenhum dia teve uso.
 * @param topApps apps somados na semana, do mais usado para o menos usado.
 */
data class WeekUsage(
    val start: LocalDate,
    val days: List<WeekDay>,
    val totalMillis: Long,
    val averageMillis: Long?,
    val daysWithData: Int,
    val peakDay: WeekDay?,
    val topApps: List<AppUsage>,
) {
    val end: LocalDate get() = start.plusDays(6)
}
