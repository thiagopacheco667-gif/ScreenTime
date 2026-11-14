package app.screentime.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.screentime.widget.ui.runSync
import kotlinx.coroutines.CancellationException

/** Botão ↻ dos widgets: recalcula hoje, salva e redesenha todos os widgets, sem abrir o app. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            runSync(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("RefreshAction", "Falha ao atualizar pelo widget", e)
        }
    }
}
