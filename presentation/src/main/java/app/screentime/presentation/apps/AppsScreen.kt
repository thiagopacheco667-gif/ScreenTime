package app.screentime.presentation.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.dayLabel
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDuration
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.UsageBar
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AppsRoute(
    viewModel: AppsViewModel,
    onOpenApp: (packageName: String, date: LocalDate) -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when (val s = state) {
        AppsUiState.Loading -> LoadingState()
        AppsUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is AppsUiState.Error -> ErrorState(s.message, viewModel::refresh)
        is AppsUiState.Content -> AppsContent(s, onSelect = viewModel::select, onOpenApp = onOpenApp)
    }
}

@Composable
private fun AppsContent(
    state: AppsUiState.Content,
    onSelect: (LocalDate) -> Unit,
    onOpenApp: (String, LocalDate) -> Unit,
) {
    val usage = state.usage
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.apps_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true,
            ) {
                items(state.days.asReversed(), key = { it.toEpochDay() }) { day ->
                    FilterChip(
                        selected = day == state.selected,
                        onClick = { onSelect(day) },
                        label = { Text(dayLabel(day, state.today)) },
                    )
                }
            }
        }

        dayAppItems(usage, state.selected, onOpenApp)
    }
}

/** Itens da lista de apps de um dia (compartilhado com a tela de um dia da semana). */
internal fun LazyListScope.dayAppItems(
    usage: DayUsage,
    date: LocalDate,
    onOpenApp: (String, LocalDate) -> Unit,
) {
    when {
        usage.status == DataStatus.Unavailable -> item {
            Text(
                text = stringResource(R.string.apps_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        usage.apps.isEmpty() -> item {
            Text(
                text = stringResource(R.string.apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        else -> {
            item {
                Text(
                    text = stringResource(R.string.apps_day_total, formatDuration(usage.totalMillis), usage.apps.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
            }
            val max = usage.apps.first().foregroundMillis.coerceAtLeast(1L)
            items(usage.apps, key = { it.packageName }) { app ->
                AppListRow(
                    app = app,
                    fraction = app.foregroundMillis.toFloat() / max,
                    zone = usage.zoneId,
                    onClick = { onOpenApp(app.packageName, date) },
                )
            }
        }
    }
}

@Composable
private fun AppListRow(app: AppUsage, fraction: Float, zone: ZoneId, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, label = app.displayName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatDuration(app.foregroundMillis), style = MaterialTheme.typography.titleSmall)
                }
                UsageBar(fraction)
                val last = app.lastUsedAt?.let { formatClock(it, zone) } ?: "—"
                Text(
                    text = pluralStringResource(R.plurals.diag_sessions, app.sessionCount, app.sessionCount, last),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, start = 54.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
