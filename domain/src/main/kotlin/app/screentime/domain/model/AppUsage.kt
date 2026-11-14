package app.screentime.domain.model

import java.time.Instant

/**
 * Tempo de uso de um aplicativo em um dia local.
 *
 * @param packageName identificador do pacote (ex.: com.whatsapp).
 * @param label nome exibido; pode ser nulo se o app não estiver visível ao sistema.
 * @param foregroundMillis tempo em primeiro plano dentro do dia, em milissegundos.
 * @param sessionCount quantas vezes o app foi aberto no dia (regras em OpenCounter:
 *   voltar em até 30 s sem usar outro app conta como a mesma abertura).
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
