package app.screentime.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.DayUsage
import app.screentime.domain.model.UsageEvent
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetDayEventsUseCase
import app.screentime.domain.usecase.GetDayUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DiagnosticsUiState {
    data object Loading : DiagnosticsUiState
    data object PermissionRequired : DiagnosticsUiState
    data class Error(val message: String) : DiagnosticsUiState
    data class Content(
        val today: DayUsage,
        /** Eventos mais recentes primeiro, limitados a [MAX_EVENTS]. */
        val recentEvents: List<UsageEvent>,
        val totalEvents: Int,
    ) : DiagnosticsUiState
}

const val MAX_EVENTS = 300

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val getDayUsage: GetDayUsageUseCase,
    private val getDayEvents: GetDayEventsUseCase,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow<DiagnosticsUiState>(DiagnosticsUiState.Loading)
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = try {
                val today = time.today()
                val usage = getDayUsage(today)
                val events = getDayEvents(today)
                DiagnosticsUiState.Content(
                    today = usage,
                    recentEvents = events.takeLast(MAX_EVENTS).asReversed(),
                    totalEvents = events.size,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: UsageAccessMissingException) {
                DiagnosticsUiState.PermissionRequired
            } catch (e: Exception) {
                DiagnosticsUiState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }
}
