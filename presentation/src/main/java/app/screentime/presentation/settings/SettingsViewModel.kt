package app.screentime.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.StorageInfo
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.usecase.ClearHistoryUseCase
import app.screentime.domain.usecase.GetStorageInfoUseCase
import app.screentime.permissions.BatterySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class SettingsUiState(
    val storage: StorageInfo? = null,
    val ignoringBattery: Boolean = false,
    val isXiaomiFamily: Boolean = false,
    val zone: ZoneId = ZoneId.systemDefault(),
    val today: LocalDate = LocalDate.now(),
    val clearing: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getStorageInfo: GetStorageInfoUseCase,
    private val clearHistory: ClearHistoryUseCase,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                storage = getStorageInfo(),
                ignoringBattery = BatterySettings.isIgnoringOptimizations(context),
                isXiaomiFamily = BatterySettings.isXiaomiFamily,
                zone = time.zone(),
                today = time.today(),
            )
        }
    }

    fun clear() {
        viewModelScope.launch {
            _state.value = _state.value.copy(clearing = true)
            clearHistory()
            _state.value = _state.value.copy(clearing = false, storage = getStorageInfo())
        }
    }
}
