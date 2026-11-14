package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import app.screentime.domain.model.WidgetKind
import app.screentime.widget.data.WidgetData
import app.screentime.widget.ui.AppIconTile
import app.screentime.widget.ui.ChartRenderer
import app.screentime.widget.ui.ComparisonPill
import app.screentime.widget.ui.RefreshButton
import app.screentime.widget.ui.WidgetFrame
import app.screentime.widget.ui.WidgetPalette
import app.screentime.widget.ui.formatClock
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.formatShort
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData

/**
 * Widget quadrado (2×2): "HOJE", horário e ↻; total; comparação com ontem;
 * barras por hora com as horas marcadas; os apps mais usados.
 */
class SquareWidget : GlanceAppWidget() {

    // Tamanho real informado pelo launcher (o HyperOS arredonda para baixo no modo responsivo).
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        val open = launchIntent(context)
        provideContent { SquareContent(data, open) }
    }
}

class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareWidget()
}

private val PAD = 12.dp
private val TOP_ROW = 20.dp
private val TOTAL = 28.dp
private val PILL = 20.dp
private val ROW = 19.dp
private val MIN_CHART = 20.dp
private val MAX_CHART = 56.dp
private val LABELS_FROM = 32.dp

@Composable
private fun SquareContent(data: WidgetData, open: Intent) {
    WidgetFrame(data, WidgetKind.Square, corner = 24.dp, open = open) { palette ->
        Column(GlanceModifier.fillMaxSize().padding(PAD)) {
            when (data) {
                is WidgetData.NoAccess -> NoAccess(palette)
                is WidgetData.Ready -> Ready(data, palette)
            }
        }
    }
}

@Composable
private fun NoAccess(palette: WidgetPalette) {
    val context = LocalContext.current
    Text(context.getString(R.string.widget_no_access_title), style = palette.style(13.sp, FontWeight.Bold))
    Spacer(GlanceModifier.height(4.dp))
    Text(context.getString(R.string.widget_no_access_body), style = palette.style(12.sp, color = palette.muted))
}

/**
 * Prioridade do espaço: 1) total, 2) os apps (3, ou 4 se couber e estiver escolhido),
 * 3) gráfico, 4) selo de comparação. O que não couber sai na ordem inversa.
 */
@Composable
private fun Ready(data: WidgetData.Ready, palette: WidgetPalette) {
    val context = LocalContext.current
    val size = LocalSize.current
    val innerWidth = size.width - PAD * 2
    val percent = data.comparisonPercent
    val wantsPill = data.settings.widgetShowComparison

    var free = size.height - PAD * 2 - TOP_ROW - TOTAL - 6.dp
    val wanted = minOf(data.settings.widgetAppCount, data.top.size).coerceAtLeast(0)
    var appCount = minOf(wanted, 3)
    // 4º app só se ainda sobrar espaço para o gráfico mínimo.
    if (wanted >= 4 && free - ROW * 4 >= MIN_CHART) appCount = 4
    while (appCount > 1 && free - ROW * appCount < 0.dp) appCount--
    free -= ROW * appCount
    val showPill = wantsPill && free >= PILL + MIN_CHART
    if (showPill) free -= PILL
    val chartHeight = free.coerceAtMost(MAX_CHART)
    val showChart = chartHeight >= MIN_CHART
    val apps = data.top.take(appCount)

    Row(GlanceModifier.fillMaxWidth().height(TOP_ROW), verticalAlignment = Alignment.CenterVertically) {
        Text(
            context.getString(R.string.widget_today),
            style = palette.style(11.sp, FontWeight.Bold, palette.accent),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(formatClock(data.updatedAt, data.zone), style = palette.style(11.sp, color = palette.muted))
        RefreshButton(palette, 22.dp)
    }
    Text(
        formatDuration(data.totalMillis),
        style = palette.style(22.sp, FontWeight.Bold),
        maxLines = 1,
        modifier = GlanceModifier.height(TOTAL),
    )
    if (percent != null && showPill) {
        Box(GlanceModifier.height(PILL)) { ComparisonPill(percent, palette, long = false, time = "") }
    }
    if (showChart) {
        Image(
            provider = ImageProvider(
                ChartRenderer.render(
                    context, innerWidth.value, chartHeight.value, data.hourlyToday, data.currentHour, palette,
                    scaleLabels = false, hourLabels = chartHeight >= LABELS_FROM,
                ),
            ),
            contentDescription = context.getString(R.string.widget_chart_description),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.width(innerWidth).height(chartHeight),
        )
    }
    Spacer(GlanceModifier.height(6.dp))
    if (apps.isEmpty()) {
        Text(context.getString(R.string.widget_no_usage), style = palette.style(12.sp, color = palette.muted))
    } else {
        apps.forEach { app ->
            Row(GlanceModifier.fillMaxWidth().height(ROW), verticalAlignment = Alignment.CenterVertically) {
                AppIconTile(app, palette, 15.dp)
                Spacer(GlanceModifier.width(6.dp))
                Text(app.name, style = palette.style(12.sp, FontWeight.Bold), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                Text(formatShort(app.millis), style = palette.style(12.sp, color = palette.muted), maxLines = 1)
            }
        }
    }
}
