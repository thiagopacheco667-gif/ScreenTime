package app.screentime.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screentime.ui.theme.ScreenTimeTheme
import kotlin.math.max

/** Escalas possíveis do eixo, em minutos por hora. */
private val SCALES = listOf(10, 20, 30, 60)
private const val BAR_MS = 700f
private const val STAGGER_MS = 22f
private const val TOTAL_MS = BAR_MS + STAGGER_MS * 23

/**
 * Gráfico das 24 horas com escala em minutos.
 *
 * - Barras cheias: minutos de tela em cada hora de hoje (a hora atual mais clara,
 *   a selecionada na cor de destaque).
 * - Contorno tracejado: a mesma hora ontem, quando [showYesterday] está ligado.
 * - Tocar ou arrastar o dedo escolhe a hora (com vibração leve a cada troca).
 * - Ao abrir, as barras crescem uma depois da outra (sem animação se o movimento estiver desligado).
 */
@Composable
fun HourlyUsageChart(
    today: List<Long>,
    yesterday: List<Long>?,
    showYesterday: Boolean,
    selectedHour: Int?,
    currentHour: Int?,
    onSelect: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val c = ScreenTimeTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val motion = ScreenTimeTheme.motion
    val haptic = LocalHapticFeedback.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selectedHour)

    val ghostVisible = showYesterday && yesterday != null
    val ghostAlpha by animateFloatAsState(
        targetValue = if (ghostVisible) 1f else 0f,
        animationSpec = tween(if (motion) 300 else 0),
        label = "ghost",
    )
    val peakMinutes = max(
        today.maxOrNull() ?: 0L,
        if (ghostVisible) yesterday?.maxOrNull() ?: 0L else 0L,
    ) / 60_000f
    val scale = SCALES.firstOrNull { it >= peakMinutes } ?: 60
    val animatedScale by animateFloatAsState(
        targetValue = scale.toFloat(),
        animationSpec = tween(if (motion) 400 else 0),
        label = "scale",
    )
    val clock = remember { Animatable(if (motion) 0f else TOTAL_MS) }
    LaunchedEffect(Unit) {
        if (motion) clock.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
    }

    val labelStyle = MaterialTheme.typography.bodySmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.width(40.dp).height(height),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text("$scale min", style = labelStyle, color = labelColor, textAlign = TextAlign.End)
            Text("${scale / 2} min", style = labelStyle, color = labelColor, textAlign = TextAlign.End)
            Text("0", style = labelStyle, color = labelColor, textAlign = TextAlign.End)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics { this.contentDescription = contentDescription }
                    .pointerInput(Unit) {
                        fun pick(x: Float) {
                            val hour = (x / size.width * 24).toInt().coerceIn(0, 23)
                            if (hour != currentSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(hour)
                            }
                        }
                        detectTapGestures { pick(it.x) }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            val hour = (change.position.x / size.width * 24).toInt().coerceIn(0, 23)
                            if (hour != currentSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(hour)
                            }
                        }
                    },
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                val grid = 1.dp.toPx()
                drawLine(c.chartGrid, Offset(0f, 0f), Offset(size.width, 0f), grid, pathEffect = dash)
                drawLine(c.chartGrid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), grid, pathEffect = dash)

                val slot = size.width / 24f
                val gap = 3.dp.toPx()
                val barWidth = (slot - gap).coerceAtLeast(1f)
                val radius = (barWidth / 2f).coerceAtMost(4.dp.toPx())
                val stub = 2.dp.toPx()
                fun heightOf(millis: Long) = (millis / 60_000f / animatedScale).coerceIn(0f, 1f) * size.height

                for (hour in 0 until 24) {
                    val x = hour * slot + gap / 2f

                    if (ghostAlpha > 0f && yesterday != null) {
                        val gh = heightOf(yesterday.getOrElse(hour) { 0L })
                        if (gh > 0f) {
                            drawPath(
                                path = topRounded(x, size.height - gh, barWidth, gh, radius),
                                color = c.chartGhost.copy(alpha = ghostAlpha),
                                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                            )
                        }
                    }

                    val raw = ((clock.value - hour * STAGGER_MS) / BAR_MS).coerceIn(0f, 1f)
                    val grow = FastOutSlowInEasing.transform(raw)
                    val value = today.getOrElse(hour) { 0L }
                    val h = heightOf(value) * grow
                    val color: Color = when (hour) {
                        selectedHour -> accent
                        currentHour -> c.chartBarNow
                        else -> c.chartBar
                    }
                    if (h < stub) {
                        drawRect(if (hour == selectedHour) accent else c.chartAxis, Offset(x, size.height - stub), Size(barWidth, stub))
                    } else {
                        drawPath(topRounded(x, size.height - h, barWidth, h, radius), color)
                    }
                }
                drawLine(c.chartAxis, Offset(0f, size.height), Offset(size.width, size.height), grid)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0h", "6h", "12h", "18h", "23h").forEach {
                    Text(it, style = labelStyle, color = labelColor)
                }
            }
        }
    }
}

/** Retângulo com só os cantos de cima arredondados. */
private fun topRounded(x: Float, y: Float, w: Float, h: Float, r: Float): Path = Path().apply {
    addRoundRect(
        RoundRect(
            left = x, top = y, right = x + w, bottom = y + h,
            topLeftCornerRadius = CornerRadius(r, r),
            topRightCornerRadius = CornerRadius(r, r),
            bottomRightCornerRadius = CornerRadius.Zero,
            bottomLeftCornerRadius = CornerRadius.Zero,
        ),
    )
}
