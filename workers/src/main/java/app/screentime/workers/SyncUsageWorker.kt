package app.screentime.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.screentime.domain.usecase.SyncUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Salva os dias recentes no banco e atualiza os widgets.
 * Roda a cada 30 minutos (o Android pode atrasar no modo Doze ou com economia de bateria).
 */
@HiltWorker
class SyncUsageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncUsage: SyncUsageUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        syncUsage()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Falha na sincronização", e)
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private companion object {
        const val TAG = "SyncUsageWorker"
        const val MAX_RETRIES = 3
    }
}
