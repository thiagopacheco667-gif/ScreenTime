package app.screentime.presentation.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.model.UsageEventType
import app.screentime.presentation.R
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatClockSeconds
import app.screentime.presentation.common.formatDuration
import app.screentime.ui.component.InfoCard
import java.time.ZoneId

@Composable
fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    onOpenSettings: () -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when (val s = state) {
        DiagnosticsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        DiagnosticsUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is DiagnosticsUiState.Error -> ErrorContent(s.message, onRetry = viewModel::refresh)
        is DiagnosticsUiState.Content -> DiagnosticsContent(
            state = s,
            onRefresh = viewModel::refresh,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.diag_error_title), style = MaterialTheme.typography.headlineSmall)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text(stringResource(R.string.diag_retry)) }
    }
}

@Composable
private fun DiagnosticsContent(
    state: DiagnosticsUiState.Content,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val day = state.today
    val zone = day.zoneId
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = insets.calculateTopPadding() + 24.dp,
            bottom = insets.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.diag_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDuration(day.totalMillis),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = stringResource(
                        R.string.diag_summary,
                        formatClock(day.computedAt, zone),
                        day.apps.size,
                        state.totalEvents,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            InfoCard(title = stringResource(R.string.diag_notice_title)) {
                Text(stringResource(R.string.diag_notice_body), style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRefresh, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.diag_refresh))
                }
                OutlinedButton(onClick = onOpenSettings, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.granted_open_settings))
                }
            }
        }

        item { SectionTitle(stringResource(R.string.diag_apps_title)) }

        if (day.apps.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.diag_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(day.apps, key = { it.packageName }) { app -> AppRow(app, zone) }
        }

        item {
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.diag_events_title, state.recentEvents.size, state.totalEvents))
        }

        items(state.recentEvents) { event -> EventRow(event, zone) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AppRow(app: AppUsage, zone: ZoneId) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDuration(app.foregroundMillis), style = MaterialTheme.typography.titleMedium)
                val last = app.lastUsedAt?.let { formatClock(it, zone) } ?: "—"
                Text(
                    text = pluralStringResource(R.plurals.diag_sessions, app.sessionCount, app.sessionCount, last),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun EventRow(event: UsageEvent, zone: ZoneId) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = formatClockSeconds(event.timestamp, zone),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(event.type.labelRes()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = event.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun UsageEventType.labelRes(): Int = when (this) {
    UsageEventType.ActivityResumed -> R.string.event_resumed
    UsageEventType.ActivityPaused -> R.string.event_paused
    UsageEventType.ActivityStopped -> R.string.event_stopped
    UsageEventType.ScreenInteractive -> R.string.event_screen_on
    UsageEventType.ScreenNonInteractive -> R.string.event_screen_off
    UsageEventType.KeyguardShown -> R.string.event_keyguard_shown
    UsageEventType.KeyguardHidden -> R.string.event_keyguard_hidden
    UsageEventType.DeviceShutdown -> R.string.event_shutdown
    UsageEventType.DeviceStartup -> R.string.event_startup
}
