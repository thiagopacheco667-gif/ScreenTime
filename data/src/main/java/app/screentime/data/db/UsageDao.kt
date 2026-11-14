package app.screentime.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class UsageDao {

    @Query("SELECT * FROM day_summary WHERE epochDay = :epochDay")
    abstract suspend fun day(epochDay: Long): DaySummaryEntity?

    @Query("SELECT * FROM app_usage_daily WHERE epochDay = :epochDay ORDER BY foregroundMillis DESC")
    abstract suspend fun apps(epochDay: Long): List<AppUsageDailyEntity>

    @Query("SELECT * FROM app_metadata WHERE packageName IN (:packages)")
    abstract suspend fun labels(packages: List<String>): List<AppMetadataEntity>

    @Query("UPDATE day_summary SET calcVersion = :version WHERE epochDay = :epochDay")
    abstract suspend fun markChecked(epochDay: Long, version: Int)

    @Query("SELECT COUNT(*) FROM day_summary")
    abstract suspend fun countDays(): Int

    @Query("SELECT MIN(epochDay) FROM day_summary")
    abstract suspend fun oldestDay(): Long?

    @Upsert
    protected abstract suspend fun upsertDay(day: DaySummaryEntity)

    @Query("DELETE FROM app_usage_daily WHERE epochDay = :epochDay")
    protected abstract suspend fun deleteApps(epochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertApps(apps: List<AppUsageDailyEntity>)

    @Upsert
    abstract suspend fun upsertLabels(labels: List<AppMetadataEntity>)

    @Query("DELETE FROM day_summary")
    protected abstract suspend fun deleteAllDays()

    @Query("DELETE FROM app_usage_daily")
    protected abstract suspend fun deleteAllApps()

    @Query("DELETE FROM app_metadata")
    protected abstract suspend fun deleteAllLabels()

    /** Troca o dia inteiro de uma vez, para nunca ficar um dia pela metade no banco. */
    @Transaction
    open suspend fun replaceDay(day: DaySummaryEntity, apps: List<AppUsageDailyEntity>) {
        upsertDay(day)
        deleteApps(day.epochDay)
        if (apps.isNotEmpty()) insertApps(apps)
    }

    @Transaction
    open suspend fun clearAll() {
        deleteAllApps()
        deleteAllDays()
        deleteAllLabels()
    }
}
