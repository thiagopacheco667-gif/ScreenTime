package app.screentime.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.Comparison
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.Dashboard
import app.screentime.presentation.R
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatLongDate
import app.screentime.ui.chart.HourlyBarChart
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.InfoCard
import app.screentime.ui.component.UsageBar

private const val TOP_APPS = 5

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onOpenDiagnostics: () -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible()
        onPauseOrDispose { viewModel.onHidden() }
    }

    when (val s = state) {
        DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        DashboardUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is DashboardUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.diag_error_title), style = MaterialTheme.typography.headlineSmall)
            Text(s.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = viewModel::refresh) { Text(stringResource(R.string.diag_retry)) }
        }
        is DashboardUiState.Content -> DashboardContent(s.dashboard, onOpenDiagnostics)
    }
}

@Composable
private fun DashboardContent(dashboard: Dashboard, onOpenDiagnostics: () -> Unit) {
    val today = dashboard.today
    val zone = today.zoneId
    val currentHour = today.computedAt.atZone(zone).hour

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabeçalho: data, total e comparação.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = formatLongDate(today.date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(today.totalMillis),
                style = MaterialTheme.typography.displayLarge,
            )
            ComparisonPill(dashboard)
            Text(
                text = yesterdayLine(dashboard),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoCard(title = stringResource(R.string.dash_hourly_title)) {
            val peak = today.hourlyMillis.maxOrNull() ?: 0L
            HourlyBarChart(
                values = today.hourlyMillis,
                highlightHour = currentHour,
                contentDescription = stringResource(R.string.dash_hourly_a11y, formatDuration(today.totalMillis)),
            )
            if (peak > 0L) {
                val peakHour = today.hourlyMillis.indexOf(peak)
                Text(
                    text = stringResource(R.string.dash_peak_hour, peakHour, formatDuration(peak)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        InfoCard(title = stringResource(R.string.dash_top_title)) {
            val top = today.apps.take(TOP_APPS)
            if (top.isEmpty()) {
                Text(
                    stringResource(R.string.diag_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val max = top.first().foregroundMillis.coerceAtLeast(1L)
                top.forEach { app -> TopAppRow(app, fraction = app.foregroundMillis.toFloat() / max) }
            }
        }

        Text(
            text = stringResource(R.string.dash_updated_at, formatClock(today.computedAt, zone)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenDiagnostics) {
            Text(stringResource(R.string.dash_open_diagnostics))
        }
    }
}

@Composable
private fun ComparisonPill(dashboard: Dashboard) {
    val time = formatClock(dashboard.today.computedAt, dashboard.today.zoneId)
    val (text, container, content) = when (val c = dashboard.comparison) {
        is Comparison.Percent -> when {
            c.value > 0 -> Triple(
                stringResource(R.string.dash_cmp_more, c.value, time),
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
            )
            c.value < 0 -> Triple(
                stringResource(R.string.dash_cmp_less, -c.value, time),
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurface,
            )
            else -> Triple(
                stringResource(R.string.dash_cmp_same, time),
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurface,
            )
        }
        Comparison.NoBaseline -> Triple(
            stringResource(R.string.dash_cmp_no_baseline, time),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Comparison.Unavailable -> Triple(
            stringResource(R.string.dash_cmp_unavailable),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Pill(text, container, content)
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun yesterdayLine(dashboard: Dashboard): String =
    if (dashboard.yesterday.status == DataStatus.Unavailable) {
        stringResource(R.string.dash_yesterday_unavailable)
    } else {
        stringResource(R.string.dash_yesterday_total, formatDuration(dashboard.yesterday.totalMillis))
    }

@Composable
private fun TopAppRow(app: AppUsage, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
