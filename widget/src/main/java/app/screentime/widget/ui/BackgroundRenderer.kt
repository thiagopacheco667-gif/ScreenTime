package app.screentime.widget.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import app.screentime.domain.model.ImageFocus
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Monta o fundo com imagem: corta no formato do widget (respeitando o enquadramento),
 * desfoca, escurece e arredonda os cantos. Os cantos vêm prontos na imagem porque
 * o arredondamento do widget só existe a partir do Android 12.
 */
internal object BackgroundRenderer {

    /** Lado maior da imagem do widget: suficiente para nitidez, leve para o limite de memória. */
    private const val MAX_SIDE = 720
    private val cache = LruCache<String, Bitmap>(4)

    fun render(
        context: Context,
        path: String,
        version: Long,
        widthDp: Float,
        heightDp: Float,
        focus: ImageFocus,
        dimPercent: Int,
        blurDp: Int,
        cornerDp: Float,
    ): Bitmap? {
        val density = context.resources.displayMetrics.density
        var w = (widthDp * density).roundToInt().coerceAtLeast(1)
        var h = (heightDp * density).roundToInt().coerceAtLeast(1)
        val shrink = MAX_SIDE.toFloat() / max(w, h)
        val pxScale = if (shrink < 1f) shrink else 1f
        w = (w * pxScale).roundToInt().coerceAtLeast(1)
        h = (h * pxScale).roundToInt().coerceAtLeast(1)

        val key = "$path|$version|$w|$h|$focus|$dimPercent|$blurDp|$cornerDp"
        cache.get(key)?.let { return it }

        val source = decode(path, w, h) ?: return null

        // Corte "preencher" com o enquadramento escolhido.
        val cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val scale = max(w.toFloat() / source.width, h.toFloat() / source.height)
        val sw = source.width * scale
        val sh = source.height * scale
        val dx = (w - sw) / 2f
        val dy = when (focus) {
            ImageFocus.Top -> 0f
            ImageFocus.Center -> (h - sh) / 2f
            ImageFocus.Bottom -> h - sh
        }
        Canvas(cropped).drawBitmap(
            source,
            Matrix().apply { setScale(scale, scale); postTranslate(dx, dy) },
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        source.recycle()

        // Desfoque aproximado: reduzir e ampliar com filtro, duas vezes.
        var current = cropped
        val factor = 1f + blurDp * density * pxScale / 3f
        if (blurDp > 0 && factor >= 1.05f) {
            repeat(2) {
                val small = Bitmap.createScaledBitmap(
                    current,
                    (w / factor).roundToInt().coerceAtLeast(1),
                    (h / factor).roundToInt().coerceAtLeast(1),
                    true,
                )
                val back = Bitmap.createScaledBitmap(small, w, h, true)
                if (small !== back && small !== current) small.recycle()
                if (current !== cropped && current !== back) current.recycle()
                current = back
            }
        }

        // Cantos arredondados + escurecimento, para o texto continuar legível.
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val r = cornerDp * density * pxScale
        canvas.drawRoundRect(
            rect, r, r,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = BitmapShader(current, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            },
        )
        val alpha = (dimPercent.coerceIn(0, 100) * 2.55f).roundToInt()
        canvas.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(alpha, 8, 10, 20) })
        if (current !== cropped) current.recycle()
        cropped.recycle()
        cache.put(key, out)
        return out
    }

    private fun decode(path: String, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
