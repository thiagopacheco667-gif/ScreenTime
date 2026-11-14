package app.screentime.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.DayUsage
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetDayUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data object PermissionRequired : AppsUiState
    data class Error(val message: String) : AppsUiState
    data class Content(
        val days: List<LocalDate>,
        val selected: LocalDate,
        val today: LocalDate,
        val usage: DayUsage,
    ) : AppsUiState
}

/** Lista de apps de um dia, com seletor dos últimos 7 dias. */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getDayUsage: GetDayUsageUseCase,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    private var selected: LocalDate? = null
    private var job: Job? = null

    fun select(date: LocalDate) {
        selected = date
        refresh()
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            val today = time.today()
            val days = (DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }
            // Se o dia virou com a tela aberta, um dia fora da faixa volta para hoje.
            val date = selected?.takeIf { it in days } ?: today
            _state.value = try {
                AppsUiState.Content(days, date, today, getDayUsage(date))
            } catch (e: CancellationException) {
                throw e
            } catch (_: UsageAccessMissingException) {
                AppsUiState.PermissionRequired
            } catch (e: Exception) {
                AppsUiState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }

    private companion object {
        const val DAYS = 7
    }
}
