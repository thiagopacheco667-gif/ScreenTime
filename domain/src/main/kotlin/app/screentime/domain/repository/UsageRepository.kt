package app.screentime.domain.repository

import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.UsageEvent
import java.time.Instant
import java.time.LocalDate

/**
 * Fonte do uso por dia. Implementado no módulo :data.
 * As funções lançam [UsageAccessMissingException] se o acesso ao uso não estiver concedido.
 */
interface UsageRepository {

    /**
     * Uso de um dia local.
     * @param upTo corta o dia neste instante (para "ontem até o mesmo horário").
     */
    suspend fun dayUsage(date: LocalDate, upTo: Instant? = null): DayUsage

    /** Eventos brutos do dia, do mais antigo para o mais recente (tela de diagnóstico). */
    suspend fun dayEvents(date: LocalDate): List<UsageEvent>

    /** Nome exibido do app, ou nulo se o sistema não o conhece (desinstalado ou invisível). */
    suspend fun appLabel(packageName: String): String?
}

/** O usuário não concedeu (ou revogou) o acesso ao uso. */
class UsageAccessMissingException : IllegalStateException("Acesso ao uso não concedido")
