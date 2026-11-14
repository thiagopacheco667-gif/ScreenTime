package app.screentime.presentation.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.WeekDay
import app.screentime.domain.model.WeekUsage
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.HeroCard
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.SectionCard
import app.screentime.presentation.common.animatedMillis
import app.screentime.presentation.common.formatDayMonth
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatRange
import app.screentime.presentation.common.formatWeekdayLong
import app.screentime.presentation.common.formatWeekdayShort
import app.screentime.ui.chart.DayBarsChart
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.UsageBar
import app.screentime.ui.icons.ScreenTimeIcons
import app.screentime.ui.theme.ScreenTimeTheme
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
        is WeekUiState.Content -> WeekContent(s, viewModel::previous, viewModel::next, onOpenDay, onOpenApp)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.week_title), style = MaterialTheme.typography.headlineMedium)
        WeekNavigator(state, onPrevious, onNext)

        if (week.daysWithData == 0) {
            SectionCard {
                Text(
                    stringResource(R.string.week_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            StatTiles(week)
        }
        WeekChartCard(state)
        DaysCard(week, state.today, onOpenDay)
        if (week.topApps.isNotEmpty()) TopAppsCard(week, state.today, onOpenApp)
    }
}

@Composable
private fun WeekNavigator(state: WeekUiState.Content, onPrevious: () -> Unit, onNext: () -> Unit) {
    val week = state.week
    SectionCard(padding = 4.dp, spacing = 0.dp) {
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
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.week_range_long, week.start.dayOfMonth, formatDayMonth(week.end)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(ScreenTimeIcons.Forward, contentDescription = stringResource(R.string.week_next))
            }
        }
    }
}

@Composable
private fun StatTiles(week: WeekUsage) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeroCard(Modifier.weight(1f), padding = 14.dp) {
            TileLabel(stringResource(R.string.week_tile_total), accent = true)
            TileValue(formatShort(animatedMillis(week.totalMillis)))
            TileNote(stringResource(R.string.week_tile_total_note, week.daysWithData))
        }
        SectionCard(Modifier.weight(1f), padding = 14.dp, spacing = 6.dp) {
            TileLabel(stringResource(R.string.week_tile_average))
            TileValue(week.averageMillis?.let { formatShort(it) } ?: "—")
            TileNote(stringResource(R.string.week_tile_average_note))
        }
        SectionCard(Modifier.weight(1f), padding = 14.dp, spacing = 6.dp) {
            TileLabel(stringResource(R.string.week_tile_peak))
            val peak = week.peakDay
            TileValue(peak?.millis?.let { formatShort(it) } ?: "—")
            TileNote(peak?.let { "${formatWeekdayShort(it.date)}, ${it.date.dayOfMonth}" } ?: "")
        }
    }
}

/** "51 h 33" (sem "min"), para caber nos blocos. */
private fun formatShort(millis: Long): String {
    val minutes = millis / 60_000L
    val h = minutes / 60
    val m = minutes % 60
    return if (h == 0L) "$m min" else "$h h ${m.toString().padStart(2, '0')}"
}

@Composable
private fun TileLabel(text: String, accent: Boolean = false) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
)

@Composable
private fun TileValue(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1)

@Composable
private fun TileNote(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun WeekChartCard(state: WeekUiState.Content) {
    val week = state.week
    val c = ScreenTimeTheme.colors
    val defaultIndex = week.days.indexOfFirst { it.date == state.today }.takeIf { it >= 0 }
        ?: week.peakDay?.let { p -> week.days.indexOfFirst { it.date == p.date } }
        ?: week.days.indexOfLast { it.millis != null }.coerceAtLeast(0)
    var selected by rememberSaveable(state.offset) { mutableIntStateOf(defaultIndex) }
    val day = week.days[selected.coerceIn(0, week.days.lastIndex)]

    SectionCard {
        Column {
            Text(stringResource(R.string.week_chart_title), style = MaterialTheme.typography.titleMedium)
            Text(
                week.averageMillis?.let { stringResource(R.string.week_chart_sub, formatDuration(it)) }
                    ?: stringResource(R.string.week_chart_sub_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.inset)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val name = dayName(day.date, state.today)
            val value = day.millis
            Text(
                text = if (value != null) {
                    "$name · ${formatDuration(value)}"
                } else {
                    "$name · ${stringResource(if (day.isFuture) R.string.week_day_future_long else R.string.week_day_no_data)}"
                },
                style = MaterialTheme.typography.titleSmall,
            )
            val body = tipBody(day, week.averageMillis, state.today)
            if (body.isNotEmpty()) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DayBarsChart(
            values = week.days.map { it.millis },
            isFuture = week.days.map { it.isFuture },
            labels = week.days.map { formatWeekdayShort(it.date) },
            selectedIndex = selected,
            averageMillis = week.averageMillis,
            onSelect = { selected = it },
            contentDescription = stringResource(
                R.string.week_chart_a11y,
                formatRange(week.start, week.end),
                formatDuration(week.totalMillis),
            ),
        )
    }
}

@Composable
private fun tipBody(day: WeekDay, average: Long?, today: LocalDate): String {
    val millis = day.millis
    return when {
        day.isFuture -> stringResource(R.string.week_tip_future)
        millis == null -> stringResource(R.string.week_tip_no_data)
        average == null -> ""
        else -> {
            val diff = millis - average
            val base = if (diff >= 0) {
                stringResource(R.string.week_tip_above, formatDuration(diff))
            } else {
                stringResource(R.string.week_tip_below, formatDuration(-diff))
            }
            if (day.date == today) base + stringResource(R.string.week_tip_today_suffix) else base
        }
    }
}

@Composable
private fun dayName(date: LocalDate, today: LocalDate): String = (
    if (date == today) {
        stringResource(R.string.week_today_named, formatWeekdayLong(date), date.dayOfMonth)
    } else {
        stringResource(R.string.apps_hero_day, formatWeekdayLong(date), date.dayOfMonth)
    }
    ).replaceFirstChar { it.uppercase() }

@Composable
private fun DaysCard(week: WeekUsage, today: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    val c = ScreenTimeTheme.colors
    val peakDate = week.peakDay?.date
    val average = week.averageMillis
    SectionCard(padding = 8.dp, spacing = 0.dp) {
        week.days.forEachIndexed { i, day ->
            val value = day.millis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !day.isFuture) { onOpenDay(day.date) }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(dayName(day.date, today), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when {
                            day.isFuture -> stringResource(R.string.week_day_future_long)
                            value == null -> stringResource(R.string.week_day_no_data)
                            day.date == peakDate -> stringResource(R.string.week_note_peak)
                            day.date == today -> stringResource(R.string.week_note_today)
                            average != null && value > average -> stringResource(R.string.week_note_above)
                            else -> stringResource(R.string.week_note_below)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (day.isFuture || value == null) "—" else formatDuration(value),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (i < week.days.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(c.divider))
            }
        }
    }
}

@Composable
private fun TopAppsCard(week: WeekUsage, today: LocalDate, onOpenApp: (String, LocalDate) -> Unit) {
    SectionCard {
        Text(stringResource(R.string.week_top_title), style = MaterialTheme.typography.titleMedium)
        val top = week.topApps.take(TOP_APPS)
        val max = top.first().foregroundMillis.coerceAtLeast(1L)
        val detailDate = minOf(week.end, today)
        top.forEachIndexed { index, app ->
            WeekAppRow(app, app.foregroundMillis.toFloat() / max, 200 + index * 80) {
                onOpenApp(app.packageName, detailDate)
            }
        }
    }
}

@Composable
private fun WeekAppRow(app: AppUsage, fraction: Float, delayMillis: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, label = app.displayName)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(formatDuration(app.foregroundMillis), style = MaterialTheme.typography.titleSmall)
            }
            UsageBar(fraction, delayMillis = delayMillis)
        }
    }
}
