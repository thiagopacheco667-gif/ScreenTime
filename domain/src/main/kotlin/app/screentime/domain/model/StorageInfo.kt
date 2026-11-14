package app.screentime.domain.model

import java.time.Instant
import java.time.LocalDate

/** Situação do histórico salvo no aparelho. */
data class StorageInfo(
    val daysStored: Int,
    val oldestDate: LocalDate?,
    val lastBackgroundSyncAt: Instant?,
)
