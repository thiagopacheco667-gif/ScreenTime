package app.screentime.data.di

import app.screentime.data.repository.UsageRepositoryImpl
import app.screentime.data.settings.SettingsRepositoryImpl
import app.screentime.data.time.SystemTimeSource
import app.screentime.domain.engine.UsageCalculator
import app.screentime.domain.repository.SettingsRepository
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.domain.repository.WidgetUpdater
import app.screentime.domain.repository.UsageRepository
import app.screentime.domain.usecase.ClearHistoryUseCase
import app.screentime.domain.usecase.GetAppDetailUseCase
import app.screentime.domain.usecase.GetStorageInfoUseCase
import app.screentime.domain.usecase.SyncUsageUseCase
import app.screentime.domain.usecase.GetDashboardUseCase
import app.screentime.domain.usecase.GetDayEventsUseCase
import app.screentime.domain.usecase.GetDayUsageUseCase
import app.screentime.domain.usecase.GetWeekUsageUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {

    @Binds
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

/**
 * As classes do :domain são Kotlin puro e não usam anotações do Dagger,
 * então são criadas aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainProviders {

    @Provides
    @Singleton
    fun provideUsageCalculator(): UsageCalculator = UsageCalculator()

    @Provides
    fun provideGetDayUsage(repository: UsageRepository) = GetDayUsageUseCase(repository)

    @Provides
    fun provideGetDayEvents(repository: UsageRepository) = GetDayEventsUseCase(repository)

    @Provides
    fun provideGetAppDetail(repository: UsageRepository) = GetAppDetailUseCase(repository)

    @Provides
    fun provideGetWeekUsage(repository: UsageRepository, time: TimeSource) =
        GetWeekUsageUseCase(repository, time)

    @Provides
    fun provideSyncUsage(
        repository: UsageRepository,
        accessChecker: UsageAccessChecker,
        widgetUpdater: WidgetUpdater,
        time: TimeSource,
    ) = SyncUsageUseCase(repository, accessChecker, widgetUpdater, time)

    @Provides
    fun provideGetStorageInfo(repository: UsageRepository) = GetStorageInfoUseCase(repository)

    @Provides
    fun provideClearHistory(repository: UsageRepository, widgetUpdater: WidgetUpdater) =
        ClearHistoryUseCase(repository, widgetUpdater)

    @Provides
    fun provideGetDashboard(repository: UsageRepository, time: TimeSource) =
        GetDashboardUseCase(repository, time)
}
