package app.screentime.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.screentime.domain.model.DataStatus
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.dayLabel
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDayMonth
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatWeekdayShort
import app.screentime.ui.chart.DayBarsChart
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.UsageBar
import app.screentime.presentation.common.HeroCard
import app.screentime.presentation.common.SectionCard
import app.screentime.presentation.common.animatedMillis
import app.screentime.ui.icons.ScreenTimeIcons
import kotlin.math.roundToInt

@Composable
fun AppDetailRoute(
    viewModel: AppDetailViewModel,
    onBack: () -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when (val s = state) {
        AppDetailUiState.Loading -> LoadingState()
        AppDetailUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is AppDetailUiState.Error -> ErrorState(s.message, viewModel::refresh)
        is AppDetailUiState.Content -> AppDetailContent(s, onBack)
    }
}

@Composable
private fun AppDetailContent(state: AppDetailUiState.Content, onBack: () -> Unit) {
    val d = state.detail
    val usage = d.usage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(ScreenTimeIcons.Back, contentDescription = stringResource(R.string.nav_back))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = d.packageName, label = d.displayName, size = 60.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = d.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = d.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HeroCard {
            Text(
                "${dayLabel(d.date, state.today)} · ${formatDayMonth(d.date)}".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                d.dayStatus == DataStatus.Unavailable -> Text(
                    stringResource(R.string.apps_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                usage == null -> Text(stringResource(R.string.detail_not_used), style = MaterialTheme.typography.headlineSmall)
                else -> {
                    Text(formatDuration(animatedMillis(usage.foregroundMillis)), style = MaterialTheme.typography.displayMedium)
                    val last = usage.lastUsedAt?.let { formatClock(it, state.zone) } ?: "—"
                    Text(
                        text = pluralStringResource(R.plurals.detail_sessions, usage.sessionCount, usage.sessionCount, last),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    d.shareOfDay?.let { share ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Text(
                                text = stringResource(R.string.detail_share, (share * 100).roundToInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            UsageBar(share, delayMillis = 300)
                        }
                    }
                }
            }
        }

        SectionCard {
            Column {
                Text(stringResource(R.string.detail_last7_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = d.averageMillis?.let { stringResource(R.string.detail_average, formatDuration(it)) }
                        ?: stringResource(R.string.detail_average_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DayBarsChart(
                values = d.lastDays.map { it.millis },
                labels = d.lastDays.map { formatWeekdayShort(it.date) },
                selectedIndex = d.lastDays.lastIndex,
                averageMillis = d.averageMillis,
                contentDescription = stringResource(R.string.detail_chart_a11y, d.displayName),
                height = 150.dp,
            )
            Text(
                stringResource(R.string.detail_chart_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
