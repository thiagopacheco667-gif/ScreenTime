package app.screentime.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Recalcula quando o usuário muda a hora ou o fuso, para "hoje" e os widgets
 * acompanharem a data nova. Estes dois avisos são dos poucos que o Android
 * ainda entrega a apps fechados.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> SyncScheduler.syncNow(context)
        }
    }
}
