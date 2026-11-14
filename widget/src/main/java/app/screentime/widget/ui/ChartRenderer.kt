package app.screentime.widget.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * Gráfico do widget, desenhado como imagem (widgets não aceitam desenho livre).
 * Barras = tempo de tela em cada hora de hoje; a hora atual fica mais clara.
 * Sem curva de ontem: a comparação aparece em texto, ao lado.
 */
internal object ChartRenderer {

    /** Escalas possíveis, em minutos por hora. */
    private val SCALES = listOf(10, 20, 30, 60)

    fun render(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        today: List<Long>,
        currentHour: Int,
        palette: WidgetPalette,
        scaleLabels: Boolean,
        hourLabels: Boolean,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).roundToInt().coerceAtLeast(1)
        val h = (heightDp * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        fun dp(v: Float) = v * density

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.label
            textSize = dp(9f) * context.resources.configuration.fontScale.coerceIn(0.85f, 1.3f)
        }
        val left = if (scaleLabels) label.measureText("60m") + dp(5f) else 0f
        val top = if (scaleLabels) label.textSize / 2f else dp(1f)
        val bottom = h - if (hourLabels) label.textSize + dp(4f) else dp(1f)
        val right = w.toFloat()
        val plot = (bottom - top).coerceAtLeast(1f)
        val slot = (right - left) / 24f

        val peak = (today.maxOrNull() ?: 0L) / 60_000f
        val scale = SCALES.firstOrNull { it >= peak } ?: 60
        fun y(ms: Long) = bottom - (ms / 60_000f / scale).coerceIn(0f, 1f) * plot

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.grid
            strokeWidth = dp(1f)
            pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
        }
        val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.grid; strokeWidth = dp(1f) }
        canvas.drawLine(left, top, right, top, grid)
        canvas.drawLine(left, top + plot / 2f, right, top + plot / 2f, grid)
        canvas.drawLine(left, bottom, right, bottom, axis)

        if (scaleLabels) {
            listOf("${scale}m" to top, "${scale / 2}m" to top + plot / 2f, "0" to bottom).forEach { (text, ty) ->
                canvas.drawText(text, left - dp(4f) - label.measureText(text), ty + label.textSize / 3f, label)
            }
        }
        if (hourLabels) {
            listOf(0, 6, 12, 18).forEach { hour ->
                val text = "${hour}h"
                val x = (left + hour * slot).coerceAtMost(w - label.measureText(text))
                canvas.drawText(text, x, h - dp(1f), label)
            }
        }

        val gap = dp(1.5f)
        val barW = (slot - gap).coerceAtLeast(1f)
        val radius = (barW / 2f).coerceAtMost(dp(2.5f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gradient = palette.barTop?.let {
            LinearGradient(0f, top, 0f, bottom, it, palette.bar, Shader.TileMode.CLAMP)
        }
        for (hour in 0..currentHour.coerceIn(0, 23)) {
            val v = today.getOrElse(hour) { 0L }
            if (v <= 0L) continue
            val barTop = minOf(y(v), bottom - dp(2f))
            val x = left + hour * slot + gap / 2f
            if (hour == currentHour || gradient == null) {
                paint.shader = null
                paint.color = if (hour == currentHour) palette.barNow else palette.bar
            } else {
                paint.shader = gradient
            }
            val path = Path().apply {
                addRoundRect(
                    RectF(x, barTop, x + barW, bottom),
                    floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW,
                )
            }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }
}
