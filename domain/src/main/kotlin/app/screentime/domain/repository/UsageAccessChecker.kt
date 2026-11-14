package app.screentime.domain.repository

/**
 * Informa se o usuário concedeu o acesso especial "Acesso ao uso".
 * Implementado no módulo :permissions.
 */
interface UsageAccessChecker {
    fun hasUsageAccess(): Boolean
}
