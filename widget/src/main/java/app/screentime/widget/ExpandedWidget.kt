package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import app.screentime.domain.model.WidgetKind
import app.screentime.widget.data.WidgetApp
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
import app.screentime.widget.ui.settingsIntent

/**
 * Widget expandido (4×2): cabeçalho com horário, ↻ e ⚙; à esquerda o total,
 * a comparação e o gráfico com escala; à direita os apps mais usados (3 ou 4).
 */
class ExpandedWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 110.dp), DpSize(250.dp, 170.dp), DpSize(300.dp, 190.dp), DpSize(340.dp, 220.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        val open = launchIntent(context)
        val settings = settingsIntent(context)
        provideContent { ExpandedContent(data, open, settings) }
    }
}

class ExpandedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpandedWidget()
}

private val HEADER = 38.dp
private val ROW = 32.dp

@Composable
private fun ExpandedContent(data: WidgetData, open: Intent, settings: Intent) {
    val context = LocalContext.current
    val size = LocalSize.current
    WidgetFrame(data, WidgetKind.Expanded, corner = 26.dp, open = open) { palette ->
        Column(GlanceModifier.fillMaxSize()) {
            Row(
                GlanceModifier.fillMaxWidth().height(HEADER).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(GlanceModifier.defaultWeight()) {
                    Text(context.getString(R.string.widget_header), style = palette.style(11.sp, FontWeight.Bold, palette.muted), maxLines = 1)
                    Text(" · ", style = palette.style(11.sp, FontWeight.Bold, palette.muted))
                    Text(context.getString(R.string.widget_today), style = palette.style(11.sp, FontWeight.Bold, palette.accent), maxLines = 1)
                }
                if (data is WidgetData.Ready) {
                    Text(
                        context.getString(R.string.widget_updated_short, formatClock(data.updatedAt, data.zone)),
                        style = palette.style(10.sp, color = palette.muted),
                        maxLines = 1,
                    )
                }
                RefreshButton(palette, 30.dp)
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_settings),
                    contentDescription = context.getString(R.string.widget_open_settings),
                    colorFilter = ColorFilter.tint(palette.muted),
                    modifier = GlanceModifier.size(30.dp).padding(6.dp).clickable(actionStartActivity(settings)),
                )
            }
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(palette.divider)) {}

            when (data) {
                is WidgetData.NoAccess -> Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
                    Text(context.getString(R.string.widget_no_access_title), style = palette.style(13.sp, FontWeight.Bold))
                    Spacer(GlanceModifier.height(4.dp))
                    Text(context.getString(R.string.widget_no_access_body), style = palette.style(12.sp, color = palette.muted))
                }
                is WidgetData.Ready -> Body(data, palette, size.width, size.height - HEADER - 1.dp)
            }
        }
    }
}

@Composable
private fun Body(data: WidgetData.Ready, palette: WidgetPalette, width: Dp, height: Dp) {
    val context = LocalContext.current
    val rightWidth = width * 0.42f
    val leftWidth = width - rightWidth - 1.dp
    val showCmp = data.settings.widgetShowComparison && data.comparisonPercent != null

    val chartWidth = leftWidth - 16.dp - 12.dp
    val chartHeight = height - 10.dp - 38.dp - (if (showCmp) 16.dp else 0.dp) - 8.dp - 10.dp
    val showChart = chartHeight >= 40.dp
    val fit = ((height - 12.dp - 18.dp - 8.dp) / ROW).toInt().coerceIn(1, data.settings.widgetAppCount)
    val apps = data.top.take(fit)

    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.width(leftWidth).fillMaxHeight().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)) {
            Text(formatDuration(data.totalMillis), style = palette.style(32.sp, FontWeight.Medium), maxLines = 1)
            val percent = data.comparisonPercent
            if (showCmp && percent != null) {
                Text(
                    comparisonText(percent, long = true, time = formatClock(data.updatedAt, data.zone)),
                    style = palette.style(11.sp, FontWeight.Bold, if (percent <= 0) palette.better else palette.worse),
                    maxLines = 1,
                )
            }
            if (showChart) {
                Spacer(GlanceModifier.height(8.dp))
                Image(
                    provider = ImageProvider(
                        ChartRenderer.render(
                            context, chartWidth.value, chartHeight.value, data.hourlyToday, data.currentHour, palette,
                            scaleLabels = true, hourLabels = true,
                        ),
                    ),
                    contentDescription = context.getString(R.string.widget_chart_description),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.width(chartWidth).height(chartHeight),
                )
            }
        }
        Box(GlanceModifier.width(1.dp).fillMaxHeight().background(palette.divider)) {}
        Column(GlanceModifier.width(rightWidth).fillMaxHeight().padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)) {
            Text(context.getString(R.string.widget_top_apps), style = palette.style(10.sp, FontWeight.Bold, palette.accent), maxLines = 1)
            Spacer(GlanceModifier.height(8.dp))
            if (apps.isEmpty()) {
                Text(context.getString(R.string.widget_no_usage), style = palette.style(12.sp, color = palette.muted))
            } else {
                val max = apps.first().millis.coerceAtLeast(1L)
                apps.forEach { app -> AppRow(app, palette, app.millis.toFloat() / max) }
            }
        }
    }
}

@Composable
private fun AppRow(app: WidgetApp, palette: WidgetPalette, fraction: Float) {
    Row(GlanceModifier.fillMaxWidth().height(ROW), verticalAlignment = Alignment.CenterVertically) {
        AppIconTile(app, palette, 24.dp)
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Row(GlanceModifier.fillMaxWidth()) {
                Text(app.name, style = palette.style(12.sp, FontWeight.Bold), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                Text(formatShort(app.millis), style = palette.style(12.sp, color = palette.muted), maxLines = 1)
            }
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                color = palette.accent,
                backgroundColor = palette.track,
            )
        }
    }
}
