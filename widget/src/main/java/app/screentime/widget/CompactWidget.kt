package app.screentime.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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
import app.screentime.widget.ui.RefreshButton
import app.screentime.widget.ui.WidgetFrame
import app.screentime.widget.ui.comparisonText
import app.screentime.widget.ui.formatClock
import app.screentime.widget.ui.formatDuration
import app.screentime.widget.ui.formatShort
import app.screentime.widget.ui.launchIntent
import app.screentime.widget.ui.loadWidgetData

/** Widget compacto (2×1): total de hoje, app mais usado e horário da atualização. */
class CompactWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
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
    WidgetFrame(data, WidgetKind.Compact, corner = 22.dp, open = open) { palette ->
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (data) {
                is WidgetData.NoAccess -> {
                    Text(context.getString(R.string.widget_no_access_title), style = palette.style(13.sp, FontWeight.Bold), maxLines = 1)
                    Text(context.getString(R.string.widget_no_access_body), style = palette.style(12.sp, color = palette.muted), maxLines = 2)
                }
                is WidgetData.Ready -> {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            context.getString(R.string.widget_header_today),
                            style = palette.style(10.sp, FontWeight.Bold, palette.accent),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(formatClock(data.updatedAt, data.zone), style = palette.style(10.sp, color = palette.muted))
                        if (tall) RefreshButton(palette, 22.dp)
                    }
                    Text(
                        formatDuration(data.totalMillis),
                        style = palette.style(if (tall) 28.sp else 22.sp, FontWeight.Medium),
                        maxLines = 1,
                    )
                    val top = data.top.firstOrNull()
                    Row(GlanceModifier.fillMaxWidth()) {
                        if (top != null) {
                            Text(context.getString(R.string.widget_most_used), style = palette.style(12.sp, color = palette.muted), maxLines = 1)
                            Spacer(GlanceModifier.width(4.dp))
                            Text("${top.name} · ${formatShort(top.millis)}", style = palette.style(12.sp, FontWeight.Bold), maxLines = 1)
                        } else {
                            Text(context.getString(R.string.widget_no_usage), style = palette.style(12.sp, color = palette.muted), maxLines = 1)
                        }
                    }
                    val percent = data.comparisonPercent
                    if (tall && data.settings.widgetShowComparison && percent != null) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            comparisonText(percent, long = false, time = ""),
                            style = palette.style(11.sp, FontWeight.Bold, if (percent <= 0) palette.better else palette.worse),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
