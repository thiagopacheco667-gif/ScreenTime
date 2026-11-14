package app.screentime.widget.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import app.screentime.domain.model.DataStatus
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.repository.UsageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** O que os widgets mostram. */
sealed interface WidgetData {
    /** Sem acesso ao uso: o widget pede para abrir o app. */
    data object NoAccess : WidgetData

    /**
     * @param hourlyToday uso por hora de hoje (24 posições).
     * @param hourlyYesterday uso por hora de ontem, ou nulo se não houver dados.
     */
    data class Ready(
        val totalMillis: Long,
        val top: List<WidgetApp>,
        val updatedAt: Instant,
        val zone: ZoneId,
        val hourlyToday: List<Long>,
        val hourlyYesterday: List<Long>?,
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
 * Lê o dia de hoje para os widgets.
 * Usa o resumo salvo pelo worker (rápido). Se ainda não houver resumo de hoje
 * (widget recém-adicionado ou dia que acabou de virar), calcula uma vez e salva.
 */
class WidgetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository,
    private val accessChecker: UsageAccessChecker,
    private val time: TimeSource,
) {
    suspend fun load(topCount: Int): WidgetData {
        if (!accessChecker.hasUsageAccess()) return WidgetData.NoAccess
        val today = time.today()
        val day = try {
            repository.cachedDayUsage(today) ?: repository.dayUsage(today)
        } catch (_: UsageAccessMissingException) {
            return WidgetData.NoAccess
        }
        return WidgetData.Ready(
            totalMillis = day.totalMillis,
            top = day.apps.take(topCount).map {
                WidgetApp(it.packageName, it.displayName, it.foregroundMillis, icon(it.packageName))
            },
            updatedAt = day.computedAt,
            zone = day.zoneId,
            hourlyToday = day.hourlyMillis,
            hourlyYesterday = yesterdayHourly(today.minusDays(1)),
        )
    }

    private suspend fun yesterdayHourly(date: java.time.LocalDate): List<Long>? = try {
        val day = repository.cachedDayUsage(date) ?: repository.dayUsage(date)
        day.hourlyMillis.takeIf { day.status == DataStatus.Available }
    } catch (_: UsageAccessMissingException) {
        null
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
}
