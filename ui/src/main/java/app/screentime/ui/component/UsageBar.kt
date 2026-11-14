package app.screentime.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screentime.ui.theme.ScreenTimeTheme
import kotlinx.coroutines.delay

/**
 * Barra de proporção (0..1). Ao aparecer, enche da esquerda para a direita
 * depois de [delayMillis]; mudanças seguintes também animam.
 */
@Composable
fun UsageBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    thickness: Dp = 6.dp,
) {
    val motion = ScreenTimeTheme.motion
    val target = fraction.coerceIn(0f, 1f)
    val anim = remember { Animatable(if (motion) 0f else target) }
    LaunchedEffect(target, motion) {
        if (motion) {
            if (anim.value == 0f) delay(delayMillis.toLong())
            anim.animateTo(target, tween(800))
        } else {
            anim.snapTo(target)
        }
    }
    val shape = RoundedCornerShape(thickness / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(shape)
            .background(ScreenTimeTheme.colors.track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(anim.value)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
