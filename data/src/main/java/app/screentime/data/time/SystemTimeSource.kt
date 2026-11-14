package app.screentime.data.time

import app.screentime.domain.repository.TimeSource
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Relógio do aparelho. O fuso é lido a cada chamada para acompanhar mudanças. */
@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
