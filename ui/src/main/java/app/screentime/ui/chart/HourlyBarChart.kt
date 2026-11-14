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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/**
 * Barras das 24 horas do dia.
 * A hora atual fica na cor de destaque; as demais em tom mais suave.
 * Horas sem uso mostram só um traço na base.
 *
 * @param values milissegundos por hora (24 posições).
 * @param highlightHour hora destacada (a atual), ou nulo.
 */
@Composable
fun HourlyBarChart(
    values: List<Long>,
    highlightHour: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val soft = accent.copy(alpha = 0.38f)
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val measurer = rememberTextMeasurer()
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val labelHeight = 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val slot = size.width / 24f
        val barWidth = slot * 0.62f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val stub = 2.dp.toPx()

        values.forEachIndexed { hour, value ->
            val x = hour * slot + (slot - barWidth) / 2f
            if (value <= 0L) {
                drawRoundRect(
                    color = baseline,
                    topLeft = Offset(x, chartHeight - stub),
                    size = Size(barWidth, stub),
                    cornerRadius = CornerRadius(stub / 2f, stub / 2f),
                )
            } else {
                val h = (value.toFloat() / max * chartHeight).coerceAtLeast(stub * 2)
                drawRoundRect(
                    color = if (hour == highlightHour) accent else soft,
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = radius,
                )
            }
        }

        for (hour in listOf(0, 6, 12, 18)) {
            val layout = measurer.measure("${hour}h", labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(hour * slot + (slot - layout.size.width) / 2f, chartHeight + 4.dp.toPx()),
            )
        }
    }
}
