package app.screentime.domain.model

import java.time.Instant

/**
 * Tempo de uso de um aplicativo em um dia local.
 *
 * @param packageName identificador do pacote (ex.: com.whatsapp).
 * @param label nome exibido; pode ser nulo se o app não estiver visível ao sistema.
 * @param foregroundMillis tempo em primeiro plano dentro do dia, em milissegundos.
 * @param sessionCount número de vezes que o app entrou em primeiro plano no dia.
 * @param lastUsedAt último instante em que o app esteve em primeiro plano no dia.
 */
data class AppUsage(
    val packageName: String,
    val label: String?,
    val foregroundMillis: Long,
    val sessionCount: Int,
    val lastUsedAt: Instant?,
) {
    val displayName: String get() = label ?: packageName
}
