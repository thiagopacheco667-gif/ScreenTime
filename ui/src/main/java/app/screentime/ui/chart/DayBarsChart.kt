package app.screentime.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screentime.ui.theme.ScreenTimeTheme

/** Escalas do eixo em minutos: 10, 20, 30 min, depois horas. */
private val SCALES_MIN = listOf(10, 20, 30, 60, 120, 240, 360, 480, 600, 720, 840, 960, 1200, 1440)
private const val BAR_MS = 650f
private const val STAGGER_MS = 60f

/** Rótulo do eixo: "30 min", "2 h", "14 h". */
fun axisLabel(minutes: Int): String = if (minutes < 60) "$minutes min" else "${minutes / 60} h"

/**
 * Barras por dia (semana, ou últimos 7 dias de um app), com escala ao lado.
 *
 * - [values]: milissegundos; nulo = sem dados (traço pontilhado, nunca confundido com zero).
 * - [isFuture]: dia que ainda não chegou (contorno tracejado vazio).
 * - Linha tracejada na cor de destaque = [averageMillis].
 * - Com [onSelect], tocar numa barra escolhe o dia (com vibração leve).
 */
@Composable
fun DayBarsChart(
    values: List<Long?>,
    labels: List<String>,
    selectedIndex: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isFuture: List<Boolean> = values.map { false },
    averageMillis: Long? = null,
    onSelect: ((Int) -> Unit)? = null,
    height: Dp = 170.dp,
) {
    val c = ScreenTimeTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val motion = ScreenTimeTheme.motion
    val haptic = LocalHapticFeedback.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val count = values.size.coerceAtLeast(1)

    val peakMin = maxOf(values.filterNotNull().maxOrNull() ?: 0L, averageMillis ?: 0L) / 60_000f
    val scale = SCALES_MIN.firstOrNull { it >= peakMin } ?: SCALES_MIN.last()
    val animatedScale by animateFloatAsState(scale.toFloat(), tween(if (motion) 400 else 0), label = "day-scale")
    val total = BAR_MS + STAGGER_MS * (count - 1)
    val clock = remember { Animatable(if (motion) 0f else total) }
    LaunchedEffect(Unit) { if (motion) clock.animateTo(total, tween(total.toInt(), easing = LinearEasing)) }

    val labelStyle = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier.fillMaxWidth()) {
        Column(
            Modifier.width(40.dp).height(height),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(axisLabel(scale), style = labelStyle, color = muted, textAlign = TextAlign.End)
            Text(axisLabel(scale / 2), style = labelStyle, color = muted, textAlign = TextAlign.End)
            Text("0", style = labelStyle, color = muted)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics { this.contentDescription = contentDescription }
                    .pointerInput(onSelect != null) {
                        if (onSelect == null) return@pointerInput
                        detectTapGestures { offset ->
                            val i = (offset.x / size.width * count).toInt().coerceIn(0, count - 1)
                            if (values.getOrNull(i) != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect?.invoke(i)
                            }
                        }
                    },
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                val line = 1.dp.toPx()
                drawLine(c.chartGrid, Offset(0f, 0f), Offset(size.width, 0f), line, pathEffect = dash)
                drawLine(c.chartGrid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), line, pathEffect = dash)

                val slot = size.width / count
                val barW = (slot * 0.62f).coerceAtMost(34.dp.toPx())
                val r = 8.dp.toPx().coerceAtMost(barW / 2)
                fun barPath(x: Float, h: Float) = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = x, top = size.height - h, right = x + barW, bottom = size.height,
                            topLeftCornerRadius = CornerRadius(r, r), topRightCornerRadius = CornerRadius(r, r),
                            bottomLeftCornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            bottomRightCornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        ),
                    )
                }

                values.forEachIndexed { i, v ->
                    val x = i * slot + (slot - barW) / 2
                    when {
                        isFuture.getOrElse(i) { false } -> drawPath(
                            barPath(x, size.height * 0.18f),
                            c.chartAxis,
                            style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                        )
                        v == null -> drawLine(
                            c.chartGhost,
                            Offset(x, size.height - line),
                            Offset(x + barW, size.height - line),
                            2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
                        )
                        else -> {
                            val grow = FastOutSlowInEasing.transform(((clock.value - i * STAGGER_MS) / BAR_MS).coerceIn(0f, 1f))
                            val h = ((v / 60_000f / animatedScale).coerceIn(0f, 1f) * size.height * grow).coerceAtLeast(3.dp.toPx())
                            drawPath(barPath(x, h), if (i == selectedIndex) accent else c.chartBar)
                        }
                    }
                }
                drawLine(c.chartAxis, Offset(0f, size.height), Offset(size.width, size.height), line)

                if (averageMillis != null && averageMillis > 0) {
                    val y = size.height - (averageMillis / 60_000f / animatedScale).coerceIn(0f, 1f) * size.height
                    drawLine(
                        accent.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, label ->
                    Text(
                        label,
                        style = labelStyle.copy(fontWeight = if (i == selectedIndex) FontWeight.ExtraBold else FontWeight.Medium),
                        color = if (i == selectedIndex) MaterialTheme.colorScheme.onSurface else muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
