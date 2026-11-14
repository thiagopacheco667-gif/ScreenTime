package app.screentime.domain.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Relógio e fuso atuais. Lidos a cada chamada (e não guardados), porque o
 * usuário pode mudar o fuso ou a hora com o app aberto.
 */
interface TimeSource {
    fun now(): Instant
    fun zone(): ZoneId
    // atZone().toLocalDate() em vez de LocalDate.ofInstant, que só existe no Android 14+.
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}
