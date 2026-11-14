package app.screentime.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco local. Fica só no aparelho: o app não tem internet e o backup está desligado.
 * Ao mudar o esquema, suba a versão e adicione uma migração (os esquemas ficam em data/schemas).
 */
@Database(
    entities = [DaySummaryEntity::class, AppUsageDailyEntity::class, AppMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ScreenTimeDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        const val NAME = "screentime.db"
    }
}
