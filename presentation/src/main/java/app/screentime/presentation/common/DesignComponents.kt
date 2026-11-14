package app.screentime.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screentime.ui.theme.ScreenTimeTheme

/** Cartão de destaque com gradiente (o número principal de cada tela). */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ScreenTimeTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .border(1.dp, c.heroBorder, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Cartão comum das seções. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ScreenTimeTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

/** Rótulo pequeno em caixa alta acima de um grupo. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Valor que sobe contando ao aparecer e desliza nas mudanças seguintes. */
@Composable
fun animatedMillis(target: Long): Long {
    val motion = ScreenTimeTheme.motion
    val anim = remember { Animatable(if (motion) 0f else target.toFloat()) }
    LaunchedEffect(target, motion) {
        if (motion) {
            val duration = if (anim.value == 0f) 900 else 500
            anim.animateTo(target.toFloat(), tween(duration, easing = FastOutSlowInEasing))
        } else {
            anim.snapTo(target.toFloat())
        }
    }
    return anim.value.toLong()
}
