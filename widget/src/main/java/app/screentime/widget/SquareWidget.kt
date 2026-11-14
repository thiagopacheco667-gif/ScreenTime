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
import app.screentime.widget.ui.RefreshButton
import app.screentime.widget.ui.WidgetFrame
import app.screentime.widget.ui.WidgetPalette
import app.screentime.widget.ui.comparisonText
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

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(150.dp, 150.dp), DpSize(170.dp, 170.dp), DpSize(190.dp, 210.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        val open = launchIntent(context)
        provideContent { SquareContent(data, open) }
    }
}

class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareWidget()
}

private val PAD = 14.dp
private val TOP_ROW = 22.dp
private val ROW = 20.dp
private val MIN_CHART = 26.dp
private val MAX_CHART = 64.dp

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

@Composable
private fun Ready(data: WidgetData.Ready, palette: WidgetPalette) {
    val context = LocalContext.current
    val size = LocalSize.current
    val showCmp = data.settings.widgetShowComparison && data.comparisonPercent != null
    val innerWidth = size.width - PAD * 2

    val free = size.height - PAD * 2 - TOP_ROW - 30.dp - (if (showCmp) 15.dp else 0.dp) - 10.dp
    val fit = ((free - MIN_CHART) / ROW).toInt().coerceAtLeast(1)
    // 2×2 comum: até 3 apps; maior que isso, respeita a escolha (3 ou 4) de Ajustes › Aparência.
    val limit = if (size.height >= 200.dp) data.settings.widgetAppCount else minOf(3, data.settings.widgetAppCount)
    val apps = data.top.take(minOf(fit, limit))
    val chartHeight = (free - ROW * apps.size).coerceIn(MIN_CHART, MAX_CHART)

    Row(GlanceModifier.fillMaxWidth().height(TOP_ROW), verticalAlignment = Alignment.CenterVertically) {
        Text(
            context.getString(R.string.widget_today),
            style = palette.style(11.sp, FontWeight.Bold, palette.accent),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(formatClock(data.updatedAt, data.zone), style = palette.style(11.sp, color = palette.muted))
        RefreshButton(palette, 24.dp)
    }
    Text(formatDuration(data.totalMillis), style = palette.style(26.sp, FontWeight.Medium), maxLines = 1)
    val percent = data.comparisonPercent
    if (data.settings.widgetShowComparison && percent != null) {
        Text(
            comparisonText(percent, long = false, time = ""),
            style = palette.style(11.sp, FontWeight.Bold, if (percent <= 0) palette.better else palette.worse),
            maxLines = 1,
        )
    }
    Spacer(GlanceModifier.height(4.dp))
    Image(
        provider = ImageProvider(
            ChartRenderer.render(
                context, innerWidth.value, chartHeight.value, data.hourlyToday, data.currentHour, palette,
                scaleLabels = false, hourLabels = true,
            ),
        ),
        contentDescription = context.getString(R.string.widget_chart_description),
        contentScale = ContentScale.FillBounds,
        modifier = GlanceModifier.width(innerWidth).height(chartHeight),
    )
    Spacer(GlanceModifier.height(6.dp))
    if (apps.isEmpty()) {
        Text(context.getString(R.string.widget_no_usage), style = palette.style(12.sp, color = palette.muted))
    } else {
        apps.forEach { app ->
            Row(GlanceModifier.fillMaxWidth().height(ROW), verticalAlignment = Alignment.CenterVertically) {
                AppIconTile(app, palette, 16.dp)
                Spacer(GlanceModifier.width(7.dp))
                Text(app.name, style = palette.style(12.sp, FontWeight.Bold), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                Text(formatShort(app.millis), style = palette.style(12.sp, color = palette.muted), maxLines = 1)
            }
        }
    }
}
