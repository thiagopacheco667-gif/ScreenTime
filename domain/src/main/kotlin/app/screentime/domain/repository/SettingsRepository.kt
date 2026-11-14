package app.screentime.domain.repository

import app.screentime.domain.model.AppearanceSettings
import kotlinx.coroutines.flow.Flow

/** Preferências de aparência, salvas só no aparelho. Implementado no módulo :data. */
interface SettingsRepository {
    val settings: Flow<AppearanceSettings>

    /** Leitura pontual (widgets e workers, que não observam o Flow). */
    suspend fun current(): AppearanceSettings

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings)
}
