package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.fillMaxHeight
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
import app.screentime.widget.ui.formatClock
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData
import app.screentime.widget.ui.settingsIntent

/**
 * Widget expandido (4×2), visual azul-marinho:
 * - esquerda: tempo de tela de hoje e gráfico (barras = cada hora de hoje, curva = ontem);
 * - direita: os 4 apps mais usados com ícone, tempo e barra;
 * - engrenagem: abre os Ajustes do app.
 * Em alturas menores, o gráfico some e a lista mostra menos apps, para nada ficar cortado.
 */
class ExpandedWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 170.dp),
            DpSize(300.dp, 190.dp),
            DpSize(340.dp, 220.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context, topCount = MAX_APPS)
        val open = launchIntent(context)
        val settings = settingsIntent(context)
        provideContent { ExpandedContent(data, open, settings) }
    }

    companion object {
        const val MAX_APPS = 4
    }
}

class ExpandedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpandedWidget()
}

private val HEADER_HEIGHT = 40.dp
private val ROW_HEIGHT = 34.dp

@Composable
private fun ExpandedContent(data: WidgetData, open: Intent, settings: Intent) {
    val context = LocalContext.current
    val size = LocalSize.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_dark_bg))
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(open)),
    ) {
        // Cabeçalho
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_header),
                style = NavyText.header,
                modifier = GlanceModifier.defaultWeight(),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_settings),
                contentDescription = context.getString(R.string.widget_open_settings),
                modifier = GlanceModifier.size(36.dp).padding(8.dp).clickable(actionStartActivity(settings)),
            )
        }
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(NavyColors.divider)) {}

        when (data) {
            WidgetData.NoAccess -> Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
                Text(context.getString(R.string.widget_no_access_title), style = NavyText.body)
                Spacer(GlanceModifier.height(4.dp))
                Text(context.getString(R.string.widget_no_access_body), style = NavyText.bodyMuted)
            }
            is WidgetData.Ready -> ReadyContent(data, width = size.width, height = size.height - HEADER_HEIGHT - 1.dp)
        }
    }
}

@Composable
private fun ReadyContent(data: WidgetData.Ready, width: Dp, height: Dp) {
    val context = LocalContext.current
    val rightWidth = width * 0.42f
    val leftWidth = width - rightWidth - 1.dp

    // Altura que sobra para o gráfico depois do total, do rótulo e da legenda.
    val chartWidth = leftWidth - 16.dp - 10.dp
    val chartHeight = height - 8.dp - 42.dp - 16.dp - 6.dp - 16.dp - 10.dp
    val showChart = chartHeight >= 44.dp

    // Quantos apps cabem na coluna da direita.
    val appsFit = ((height - 12.dp - 20.dp - 8.dp) / ROW_HEIGHT).toInt().coerceIn(1, ExpandedWidget.MAX_APPS)
    val apps = data.top.take(appsFit)

    Row(modifier = GlanceModifier.fillMaxSize()) {
        // Coluna esquerda
        Column(
            modifier = GlanceModifier.width(leftWidth).fillMaxHeight()
                .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
        ) {
            Text(formatDuration(data.totalMillis), style = NavyText.total, maxLines = 1)
            Text(context.getString(R.string.widget_total_today), style = NavyText.goldLabel, maxLines = 1)
            if (showChart) {
                Spacer(GlanceModifier.height(6.dp))
                val bitmap = ChartRenderer.render(
                    context = context,
                    widthDp = chartWidth.value,
                    heightDp = chartHeight.value,
                    today = data.hourlyToday,
                    yesterday = data.hourlyYesterday,
                    currentHour = data.currentHour,
                )
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(R.string.widget_chart_description),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.width(chartWidth).height(chartHeight),
                )
                Row(modifier = GlanceModifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = context.getString(
                            if (data.hourlyYesterday != null) R.string.widget_legend else R.string.widget_legend_today_only,
                        ),
                        style = NavyText.small,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = context.getString(R.string.widget_updated_short, formatClock(data.updatedAt, data.zone)),
                        style = NavyText.small,
                        maxLines = 1,
                    )
                }
            } else {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_updated_at, formatClock(data.updatedAt, data.zone)),
                    style = NavyText.small,
                    maxLines = 1,
                )
            }
        }

        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(NavyColors.divider)) {}

        // Coluna direita
        Column(
            modifier = GlanceModifier.width(rightWidth).fillMaxHeight()
                .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Text(context.getString(R.string.widget_top_apps), style = NavyText.goldLabel, maxLines = 1)
            Spacer(GlanceModifier.height(8.dp))
            if (apps.isEmpty()) {
                Text(context.getString(R.string.widget_no_usage), style = NavyText.bodyMuted)
            } else {
                val max = apps.first().millis.coerceAtLeast(1L)
                apps.forEach { app -> NavyAppRow(app, fraction = app.millis.toFloat() / max) }
            }
        }
    }
}

@Composable
private fun NavyAppRow(app: WidgetApp, fraction: Float) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(26.dp).cornerRadius(7.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.size(26.dp).background(NavyColors.tile).cornerRadius(7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.name.take(1).uppercase(), style = NavyText.small)
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(app.name, style = NavyText.appName, maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                Text(formatDuration(app.millis), style = NavyText.appTime, maxLines = 1)
            }
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                color = NavyColors.gold,
                backgroundColor = NavyColors.track,
            )
        }
    }
}
