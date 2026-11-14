package app.screentime.presentation.apps

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.DayUsage
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.HeroCard
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.SectionLabel
import app.screentime.presentation.common.animatedMillis
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatMonth
import app.screentime.presentation.common.formatWeekdayLong
import app.screentime.presentation.common.formatWeekdayShort
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.UsageBar
import app.screentime.ui.icons.ScreenTimeIcons
import app.screentime.ui.theme.ScreenTimeTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.apps_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(state.days.map { formatMonth(it) }.distinct().joinToString(" · "))
                DayPicker(state, onSelect)
            }
        }
        dayAppItems(state.usage, state.selected, state.today, onOpenApp)
    }
}

/** Sete dias lado a lado: dia da semana em cima, número embaixo. */
@Composable
private fun DayPicker(state: AppsUiState.Content, onSelect: (LocalDate) -> Unit) {
    val c = ScreenTimeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.days.forEach { day ->
            val on = day == state.selected
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else c.card, label = "day-bg")
            val fg = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp)
                    .clip(shape)
                    .background(bg)
                    .border(1.dp, if (on) MaterialTheme.colorScheme.primary else c.cardBorder, shape)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(day) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = when (day) {
                        state.today -> stringResource(R.string.day_today).lowercase()
                        state.today.minusDays(1) -> stringResource(R.string.day_yesterday).lowercase()
                        else -> formatWeekdayShort(day)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = fg,
                    maxLines = 1,
                )
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = fg)
            }
        }
    }
}

/** Título do cartão do dia: "Hoje até agora", "Ontem · sexta-feira, 13", "terça-feira, 10". */
@Composable
internal fun dayHeroTitle(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.apps_hero_today)
    today.minusDays(1) -> stringResource(R.string.apps_hero_yesterday, formatWeekdayLong(date), date.dayOfMonth)
    else -> stringResource(R.string.apps_hero_day, formatWeekdayLong(date), date.dayOfMonth)
}

/** Cartão do dia + lista de apps (também usado ao abrir um dia pela Semana). */
internal fun LazyListScope.dayAppItems(
    usage: DayUsage,
    date: LocalDate,
    today: LocalDate,
    onOpenApp: (String, LocalDate) -> Unit,
) {
    item(key = "hero-${date.toEpochDay()}") {
        HeroCard {
            Text(
                dayHeroTitle(date, today).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (usage.status == DataStatus.Unavailable) {
                        stringResource(R.string.week_day_no_data)
                    } else {
                        formatDuration(animatedMillis(usage.totalMillis))
                    },
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f),
                )
                if (usage.status == DataStatus.Available) {
                    Text(
                        pluralStringResource(R.plurals.apps_opened_count, usage.apps.size, usage.apps.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    when {
        usage.status == DataStatus.Unavailable -> item {
            Text(
                stringResource(R.string.apps_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        usage.apps.isEmpty() -> item {
            Text(
                stringResource(R.string.apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        else -> {
            item { SectionLabel(stringResource(R.string.apps_order_label), Modifier.padding(top = 16.dp, bottom = 4.dp)) }
            val max = usage.apps.first().foregroundMillis.coerceAtLeast(1L)
            val total = usage.totalMillis.coerceAtLeast(1L)
            itemsIndexed(usage.apps, key = { _, app -> "${date.toEpochDay()}-${app.packageName}" }) { index, app ->
                AppListRow(
                    app = app,
                    fraction = app.foregroundMillis.toFloat() / max,
                    share = (app.foregroundMillis * 100f / total).roundToInt(),
                    zone = usage.zoneId,
                    delayMillis = index.coerceAtMost(8) * 70,
                    onClick = { onOpenApp(app.packageName, date) },
                )
            }
        }
    }
}

@Composable
private fun AppListRow(
    app: AppUsage,
    fraction: Float,
    share: Int,
    zone: ZoneId,
    delayMillis: Int,
    onClick: () -> Unit,
) {
    val c = ScreenTimeTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = app.packageName, label = app.displayName, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatDuration(app.foregroundMillis), style = MaterialTheme.typography.titleSmall)
                }
                UsageBar(fraction, delayMillis = delayMillis)
                Row {
                    val last = app.lastUsedAt?.let { formatClock(it, zone) } ?: "—"
                    Text(
                        pluralStringResource(R.plurals.apps_row_sub, app.sessionCount, app.sessionCount, last),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.apps_share, share),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                ScreenTimeIcons.Forward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}
