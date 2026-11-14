package app.screentime.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.AccentColor
import app.screentime.domain.model.AppearanceSettings
import app.screentime.domain.model.FontChoice
import app.screentime.domain.model.ThemeMode
import app.screentime.domain.repository.SettingsRepository
import app.screentime.ui.theme.Accent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppearanceSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setAccent(accent: AccentColor) = update { it.copy(accent = accent) }
    fun setFont(font: FontChoice) = update { it.copy(font = font) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animations = enabled) }

    private fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { repository.update(transform) }
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
