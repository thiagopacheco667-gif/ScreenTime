package app.screentime.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resumo de um dia local.
 * @param hourlyCsv 24 valores em milissegundos separados por vírgula (00h a 23h).
 * @param zoneId fuso usado no cálculo; dias antigos mantêm o fuso em que foram calculados.
 */
@Entity(tableName = "day_summary")
data class DaySummaryEntity(
    @PrimaryKey val epochDay: Long,
    val totalMillis: Long,
    val hourlyCsv: String,
    val isFinal: Boolean,
    val zoneId: String,
    val computedAt: Long,
)

/** Tempo de um app em um dia. */
@Entity(
    tableName = "app_usage_daily",
    primaryKeys = ["epochDay", "packageName"],
    indices = [Index("epochDay")],
)
data class AppUsageDailyEntity(
    val epochDay: Long,
    val packageName: String,
    val foregroundMillis: Long,
    val sessionCount: Int,
    val lastUsedAt: Long?,
)

/** Nome do app, guardado para o histórico continuar legível depois de uma desinstalação. */
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val updatedAt: Long,
)
