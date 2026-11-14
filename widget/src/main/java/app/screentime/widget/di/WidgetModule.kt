package app.screentime.widget.di

import app.screentime.domain.repository.WidgetUpdater
import app.screentime.widget.GlanceWidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater
}
