package app.screentime.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import app.screentime.data.db.ScreenTimeDatabase
import app.screentime.data.db.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScreenTimeDatabase =
        Room.databaseBuilder(context, ScreenTimeDatabase::class.java, ScreenTimeDatabase.NAME).build()

    @Provides
    fun provideUsageDao(db: ScreenTimeDatabase): UsageDao = db.usageDao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("screentime_state", Context.MODE_PRIVATE)
}
