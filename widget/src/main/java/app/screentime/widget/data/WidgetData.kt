package app.screentime.widget.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import app.screentime.domain.color.ImagePalette
import app.screentime.domain.model.AppearanceSettings
import app.screentime.domain.model.Comparison
import app.screentime.domain.repository.SettingsRepository
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.repository.UsageRepository
import app.screentime.domain.repository.WidgetBackgroundStore
import app.screentime.domain.usecase.SyncUsageUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** O que os widgets mostram. */
sealed interface WidgetData {
    val settings: AppearanceSettings
    val backgroundPath: String?
    val backgroundVersion: Long

    /** Cores tiradas da imagem de fundo (nulo sem imagem). */
    val imagePalette: ImagePalette?

    /** Sem acesso ao uso: o widget pede para abrir o app. */
    data class NoAccess(
        override val settings: AppearanceSettings,
        override val backgroundPath: String?,
        override val backgroundVersion: Long,
        override val imagePalette: ImagePalette?,
    ) : WidgetData

    /**
     * @param hourlyToday tempo de tela por hora de hoje (24 posições, em ms).
     * @param comparisonPercent variação contra ontem até o mesmo horário; nulo se não houver base.
     */
    data class Ready(
        val totalMillis: Long,
        val top: List<WidgetApp>,
        val updatedAt: Instant,
        val zone: ZoneId,
        val hourlyToday: List<Long>,
        val comparisonPercent: Int?,
        override val settings: AppearanceSettings,
        override val backgroundPath: String?,
        override val backgroundVersion: Long,
        override val imagePalette: ImagePalette?,
    ) : WidgetData {
        val currentHour: Int get() = updatedAt.atZone(zone).hour
    }
}

data class WidgetApp(
    val packageName: String,
    val name: String,
    val millis: Long,
    val icon: Bitmap?,
)

/**
 * Lê o dia de hoje para os widgets. Recalcula a partir dos eventos a cada desenho,
 * para nunca mostrar um resumo antigo quando a atualização em segundo plano não
 * rodou (o HyperOS costuma segurá-la). O recálculo também salva o resumo.
 */
class WidgetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository,
    private val accessChecker: UsageAccessChecker,
    private val time: TimeSource,
    private val settingsRepository: SettingsRepository,
    private val backgroundStore: WidgetBackgroundStore,
) {
    suspend fun load(topCount: Int = AppearanceSettings.MAX_WIDGET_APPS): WidgetData {
        val settings = settingsRepository.current()
        val path = backgroundStore.imagePath()
        val version = backgroundStore.version()
        val imagePalette = if (path != null) runCatching { backgroundStore.palette() }.getOrNull() else null
        if (!accessChecker.hasUsageAccess()) return WidgetData.NoAccess(settings, path, version, imagePalette)

        val today = time.today()
        val day = try {
            repository.dayUsage(today)
        } catch (_: UsageAccessMissingException) {
            return WidgetData.NoAccess(settings, path, version, imagePalette)
        }
        val comparison = runCatching {
            val zone = time.zone()
            val sameTime = today.minusDays(1)
                .atTime(day.computedAt.atZone(zone).toLocalTime())
                .atZone(zone)
                .toInstant()
            Comparison.of(day, repository.dayUsage(today.minusDays(1), upTo = sameTime))
        }.getOrNull()

        return WidgetData.Ready(
            totalMillis = day.totalMillis,
            top = day.apps.take(topCount).map {
                WidgetApp(it.packageName, it.displayName, it.foregroundMillis, icon(it.packageName))
            },
            updatedAt = day.computedAt,
            zone = day.zoneId,
            hourlyToday = day.hourlyMillis,
            comparisonPercent = (comparison as? Comparison.Percent)?.value,
            settings = settings,
            backgroundPath = path,
            backgroundVersion = version,
            imagePalette = imagePalette,
        )
    }

    private fun icon(packageName: String): Bitmap? = try {
        context.packageManager.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private companion object {
        // Pequeno de propósito: widgets têm limite de memória para imagens.
        const val ICON_PX = 72
    }
}

/** Acesso ao Hilt a partir do Glance, que não injeta dependências sozinho. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetDataSource(): WidgetDataSource
    fun syncUsage(): SyncUsageUseCase
}
