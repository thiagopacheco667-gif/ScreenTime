package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import app.screentime.widget.data.WidgetApp
import app.screentime.widget.data.WidgetData
import app.screentime.widget.ui.ChartRenderer
import app.screentime.widget.ui.NavyColors
import app.screentime.widget.ui.NavyText
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData
import app.screentime.widget.ui.settingsIntent

/**
 * Widget quadrado (2×2), mesmo visual do expandido em espaço menor:
 * tempo de hoje + engrenagem, gráfico por hora (barras = hoje, curva = ontem)
 * e os apps mais usados. Maior que 2×2, mostra o 4º app e as barras de cada um.
 */
class SquareWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(150.dp, 150.dp),
            DpSize(170.dp, 170.dp),
            DpSize(190.dp, 210.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context, topCount = MAX_APPS)
        val open = launchIntent(context)
        val settings = settingsIntent(context)
        provideContent { SquareContent(data, open, settings) }
    }

    companion object {
        const val MAX_APPS = 4
    }
}

class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareWidget()
}

private val PADDING = 12.dp
private val TOP_ROW = 32.dp

@Composable
private fun SquareContent(data: WidgetData, open: Intent, settings: Intent) {
    val context = LocalContext.current
    val size = LocalSize.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_dark_bg))
            .cornerRadius(24.dp)
            .padding(PADDING)
            .clickable(actionStartActivity(open)),
    ) {
        when (data) {
            WidgetData.NoAccess -> {
                Text(context.getString(R.string.widget_no_access_title), style = NavyText.body)
                Spacer(GlanceModifier.height(4.dp))
                Text(context.getString(R.string.widget_no_access_body), style = NavyText.bodyMuted)
            }
            is WidgetData.Ready -> {
                val innerWidth = size.width - PADDING * 2
                val showLabel = size.height >= 180.dp
                val showBars = size.height >= 200.dp
                val rowHeight = if (showBars) 26.dp else 20.dp

                // Espaço que sobra para gráfico + apps.
                val free = size.height - PADDING * 2 - TOP_ROW - (if (showLabel) 14.dp else 0.dp) - 12.dp
                val apps = ((free - MIN_CHART) / rowHeight).toInt().coerceIn(1, SquareWidget.MAX_APPS)
                val chartHeight = (free - rowHeight * apps).coerceIn(MIN_CHART, MAX_CHART)

                if (showLabel) {
                    Text(context.getString(R.string.widget_header), style = NavyText.goldLabel, maxLines = 1)
                }

                // Tempo de hoje + engrenagem
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(TOP_ROW),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDuration(data.totalMillis),
                        style = NavyText.totalSmall,
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = context.getString(R.string.widget_today),
                        style = NavyText.goldLabel,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_settings),
                        contentDescription = context.getString(R.string.widget_open_settings),
                        modifier = GlanceModifier.size(28.dp).padding(5.dp).clickable(actionStartActivity(settings)),
                    )
                }

                Spacer(GlanceModifier.height(4.dp))
                val bitmap = ChartRenderer.render(
                    context = context,
                    widthDp = innerWidth.value,
                    heightDp = chartHeight.value,
                    today = data.hourlyToday,
                    yesterday = data.hourlyYesterday,
                    currentHour = data.currentHour,
                    showLabels = false,
                )
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(R.string.widget_chart_description),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.width(innerWidth).height(chartHeight),
                )
                Spacer(GlanceModifier.height(8.dp))

                val top = data.top.take(apps)
                if (top.isEmpty()) {
                    Text(context.getString(R.string.widget_no_usage), style = NavyText.bodyMuted)
                } else {
                    val max = top.first().millis.coerceAtLeast(1L)
                    top.forEach { app ->
                        SquareAppRow(app, rowHeight, showBar = showBars, fraction = app.millis.toFloat() / max)
                    }
                }
            }
        }
    }
}

private val MIN_CHART = 28.dp
private val MAX_CHART = 72.dp

@Composable
private fun SquareAppRow(app: WidgetApp, height: androidx.compose.ui.unit.Dp, showBar: Boolean, fraction: Float) {
    Column(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp).cornerRadius(4.dp),
                )
            } else {
                Box(
                    modifier = GlanceModifier.size(16.dp).background(NavyColors.tile).cornerRadius(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(app.name.take(1).uppercase(), style = NavyText.small)
                }
            }
            Spacer(GlanceModifier.width(6.dp))
            Text(app.name, style = NavyText.appNameSmall, maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(4.dp))
            Text(formatDuration(app.millis), style = NavyText.appTimeSmall, maxLines = 1)
        }
        if (showBar) {
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(2.dp),
                color = NavyColors.gold,
                backgroundColor = NavyColors.track,
            )
        }
    }
}
