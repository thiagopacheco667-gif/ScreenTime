package app.screentime.domain.repository

/**
 * Pede aos widgets da tela inicial que se redesenhem com os dados salvos.
 * Implementado no módulo :widget; chamado pelos workers e pelo app.
 */
interface WidgetUpdater {
    suspend fun updateAll()
}
