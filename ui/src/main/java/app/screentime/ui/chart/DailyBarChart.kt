package app.screentime.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Barras por dia (semana ou últimos 7 dias).
 *
 * @param values milissegundos por dia; nulo = sem dados (desenhado como traço pontilhado,
 *   para não parecer um dia de uso zero).
 * @param labels rótulo embaixo de cada barra.
 * @param highlightIndex barra em destaque (dia escolhido ou dia de maior uso).
 * @param averageMillis linha horizontal discreta da média, se informada.
 */
@Composable
fun DailyBarChart(
    values: List<Long?>,
    labels: List<String>,
    highlightIndex: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    averageMillis: Long? = null,
    height: Dp = 160.dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    val soft = accent.copy(alpha = 0.38f)
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val highlightLabelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()
    val max = (values.filterNotNull().maxOrNull() ?: 0L).coerceAtLeast(averageMillis ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val count = values.size.coerceAtLeast(1)
        val slot = size.width / count
        val barWidth = (slot * 0.5f).coerceAtMost(28.dp.toPx())
        val stub = 2.dp.toPx()

        values.forEachIndexed { i, value ->
            val x = i * slot + (slot - barWidth) / 2f
            when {
                value == null -> drawLine(
                    color = baseline,
                    start = Offset(x, chartHeight - stub),
                    end = Offset(x + barWidth, chartHeight - stub),
                    strokeWidth = stub,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(stub * 2, stub * 2)),
                )
                value <= 0L -> drawRoundRect(
                    color = baseline,
                    topLeft = Offset(x, chartHeight - stub),
                    size = Size(barWidth, stub),
                    cornerRadius = CornerRadius(stub / 2f, stub / 2f),
                )
                else -> {
                    val h = (value.toFloat() / max * chartHeight).coerceAtLeast(stub * 2)
                    drawRoundRect(
                        color = if (i == highlightIndex) accent else soft,
                        topLeft = Offset(x, chartHeight - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(8.dp.toPx().coerceAtMost(barWidth / 2f)),
                    )
                }
            }
            labels.getOrNull(i)?.let { text ->
                val layout = measurer.measure(text, if (i == highlightIndex) highlightLabelStyle else labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(i * slot + (slot - layout.size.width) / 2f, chartHeight + 5.dp.toPx()),
                )
            }
        }

        if (averageMillis != null && averageMillis > 0) {
            val y = chartHeight - averageMillis.toFloat() / max * chartHeight
            drawLine(
                color = accent.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }
    }
}
