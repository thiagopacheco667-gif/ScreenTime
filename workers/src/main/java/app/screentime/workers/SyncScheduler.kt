package app.screentime.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Agenda a sincronização. Sem restrição de rede ou bateria: o trabalho é leve e local. */
object SyncScheduler {

    private const val PERIODIC = "usage-sync-periodic"
    private const val NOW = "usage-sync-now"
    private const val INTERVAL_MINUTES = 30L

    /** Agenda a sincronização periódica. KEEP: chamar de novo não duplica nem reinicia o relógio. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncUsageWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Sincroniza agora (ao abrir o app, ao mudar hora ou fuso). */
    fun syncNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<SyncUsageWorker>().build())
    }
}
