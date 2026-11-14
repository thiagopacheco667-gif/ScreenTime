package app.screentime.data.settings

import android.content.Context
import android.content.SharedPreferences
import app.screentime.domain.model.AppearanceSettings
import app.screentime.domain.model.WidgetImageSettings
import app.screentime.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda a aparência em SharedPreferences (arquivo próprio, fora do backup:
 * o app tem allowBackup=false). Valores desconhecidos voltam ao padrão.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("screentime_appearance", Context.MODE_PRIVATE)
    private val lock = Mutex()

    override val settings: Flow<AppearanceSettings> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun current(): AppearanceSettings = withContext(Dispatchers.IO) { read() }

    override suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        withContext(Dispatchers.IO) {
            lock.withLock { write(transform(read()).normalized()) }
        }
    }

    private fun read(): AppearanceSettings {
        val d = AppearanceSettings()
        return AppearanceSettings(
            themeMode = enumOr(prefs.getString(K_THEME, null), d.themeMode),
            accent = enumOr(prefs.getString(K_ACCENT, null), d.accent),
            font = enumOr(prefs.getString(K_FONT, null), d.font),
            animations = prefs.getBoolean(K_ANIM, d.animations),
            widgetStyle = enumOr(prefs.getString(K_W_STYLE, null), d.widgetStyle),
            widgetAppCount = prefs.getInt(K_W_COUNT, d.widgetAppCount),
            widgetShowComparison = prefs.getBoolean(K_W_CMP, d.widgetShowComparison),
            widgetImage = WidgetImageSettings(
                hasImage = prefs.getBoolean(K_IMG_HAS, d.widgetImage.hasImage),
                dimPercent = prefs.getInt(K_IMG_DIM, d.widgetImage.dimPercent),
                blurPx = prefs.getInt(K_IMG_BLUR, d.widgetImage.blurPx),
                target = enumOr(prefs.getString(K_IMG_TARGET, null), d.widgetImage.target),
            ),
        ).normalized()
    }

    private fun write(s: AppearanceSettings) {
        prefs.edit()
            .putString(K_THEME, s.themeMode.name)
            .putString(K_ACCENT, s.accent.name)
            .putString(K_FONT, s.font.name)
            .putBoolean(K_ANIM, s.animations)
            .putString(K_W_STYLE, s.widgetStyle.name)
            .putInt(K_W_COUNT, s.widgetAppCount)
            .putBoolean(K_W_CMP, s.widgetShowComparison)
            .putBoolean(K_IMG_HAS, s.widgetImage.hasImage)
            .putInt(K_IMG_DIM, s.widgetImage.dimPercent)
            .putInt(K_IMG_BLUR, s.widgetImage.blurPx)
            .putString(K_IMG_TARGET, s.widgetImage.target.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private companion object {
        const val K_THEME = "theme_mode"
        const val K_ACCENT = "accent"
        const val K_FONT = "font"
        const val K_ANIM = "animations"
        const val K_W_STYLE = "widget_style"
        const val K_W_COUNT = "widget_app_count"
        const val K_W_CMP = "widget_show_comparison"
        const val K_IMG_HAS = "widget_image_has"
        const val K_IMG_DIM = "widget_image_dim"
        const val K_IMG_BLUR = "widget_image_blur"
        const val K_IMG_TARGET = "widget_image_target"
    }
}

