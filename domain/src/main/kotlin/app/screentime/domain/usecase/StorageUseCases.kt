package app.screentime.domain.usecase

import app.screentime.domain.model.StorageInfo
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.UsageRepository
import app.screentime.domain.repository.WidgetUpdater

/**
 * Sincronização em segundo plano: salva os dias recentes e redesenha os widgets.
 * Sem acesso ao uso, não faz nada (e os widgets mostram o aviso de permissão).
 */
class SyncUsageUseCase(
    private val repository: UsageRepository,
    private val accessChecker: UsageAccessChecker,
    private val widgetUpdater: WidgetUpdater,
    private val time: TimeSource,
) {
    suspend operator fun invoke() {
        if (accessChecker.hasUsageAccess()) {
            repository.syncRecentDays()
            repository.recordBackgroundSync(time.now())
        }
        widgetUpdater.updateAll()
    }
}

class GetStorageInfoUseCase(private val repository: UsageRepository) {
    suspend operator fun invoke(): StorageInfo = repository.storageInfo()
}

class ClearHistoryUseCase(
    private val repository: UsageRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke() {
        repository.clearHistory()
        widgetUpdater.updateAll()
    }
}
