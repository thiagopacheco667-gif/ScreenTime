package app.screentime.domain.model

import java.time.LocalDate

/** Tempo de um dia; [millis] nulo quando o sistema não tem dados desse dia. */
data class DayValue(val date: LocalDate, val millis: Long?)

/**
 * Detalhe de um app em um dia, com os 7 dias que terminam nele.
 *
 * @param usage uso no dia escolhido; nulo se o app não foi aberto nesse dia.
 * @param dayTotalMillis tempo de tela total do dia, para calcular a fatia do app.
 * @param lastDays 7 dias, do mais antigo para o dia escolhido.
 * @param averageMillis média dos dias com dados (dias sem uso contam como zero); nulo se nenhum dia tem dados.
 */
data class AppDetail(
    val packageName: String,
    val label: String?,
    val date: LocalDate,
    val dayStatus: DataStatus,
    val usage: AppUsage?,
    val dayTotalMillis: Long,
    val lastDays: List<DayValue>,
    val averageMillis: Long?,
) {
    val displayName: String get() = label ?: packageName

    /** Fatia do tempo de tela do dia (0..1), ou nulo se o dia não teve uso. */
    val shareOfDay: Float?
        get() = if (dayTotalMillis > 0 && usage != null) {
            (usage.foregroundMillis.toFloat() / dayTotalMillis).coerceIn(0f, 1f)
        } else {
            null
        }
}
