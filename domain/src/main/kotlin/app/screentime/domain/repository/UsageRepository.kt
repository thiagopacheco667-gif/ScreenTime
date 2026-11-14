package app.screentime.domain.repository

import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.StorageInfo
import app.screentime.domain.model.UsageEvent
import java.time.Instant
import java.time.LocalDate

/**
 * Fonte do uso por dia. Implementado no módulo :data.
 * As funções de leitura lançam [UsageAccessMissingException] se o acesso ao uso
 * não estiver concedido e o dia não estiver salvo.
 */
interface UsageRepository {

    /**
     * Uso de um dia local. Dias encerrados vêm do banco; os demais são
     * recalculados dos eventos e salvos.
     * @param upTo corta o dia neste instante (para "ontem até o mesmo horário");
     *   nesse caso o cálculo é sempre direto dos eventos e nada é salvo.
     */
    suspend fun dayUsage(date: LocalDate, upTo: Instant? = null): DayUsage

    /** Só o dia salvo no banco, sem recalcular (leitura rápida para os widgets). */
    suspend fun cachedDayUsage(date: LocalDate): DayUsage?

    /** Eventos brutos do dia, do mais antigo para o mais recente (tela de diagnóstico). */
    suspend fun dayEvents(date: LocalDate): List<UsageEvent>

    /** Nome exibido do app, ou nulo se o sistema não o conhece (desinstalado ou invisível). */
    suspend fun appLabel(packageName: String): String?

    /**
     * Salva os dias recentes que o sistema ainda tem, antes que os eventos sejam apagados.
     * @return quantos dias foram lidos.
     */
    suspend fun syncRecentDays(): Int

    suspend fun recordBackgroundSync(at: Instant)

    suspend fun storageInfo(): StorageInfo

    /** Apaga todo o histórico salvo no app. Não afeta os dados do sistema. */
    suspend fun clearHistory()
}

/** O usuário não concedeu (ou revogou) o acesso ao uso. */
class UsageAccessMissingException : IllegalStateException("Acesso ao uso não concedido")
