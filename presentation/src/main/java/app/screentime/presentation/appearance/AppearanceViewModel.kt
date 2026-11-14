package app.screentime.presentation.appearance

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.AccentColor
import app.screentime.domain.model.AppearanceSettings
import app.screentime.domain.model.FontChoice
import app.screentime.domain.model.ImageFocus
import app.screentime.domain.model.ThemeMode
import app.screentime.domain.model.WidgetImageTarget
import app.screentime.domain.model.WidgetStyle
import app.screentime.domain.repository.SettingsRepository
import app.screentime.domain.repository.WidgetBackgroundStore
import app.screentime.domain.repository.WidgetUpdater
import app.screentime.ui.theme.Accent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Imagem de fundo salva: caminho e versão (muda a cada imagem nova). */
data class BackgroundImage(val path: String?, val version: Long)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val backgroundStore: WidgetBackgroundStore,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    val settings: StateFlow<AppearanceSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    private val _image = MutableStateFlow(BackgroundImage(backgroundStore.imagePath(), backgroundStore.version()))
    val image: StateFlow<BackgroundImage> = _image.asStateFlow()

    /** true enquanto a foto escolhida é copiada; erro se a cópia falhar. */
    private val _imageBusy = MutableStateFlow(false)
    val imageBusy: StateFlow<Boolean> = _imageBusy.asStateFlow()
    private val _imageError = MutableStateFlow(false)
    val imageError: StateFlow<Boolean> = _imageError.asStateFlow()

    // App
    fun setTheme(mode: ThemeMode) = update(widgets = true) { it.copy(themeMode = mode) }
    fun setAccent(accent: AccentColor) = update(widgets = true) { it.copy(accent = accent) }
    fun setFont(font: FontChoice) = update { it.copy(font = font) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animations = enabled) }

    // Widgets
    fun setWidgetStyle(style: WidgetStyle) = update(widgets = true) { it.copy(widgetStyle = style) }
    fun setWidgetAppCount(count: Int) = update(widgets = true) { it.copy(widgetAppCount = count) }
    fun setWidgetComparison(show: Boolean) = update(widgets = true) { it.copy(widgetShowComparison = show) }
    fun setImageTarget(target: WidgetImageTarget) = update(widgets = true) { it.copy(widgetImage = it.widgetImage.copy(target = target)) }
    fun setImageFocus(focus: ImageFocus) = update(widgets = true) { it.copy(widgetImage = it.widgetImage.copy(focus = focus)) }

    /** Sliders: salva durante o arraste; os widgets são redesenhados ao soltar ([refreshWidgets]). */
    fun setImageDim(percent: Int) = update { it.copy(widgetImage = it.widgetImage.copy(dimPercent = percent)) }
    fun setImageBlur(px: Int) = update { it.copy(widgetImage = it.widgetImage.copy(blurPx = px)) }

    fun refreshWidgets() {
        viewModelScope.launch { widgetUpdater.updateAll() }
    }

    fun pickImage(uri: Uri) {
        viewModelScope.launch {
            _imageBusy.value = true
            _imageError.value = false
            val ok = backgroundStore.saveFrom(uri.toString())
            _imageBusy.value = false
            if (ok) {
                _image.value = BackgroundImage(backgroundStore.imagePath(), backgroundStore.version())
                repository.update { it.copy(widgetStyle = WidgetStyle.Image, widgetImage = it.widgetImage.copy(hasImage = true)) }
                widgetUpdater.updateAll()
            } else {
                _imageError.value = true
            }
        }
    }

    fun removeImage() {
        viewModelScope.launch {
            backgroundStore.clear()
            _image.value = BackgroundImage(null, 0L)
            repository.update { it.copy(widgetImage = it.widgetImage.copy(hasImage = false)) }
            widgetUpdater.updateAll()
        }
    }

    private fun update(widgets: Boolean = false, transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch {
            repository.update(transform)
            if (widgets) widgetUpdater.updateAll()
        }
    }
}

/** Liga a escolha salva (domínio) à paleta do tema (módulo :ui). */
fun AccentColor.toUi(): Accent = when (this) {
    AccentColor.Gold -> Accent.Gold
    AccentColor.Blue -> Accent.Blue
    AccentColor.Green -> Accent.Green
    AccentColor.Coral -> Accent.Coral
    AccentColor.Lilac -> Accent.Lilac
}
