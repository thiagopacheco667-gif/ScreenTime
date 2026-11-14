package app.screentime.presentation.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AppUsage
import app.screentime.domain.model.Comparison
import app.screentime.domain.model.DataStatus
import app.screentime.domain.model.Dashboard
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDuration
import app.screentime.presentation.common.formatLongDate
import app.screentime.ui.chart.HourlyUsageChart
import app.screentime.ui.component.AppIcon
import app.screentime.ui.component.UsageBar
import app.screentime.ui.icons.ScreenTimeIcons
import app.screentime.ui.theme.ScreenTimeTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private const val TOP_APPS = 5

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit,
    onOpenApp: (packageName: String, date: LocalDate) -> Unit,
    onOpenAllApps: () -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible()
        onPauseOrDispose { viewModel.onHidden() }
    }

    when (val s = state) {
        DashboardUiState.Loading -> LoadingState()
        DashboardUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is DashboardUiState.Error -> ErrorState(s.message, viewModel::refresh)
        is DashboardUiState.Content -> DashboardContent(s.dashboard, onOpenSettings, onOpenApp, onOpenAllApps)
    }
}

@Composable
private fun DashboardContent(
    dashboard: Dashboard,
    onOpenSettings: () -> Unit,
    onOpenApp: (String, LocalDate) -> Unit,
    onOpenAllApps: () -> Unit,
) {
    val today = dashboard.today
    val currentHour = today.computedAt.atZone(today.zoneId).hour

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatLongDate(today.date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = ScreenTimeTheme.colors.card) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        ScreenTimeIcons.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        HeroCard(dashboard)
        HourlyCard(dashboard, currentHour)
        TopAppsCard(dashboard, onOpenApp, onOpenAllApps)
    }
}

/* ---------- Cartão principal ---------- */

@Composable
private fun HeroCard(dashboard: Dashboard) {
    val c = ScreenTimeTheme.colors
    val today = dashboard.today
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .border(1.dp, c.heroBorder, shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.dash_hero_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = formatDuration(countUp(today.totalMillis)),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            stringResource(R.string.dash_hero_sub, formatClock(today.computedAt, today.zoneId)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ComparisonChip(dashboard)
        Text(
            text = yesterdayLine(dashboard),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Número que sobe contando ao abrir e acompanha as atualizações seguintes. */
@Composable
private fun countUp(target: Long): Long {
    val motion = ScreenTimeTheme.motion
    val anim = remember { Animatable(if (motion) 0f else target.toFloat()) }
    LaunchedEffect(target, motion) {
        if (motion) {
            val duration = if (anim.value == 0f) 1000 else 600
            anim.animateTo(target.toFloat(), tween(duration, easing = FastOutSlowInEasing))
        } else {
            anim.snapTo(target.toFloat())
        }
    }
    return anim.value.toLong()
}

private data class ChipStyle(val text: String, val bg: Color, val fg: Color, val up: Boolean?)

@Composable
private fun ComparisonChip(dashboard: Dashboard) {
    val c = ScreenTimeTheme.colors
    val time = formatClock(dashboard.today.computedAt, dashboard.today.zoneId)
    val chip = when (val cmp = dashboard.comparison) {
        is Comparison.Percent -> when {
            cmp.value < 0 -> ChipStyle(stringResource(R.string.dash_cmp_less, -cmp.value, time), c.betterContainer, c.better, false)
            cmp.value > 0 -> ChipStyle(stringResource(R.string.dash_cmp_more, cmp.value, time), c.worseContainer, c.worse, true)
            else -> ChipStyle(stringResource(R.string.dash_cmp_same, time), c.inset, MaterialTheme.colorScheme.onSurface, null)
        }
        Comparison.NoBaseline -> ChipStyle(stringResource(R.string.dash_cmp_no_baseline, time), c.inset, MaterialTheme.colorScheme.onSurfaceVariant, null)
        Comparison.Unavailable -> ChipStyle(stringResource(R.string.dash_cmp_unavailable), c.inset, MaterialTheme.colorScheme.onSurfaceVariant, null)
    }
    Surface(shape = CircleShape, color = chip.bg) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val up = chip.up
            if (up != null) {
                Icon(
                    if (up) ScreenTimeIcons.ArrowUp else ScreenTimeIcons.ArrowDown,
                    contentDescription = null,
                    tint = chip.fg,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(chip.text, style = MaterialTheme.typography.labelLarge, color = chip.fg)
        }
    }
}

@Composable
private fun yesterdayLine(dashboard: Dashboard) = if (dashboard.yesterday.status == DataStatus.Unavailable) {
    buildAnnotatedString { append(stringResource(R.string.dash_yesterday_unavailable)) }
} else {
    val value = formatDuration(dashboard.yesterday.totalMillis)
    val full = stringResource(R.string.dash_yesterday_total, value)
    val strong = MaterialTheme.colorScheme.onSurface
    buildAnnotatedString {
        val i = full.indexOf(value)
        if (i < 0) {
            append(full)
        } else {
            append(full.substring(0, i))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = strong)) { append(value) }
            append(full.substring(i + value.length))
        }
    }
}

/* ---------- Uso por hora ---------- */

@Composable
private fun HourlyCard(dashboard: Dashboard, currentHour: Int) {
    val c = ScreenTimeTheme.colors
    val today = dashboard.today
    val yesterday = dashboard.yesterday.takeIf { it.status == DataStatus.Available }?.hourlyMillis
    var selected by rememberSaveable { mutableStateOf(currentHour) }
    var compare by rememberSaveable { mutableStateOf(false) }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dash_hourly_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.dash_hourly_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val border by animateColorAsState(
                if (compare) MaterialTheme.colorScheme.primary else c.chartAxis,
                label = "cmp-border",
            )
            Surface(
                onClick = { compare = !compare },
                shape = CircleShape,
                color = if (compare) c.navIndicator else Color.Transparent,
                border = BorderStroke(1.dp, border),
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                Text(
                    stringResource(if (compare) R.string.dash_compare_on else R.string.dash_compare_off),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }

        // Detalhe da hora escolhida.
        val hourValue = today.hourlyMillis.getOrElse(selected) { 0L }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.inset)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(
                    R.string.dash_tip_title,
                    hourLabel(selected),
                    hourLabel((selected + 1) % 24),
                    if (hourValue > 0) formatDuration(hourValue) else stringResource(R.string.dash_tip_no_use),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = when {
                    selected > currentHour -> stringResource(R.string.dash_tip_future)
                    compare && yesterday == null -> stringResource(R.string.dash_tip_yesterday_none)
                    compare && yesterday != null -> stringResource(
                        R.string.dash_tip_yesterday,
                        formatDuration(yesterday.getOrElse(selected) { 0L }),
                    )
                    selected == currentHour -> stringResource(R.string.dash_tip_now)
                    else -> stringResource(R.string.dash_tip_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HourlyUsageChart(
            today = today.hourlyMillis,
            yesterday = yesterday,
            showYesterday = compare,
            selectedHour = selected,
            currentHour = currentHour,
            onSelect = { selected = it },
            contentDescription = stringResource(R.string.dash_hourly_a11y, formatDuration(today.totalMillis)),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(stringResource(R.string.dash_legend_today), c.chartBar)
            LegendDot(stringResource(R.string.dash_legend_selected), MaterialTheme.colorScheme.primary)
            if (compare) LegendDot(stringResource(R.string.dash_legend_yesterday), c.chartGhost, dashed = true)
        }
    }
}

private fun hourLabel(hour: Int): String = (if (hour < 10) "0$hour" else "$hour") + "h"

@Composable
private fun LegendDot(label: String, color: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier
                .size(10.dp)
                .clip(shape)
                .then(if (dashed) Modifier.border(1.dp, color, shape) else Modifier.background(color)),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ---------- Mais usados ---------- */

@Composable
private fun TopAppsCard(dashboard: Dashboard, onOpenApp: (String, LocalDate) -> Unit, onOpenAllApps: () -> Unit) {
    val today = dashboard.today
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dash_top_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.dash_top_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenAllApps) {
                Text(stringResource(R.string.dash_see_all), style = MaterialTheme.typography.labelLarge)
            }
        }
        val top = today.apps.take(TOP_APPS)
        if (top.isEmpty()) {
            Text(
                stringResource(R.string.diag_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val max = top.first().foregroundMillis.coerceAtLeast(1L)
            val total = today.totalMillis.coerceAtLeast(1L)
            top.forEachIndexed { index, app ->
                TopAppRow(
                    app = app,
                    fraction = app.foregroundMillis.toFloat() / max,
                    share = (app.foregroundMillis * 100f / total).roundToInt(),
                    zone = today.zoneId,
                    delayMillis = 250 + index * 90,
                    onClick = { onOpenApp(app.packageName, today.date) },
                )
            }
        }
    }
}

@Composable
private fun TopAppRow(
    app: AppUsage,
    fraction: Float,
    share: Int,
    zone: ZoneId,
    delayMillis: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, label = app.displayName, size = 42.dp)
        Spacer(Modifier.width(12.dp))
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
            val last = app.lastUsedAt?.let { formatClock(it, zone) } ?: "—"
            Text(
                text = pluralStringResource(R.plurals.dash_app_sub, app.sessionCount, app.sessionCount, last, share),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val c = ScreenTimeTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
