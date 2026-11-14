package app.screentime.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Situação dos dados de um dia.
 * - [Available]: calculado a partir dos eventos do sistema.
 * - [Unavailable]: o sistema não tem eventos desse dia (já apagou ou o dia ainda não chegou).
 *   Deve aparecer como "Sem dados", nunca como zero.
 */
enum class DataStatus { Available, Unavailable }

/**
 * Uso de um dia local.
 *
 * @param totalMillis tempo de tela do dia: união de todas as sessões, sem contar
 *   duas vezes o mesmo intervalo.
 * @param apps uso por app, do mais usado para o menos usado.
 * @param hourlyMillis 24 posições, uma por hora local (0 = 00:00–00:59).
 * @param isFinal true quando o dia já terminou.
 * @param computedAt instante do cálculo (exibido como "Atualizado às ...").
 */
data class DayUsage(
    val date: LocalDate,
    val zoneId: ZoneId,
    val status: DataStatus,
    val totalMillis: Long,
    val apps: List<AppUsage>,
    val hourlyMillis: List<Long>,
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
            hourlyMillis = List(24) { 0L },
            isFinal = false,
            computedAt = now,
        )
    }
}
