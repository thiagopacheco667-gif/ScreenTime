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
import app.screentime.widget.ui.WidgetColors
import app.screentime.widget.ui.WidgetText
import app.screentime.widget.ui.formatClock
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData

/**
 * Widget expandido (4×2): total de hoje, os três apps mais usados com o tempo
 * de cada um e uma barra com a fatia de cada app no tempo de tela do dia.
 */
class ExpandedWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context, topCount = TOP)
        val open = launchIntent(context)
        provideContent { ExpandedContent(data, open) }
    }

    companion object {
        const val TOP = 3
    }
}

class ExpandedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpandedWidget()
}

@Composable
private fun ExpandedContent(data: WidgetData, open: Intent) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(open)),
    ) {
        when (data) {
            WidgetData.NoAccess -> {
                Text(context.getString(R.string.widget_no_access_title), style = WidgetText.body)
                Spacer(GlanceModifier.height(4.dp))
                Text(context.getString(R.string.widget_no_access_body), style = WidgetText.bodyMuted)
            }
            is WidgetData.Ready -> {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(context.getString(R.string.widget_today), style = WidgetText.label)
                        Text(formatDuration(data.totalMillis), style = WidgetText.totalLarge, maxLines = 1)
                    }
                    Text(
                        text = context.getString(R.string.widget_updated_at, formatClock(data.updatedAt, data.zone)),
                        style = WidgetText.small,
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                if (data.top.isEmpty()) {
                    Text(context.getString(R.string.widget_no_usage), style = WidgetText.bodyMuted)
                } else {
                    val total = data.totalMillis.coerceAtLeast(1L)
                    data.top.forEach { app ->
                        AppLine(app, share = app.millis.toFloat() / total)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLine(app: WidgetApp, share: Float) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val icon = app.icon
        if (icon != null) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp).cornerRadius(6.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.size(24.dp).background(WidgetColors.track).cornerRadius(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.name.take(1).uppercase(), style = WidgetText.small)
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(app.name, style = WidgetText.body, maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Text(formatDuration(app.millis), style = WidgetText.body, maxLines = 1)
            }
            Spacer(GlanceModifier.height(4.dp))
            // Indicador de uso: fatia do app no tempo de tela do dia.
            LinearProgressIndicator(
                progress = share.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = WidgetColors.accent,
                backgroundColor = WidgetColors.track,
            )
        }
    }
}
