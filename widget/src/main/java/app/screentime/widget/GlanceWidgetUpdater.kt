package app.screentime.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.screentime.domain.repository.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Redesenha todos os widgets do ScreenTime que estiverem na tela inicial. */
@Singleton
class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {
    override suspend fun updateAll() {
        CompactWidget().updateAll(context)
        ExpandedWidget().updateAll(context)
        SquareWidget().updateAll(context)
    }
}
