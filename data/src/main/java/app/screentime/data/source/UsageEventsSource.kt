package app.screentime.data.source

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.model.UsageEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lê os eventos de uso do sistema (UsageStatsManager.queryEvents).
 *
 * As constantes novas (ACTIVITY_RESUMED, SCREEN_*, DEVICE_*) são números fixos
 * que o compilador copia para o código, então funcionam em versões antigas:
 * nelas o sistema simplesmente não gera os eventos que ainda não existiam.
 */
@Singleton
class UsageEventsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * @return eventos relevantes entre [startMillis] e [endMillis], em ordem.
     *   Lista vazia se o sistema não devolver nada (sem permissão ou usuário bloqueado).
     */
    @SuppressLint("InlinedApi")
    fun query(startMillis: Long, endMillis: Long): List<UsageEvent> {
        if (endMillis <= startMillis) return emptyList()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val events = try {
            manager.queryEvents(startMillis, endMillis)
        } catch (_: SecurityException) {
            null
        } ?: return emptyList()

        val out = ArrayList<UsageEvent>(512)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            if (!events.getNextEvent(event)) break
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ActivityResumed
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ActivityPaused
                UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventType.ActivityStopped
                UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.ScreenInteractive
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.ScreenNonInteractive
                UsageEvents.Event.KEYGUARD_SHOWN -> UsageEventType.KeyguardShown
                UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KeyguardHidden
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.DeviceShutdown
                UsageEvents.Event.DEVICE_STARTUP -> UsageEventType.DeviceStartup
                else -> null
            } ?: continue
            out += UsageEvent(
                timestamp = event.timeStamp,
                packageName = event.packageName ?: "android",
                className = event.className,
                type = type,
            )
        }
        return out
    }
}
