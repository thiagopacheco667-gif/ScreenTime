package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import app.screentime.widget.data.WidgetData
import app.screentime.widget.ui.WidgetColors
import app.screentime.widget.ui.WidgetText
import app.screentime.widget.ui.formatClock
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData

/**
 * Widget compacto (2×1 ou 2×2): tempo total de hoje e app mais usado.
 * No tamanho maior também mostra o horário da última atualização.
 */
class CompactWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context, topCount = 1)
        val open = launchIntent(context)
        provideContent { CompactContent(data, open) }
    }

    companion object {
        val SMALL = DpSize(110.dp, 48.dp)
        val TALL = DpSize(110.dp, 100.dp)
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}

@Composable
private fun CompactContent(data: WidgetData, open: Intent) {
    val context = LocalContext.current
    val tall = LocalSize.current.height >= CompactWidget.TALL.height

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .cornerRadius(20.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(actionStartActivity(open)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (data) {
            WidgetData.NoAccess -> {
                Text(context.getString(R.string.widget_no_access_title), style = WidgetText.body, maxLines = 1)
                Text(context.getString(R.string.widget_no_access_body), style = WidgetText.bodyMuted, maxLines = 2)
            }
            is WidgetData.Ready -> {
                if (tall) Text(context.getString(R.string.widget_today), style = WidgetText.label)
                Text(
                    text = formatDuration(data.totalMillis),
                    style = if (tall) WidgetText.totalLarge else WidgetText.totalMedium,
                    maxLines = 1,
                )
                val top = data.top.firstOrNull()
                Text(
                    text = if (top != null) {
                        "${top.name} · ${formatDuration(top.millis)}"
                    } else {
                        context.getString(R.string.widget_no_usage)
                    },
                    style = WidgetText.bodyMuted,
                    maxLines = 1,
                )
                if (tall) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.widget_updated_at, formatClock(data.updatedAt, data.zone)),
                        style = WidgetText.small,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
