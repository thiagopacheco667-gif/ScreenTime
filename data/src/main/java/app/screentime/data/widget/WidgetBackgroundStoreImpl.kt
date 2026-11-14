package app.screentime.data.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.palette.graphics.Palette
import app.screentime.domain.color.ColorMath
import app.screentime.domain.color.ImagePalette
import app.screentime.domain.repository.WidgetBackgroundStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Guarda a foto escolhida como JPEG reduzido (lado maior até [MAX_SIDE] px) na pasta
 * privada do app. Fica fora do backup e é apagada se o app for desinstalado.
 */
@Singleton
class WidgetBackgroundStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetBackgroundStore {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    override suspend fun saveFrom(uri: String): Boolean = withContext(Dispatchers.IO) {
        val bitmap = runCatching { decode(Uri.parse(uri)) }.getOrNull() ?: return@withContext false
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        val ok = runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!ok) {
            temp.delete()
            return@withContext false
        }
        val renamed = temp.renameTo(file)
        if (renamed) savePalette(extractPalette(file.absolutePath))
        renamed
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            file.delete()
            prefs.edit().clear().apply()
        }
    }

    override suspend fun palette(): ImagePalette? = withContext(Dispatchers.IO) {
        val path = imagePath() ?: return@withContext null
        val version = version()
        if (prefs.getLong(K_VERSION, -1L) == version) {
            ImagePalette(
                dark = prefs.getInt(K_DARK, 0),
                mid = prefs.getInt(K_MID, 0),
                vivid = prefs.getInt(K_VIVID, 0),
                light = prefs.getInt(K_LIGHT, 0),
                average = prefs.getInt(K_AVG, 0),
            )
        } else {
            // Imagem escolhida antes desta versão: calcula agora e guarda.
            extractPalette(path)?.also { savePalette(it) }
        }
    }

    private val prefs by lazy { context.getSharedPreferences("widget_background_palette", Context.MODE_PRIVATE) }

    private fun savePalette(p: ImagePalette?) {
        if (p == null) return
        prefs.edit()
            .putLong(K_VERSION, version())
            .putInt(K_DARK, p.dark).putInt(K_MID, p.mid).putInt(K_VIVID, p.vivid)
            .putInt(K_LIGHT, p.light).putInt(K_AVG, p.average)
            .apply()
    }

    /** Cores principais via androidx.palette, com alternativas quando a foto não tem algum tom. */
    private fun extractPalette(path: String): ImagePalette? {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
        val p = Palette.from(bmp).maximumColorCount(16).generate()
        val average = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0) or (0xFF shl 24)
        bmp.recycle()
        val dominant = p.dominantSwatch?.rgb ?: average
        val dark = p.darkMutedSwatch?.rgb ?: p.darkVibrantSwatch?.rgb ?: ColorMath.towards(dominant, ColorMath.BLACK, 0.6f)
        val vivid = p.vibrantSwatch?.rgb ?: p.lightVibrantSwatch?.rgb ?: p.mutedSwatch?.rgb ?: dominant
        val light = p.lightMutedSwatch?.rgb ?: p.lightVibrantSwatch?.rgb ?: ColorMath.towards(dominant, ColorMath.WHITE, 0.6f)
        val mid = p.mutedSwatch?.rgb ?: dominant
        return ImagePalette(dark = dark, mid = mid, vivid = vivid, light = light, average = average)
    }

    override fun imagePath(): String? = file.takeIf { it.exists() }?.absolutePath

    override fun version(): Long = file.takeIf { it.exists() }?.lastModified() ?: 0L

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder já respeita a rotação da foto (EXIF).
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }

    private companion object {
        const val K_VERSION = "version"
        const val K_DARK = "dark"
        const val K_MID = "mid"
        const val K_VIVID = "vivid"
        const val K_LIGHT = "light"
        const val K_AVG = "average"
        const val FILE_NAME = "widget_background.jpg"
        const val MAX_SIDE = 1440
    }
}
