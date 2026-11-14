package app.screentime.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Situação dos dados de um dia.
 * - [Available]: calculado a partir dos eventos do sistema.
 * - [Unavailable]: o sistema não tem mais eventos desse dia e ele nunca foi salvo.
 *   Deve aparecer como "Sem dados", nunca como zero.
 */
enum class DataStatus { Available, Unavailable }

/**
 * Uso total de um dia local.
 *
 * @param isFinal true quando o dia já terminou e foi calculado por completo;
 *   dias finais não são recalculados, porque o sistema apaga eventos antigos.
 * @param computedAt instante do cálculo (exibido como "Atualizado às ...").
 */
data class DayUsage(
    val date: LocalDate,
    val zoneId: ZoneId,
    val status: DataStatus,
    val totalMillis: Long,
    val apps: List<AppUsage>,
    val isFinal: Boolean,
    val computedAt: Instant,
) {
    /** Apps ordenados do mais usado para o menos usado. */
    val appsByUsage: List<AppUsage> get() = apps.sortedByDescending { it.foregroundMillis }

    companion object {
        fun unavailable(date: LocalDate, zoneId: ZoneId, now: Instant) = DayUsage(
            date = date,
            zoneId = zoneId,
            status = DataStatus.Unavailable,
            totalMillis = 0L,
            apps = emptyList(),
            isFinal = false,
            computedAt = now,
        )
    }
}
