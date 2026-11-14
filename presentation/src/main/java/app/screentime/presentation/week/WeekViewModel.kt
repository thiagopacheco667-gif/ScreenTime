package app.screentime.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.screentime.domain.model.WeekUsage
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetWeekUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface WeekUiState {
    data object Loading : WeekUiState
    data object PermissionRequired : WeekUiState
    data class Error(val message: String) : WeekUiState
    data class Content(val week: WeekUsage, val offset: Int, val today: LocalDate) : WeekUiState {
        val canGoForward: Boolean get() = offset < 0
        val canGoBack: Boolean get() = offset > -MAX_WEEKS_BACK
    }
}

/** Limite de navegação para trás (um ano). */
const val MAX_WEEKS_BACK = 52

@HiltViewModel
class WeekViewModel @Inject constructor(
    private val getWeekUsage: GetWeekUsageUseCase,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow<WeekUiState>(WeekUiState.Loading)
    val state: StateFlow<WeekUiState> = _state.asStateFlow()

    private var offset = 0
    private var job: Job? = null

    fun previous() {
        if (offset > -MAX_WEEKS_BACK) {
            offset--
            refresh()
        }
    }

    fun next() {
        if (offset < 0) {
            offset++
            refresh()
        }
    }

    fun refresh() {
        job?.cancel()
        val requested = offset
        job = viewModelScope.launch {
            _state.value = try {
                WeekUiState.Content(getWeekUsage(requested), requested, time.today())
            } catch (e: CancellationException) {
                throw e
            } catch (_: UsageAccessMissingException) {
                WeekUiState.PermissionRequired
            } catch (e: Exception) {
                WeekUiState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }
}
