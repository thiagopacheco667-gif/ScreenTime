package app.screentime.permissions.di

import app.screentime.domain.repository.UsageAccessChecker
import app.screentime.permissions.UsageAccessCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionsModule {

    @Binds
    abstract fun bindUsageAccessChecker(impl: UsageAccessCheckerImpl): UsageAccessChecker
}
