package app.screentime.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.screentime.domain.model.AppDetail
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetAppDetailUseCase
import app.screentime.presentation.navigation.AppDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState
    data object PermissionRequired : AppDetailUiState
    data class Error(val message: String) : AppDetailUiState
    data class Content(val detail: AppDetail, val today: LocalDate, val zone: ZoneId) : AppDetailUiState
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAppDetail: GetAppDetailUseCase,
    private val time: TimeSource,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppDetailDestination>()
    private val date: LocalDate = LocalDate.ofEpochDay(route.epochDay)

    private val _state = MutableStateFlow<AppDetailUiState>(AppDetailUiState.Loading)
    val state: StateFlow<AppDetailUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = try {
                AppDetailUiState.Content(getAppDetail(route.packageName, date), time.today(), time.zone())
            } catch (e: CancellationException) {
                throw e
            } catch (_: UsageAccessMissingException) {
                AppDetailUiState.PermissionRequired
            } catch (e: Exception) {
                AppDetailUiState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }
}
