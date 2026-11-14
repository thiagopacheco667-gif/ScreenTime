package app.screentime.ui.component

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cache em memória dos ícones, para a lista rolar sem recarregar. */
private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(200)

    fun peek(key: String): ImageBitmap? = cache.get(key)

    fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        val key = "$packageName@$sizePx"
        cache.get(key)?.let { return it }
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(sizePx, sizePx).asImageBitmap().also { cache.put(key, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/**
 * Ícone real do app, carregado fora da thread principal.
 * Apps desinstalados ou invisíveis ao sistema mostram a inicial do nome.
 */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState(
        initialValue = AppIconCache.peek("$packageName@$sizePx"),
        packageName,
        sizePx,
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { AppIconCache.load(context, packageName, sizePx) }
        }
    }
    val shape = RoundedCornerShape(size * 0.28f)
    val current = icon
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
