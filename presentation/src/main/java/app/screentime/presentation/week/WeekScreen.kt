package app.screentime.presentation.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.WeekDay
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.dayLabel
import app.screentime.presentation.common.formatDayMonth
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatRange
import app.screentime.presentation.common.formatWeekdayLong
import app.screentime.presentation.common.formatWeekdayShort
import app.screentime.ui.chart.DailyBarChart
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.InfoCard
import app.screentime.ui.component.UsageBar
import app.screentime.ui.icons.ScreenTimeIcons
import java.time.LocalDate

private const val TOP_APPS = 5

@Composable
fun WeekRoute(
    viewModel: WeekViewModel,
    onOpenDay: (LocalDate) -> Unit,
    onOpenApp: (String, LocalDate) -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when (val s = state) {
        WeekUiState.Loading -> LoadingState()
        WeekUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is WeekUiState.Error -> ErrorState(s.message, viewModel::refresh)
        is WeekUiState.Content -> WeekContent(
            state = s,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            onOpenDay = onOpenDay,
            onOpenApp = onOpenApp,
        )
    }
}

@Composable
private fun WeekContent(
    state: WeekUiState.Content,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenApp: (String, LocalDate) -> Unit,
) {
    val week = state.week
    val peakIndex = week.peakDay?.let { peak -> week.days.indexOfFirst { it.date == peak.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.week_title), style = MaterialTheme.typography.headlineMedium)

        // Navegação entre semanas.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.canGoBack) {
                Icon(ScreenTimeIcons.Back, contentDescription = stringResource(R.string.week_prev))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (state.offset) {
                        0 -> stringResource(R.string.week_this)
                        -1 -> stringResource(R.string.week_last)
                        else -> formatRange(week.start, week.end)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.offset >= -1) {
                    Text(
                        text = formatRange(week.start, week.end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(ScreenTimeIcons.Forward, contentDescription = stringResource(R.string.week_next))
            }
        }

        if (week.daysWithData == 0) {
            Text(
                text = stringResource(R.string.week_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatDuration(week.totalMillis), style = MaterialTheme.typography.displayMedium)
                week.averageMillis?.let {
                    Text(
                        stringResource(R.string.week_average, formatDuration(it)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    stringResource(R.string.week_days_with_data, week.daysWithData),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        InfoCard(title = formatRange(week.start, week.end)) {
            DailyBarChart(
                values = week.days.map { it.millis },
                labels = week.days.map { formatWeekdayShort(it.date) },
                highlightIndex = peakIndex,
                averageMillis = week.averageMillis,
                contentDescription = stringResource(
                    R.string.week_chart_a11y,
                    formatRange(week.start, week.end),
                    formatDuration(week.totalMillis),
                ),
            )
            week.peakDay?.let { peak ->
                Text(
                    text = stringResource(R.string.week_peak, formatWeekdayLong(peak.date), formatDuration(peak.millis ?: 0L)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.week_average_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoCard(title = stringResource(R.string.week_days_title)) {
            week.days.forEachIndexed { i, day ->
                DayRow(day, today = state.today, onClick = { onOpenDay(day.date) })
                if (i < week.days.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (week.topApps.isNotEmpty()) {
            InfoCard(title = stringResource(R.string.week_top_title)) {
                val top = week.topApps.take(TOP_APPS)
                val max = top.first().foregroundMillis.coerceAtLeast(1L)
                // Abre o detalhe no último dia da semana que já passou.
                val detailDate = minOf(week.end, state.today)
                top.forEach { app ->
                    WeekAppRow(app, app.foregroundMillis.toFloat() / max) { onOpenApp(app.packageName, detailDate) }
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: WeekDay, today: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !day.isFuture, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = dayLabel(day.date, today).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatDayMonth(day.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (day.isFuture) {
                stringResource(R.string.week_day_future)
            } else {
                day.millis?.let { formatDuration(it) } ?: stringResource(R.string.week_day_no_data)
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (day.millis == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WeekAppRow(app: AppUsage, fraction: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        }
    }
}
