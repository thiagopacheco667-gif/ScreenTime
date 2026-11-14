package app.screentime.widget

import android.content.Context
import app.screentime.domain.repository.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redesenha os widgets da tela inicial.
 * Etapa 8: ainda não há widgets; a etapa 9 preenche esta função com updateAll() do Glance.
 */
@Singleton
class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {
    override suspend fun updateAll() {
        // Widgets chegam na etapa 9.
        context.packageName
    }
}
