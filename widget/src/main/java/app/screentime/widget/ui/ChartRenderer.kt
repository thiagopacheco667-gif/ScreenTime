package app.screentime.widget.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Desenha o gráfico do widget como imagem, porque widgets não aceitam desenho livre.
 *
 * - Barras: uso real de cada hora de hoje (a hora atual mais clara).
 * - Curva suave com área: uso de cada hora de ontem, para comparar.
 * - Grade: 0, metade e topo da escala; marcas 00:00, 06:00, 12:00 e 18:00.
 */
internal object ChartRenderer {

    private const val MUTED = 0xFFA9B0D6.toInt()
    private const val GRID = 0x33FFFFFF
    private const val BAR_TOP = 0xFFF6F3FF.toInt()
    private const val BAR_BOTTOM = 0xFFB7BCEB.toInt()
    private const val CURVE = 0xFFC3C7F2.toInt()
    private const val AREA_TOP = 0x4DC3C7F2
    private const val AREA_BOTTOM = 0x00C3C7F2

    /** Escalas possíveis, em minutos por hora. */
    private val SCALES = listOf(10, 20, 30, 60)

    fun render(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        today: List<Long>,
        yesterday: List<Long>?,
        currentHour: Int,
        showLabels: Boolean = true,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).roundToInt().coerceAtLeast(1)
        val h = (heightDp * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        fun dp(v: Float) = v * density

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = dp(9.5f) * context.resources.configuration.fontScale.coerceIn(0.85f, 1.3f)
        }
        // Sem rótulos (widget quadrado): o gráfico usa a área toda.
        val labelRightWidth = if (showLabels) label.measureText("60m") + dp(6f) else 0f
        val labelBottomHeight = if (showLabels) label.textSize + dp(5f) else dp(1f)

        val left = 0f
        val top = if (showLabels) label.textSize / 2f else dp(1f)
        val right = w - labelRightWidth
        val bottom = h - labelBottomHeight
        val plotHeight = (bottom - top).coerceAtLeast(1f)
        val slot = (right - left) / 24f

        val peakMinutes = max(
            today.maxOrNull() ?: 0L,
            yesterday?.maxOrNull() ?: 0L,
        ) / 60_000f
        val scale = SCALES.firstOrNull { it >= peakMinutes } ?: 60
        fun y(millis: Long): Float = bottom - (millis / 60_000f / scale).coerceIn(0f, 1f) * plotHeight

        // Grade horizontal e escala à direita.
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRID
            strokeWidth = dp(1f)
        }
        for (step in 0..2) {
            val gy = bottom - plotHeight * step / 2f
            canvas.drawLine(left, gy, right, gy, grid)
            if (showLabels) {
                val text = if (step == 0) "0" else "${scale * step / 2}m"
                canvas.drawText(text, right + dp(5f), gy + label.textSize / 3f, label)
            }
        }

        // Marcas verticais pontilhadas e horários embaixo.
        val dashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRID
            strokeWidth = dp(1f)
            pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
        }
        for (hour in listOf(0, 6, 12, 18)) {
            val x = left + hour * slot
            canvas.drawLine(x, top, x, bottom, dashed)
            if (showLabels) {
                val text = "%02d:00".format(hour)
                val tx = (x + dp(1f)).coerceAtMost(w - label.measureText(text))
                canvas.drawText(text, tx, h - dp(2f), label)
            }
        }

        // Curva de ontem.
        if (yesterday != null && yesterday.any { it > 0 }) {
            val points = yesterday.mapIndexed { i, v -> (left + (i + 0.5f) * slot) to y(v) }
            val curve = Path().apply {
                moveTo(left, points.first().second)
                lineTo(points.first().first, points.first().second)
                for (i in 1 until points.size) {
                    val (x0, y0) = points[i - 1]
                    val (x1, y1) = points[i]
                    val mx = (x0 + x1) / 2f
                    // Controles na mesma altura dos pontos: curva suave que nunca passa abaixo de zero.
                    cubicTo(mx, y0, mx, y1, x1, y1)
                }
                lineTo(right, points.last().second)
            }
            val area = Path(curve).apply {
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top, 0f, bottom, AREA_TOP, AREA_BOTTOM, Shader.TileMode.CLAMP)
            })
            canvas.drawPath(curve, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = CURVE
                strokeWidth = dp(1.6f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            })
        }

        // Barras de hoje.
        val barWidth = slot * 0.52f
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top, 0f, bottom, BAR_TOP, BAR_BOTTOM, Shader.TileMode.CLAMP)
        }
        for (hour in 0..currentHour.coerceIn(0, 23)) {
            val value = today.getOrElse(hour) { 0L }
            if (value <= 0L) continue
            val barTop = minOf(y(value), bottom - dp(2.5f))
            val x = left + hour * slot + (slot - barWidth) / 2f
            bar.alpha = if (hour == currentHour) 255 else 215
            canvas.drawRoundRect(RectF(x, barTop, x + barWidth, bottom), barWidth / 2f, barWidth / 2f, bar)
        }

        return bitmap
    }
}
