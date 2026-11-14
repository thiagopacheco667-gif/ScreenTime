package app.screentime.data.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
        temp.renameTo(file)
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { file.delete() }
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
        const val FILE_NAME = "widget_background.jpg"
        const val MAX_SIDE = 1440
    }
}
