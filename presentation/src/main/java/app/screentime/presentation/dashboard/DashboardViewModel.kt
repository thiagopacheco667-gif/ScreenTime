package app.screentime.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.Dashboard
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object PermissionRequired : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Content(val dashboard: Dashboard) : DashboardUiState
}

/**
 * Carrega o painel e, enquanto a tela está visível, recalcula a cada minuto
 * para o tempo de hoje acompanhar o uso.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboard: GetDashboardUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var ticker: Job? = null

    fun onVisible() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                load()
                delay(REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    fun onHidden() {
        ticker?.cancel()
        ticker = null
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _state.value = try {
            DashboardUiState.Content(getDashboard())
        } catch (e: CancellationException) {
            throw e
        } catch (_: UsageAccessMissingException) {
            DashboardUiState.PermissionRequired
        } catch (e: Exception) {
            DashboardUiState.Error(e.message ?: e::class.java.simpleName)
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
