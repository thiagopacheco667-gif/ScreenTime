package app.screentime.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco local. Fica só no aparelho: o app não tem internet e o backup está desligado.
 * Ao mudar o esquema, suba a versão e adicione uma migração (os esquemas ficam em data/schemas).
 */
@Database(
    entities = [DaySummaryEntity::class, AppUsageDailyEntity::class, AppMetadataEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ScreenTimeDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        const val NAME = "screentime.db"

        /** v2: coluna calcVersion, para recalcular dias salvos com regras antigas. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_summary ADD COLUMN calcVersion INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
