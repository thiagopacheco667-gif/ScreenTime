package app.screentime.presentation.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.screentime.domain.model.DayUsage
import app.screentime.domain.repository.TimeSource
import app.screentime.domain.repository.UsageAccessMissingException
import app.screentime.domain.usecase.GetDayUsageUseCase
import app.screentime.presentation.R
import app.screentime.presentation.common.ErrorState
import app.screentime.presentation.common.LoadingState
import app.screentime.presentation.common.dayLabel
import app.screentime.presentation.common.formatDayMonth
import app.screentime.presentation.navigation.DayAppsDestination
import app.screentime.ui.icons.ScreenTimeIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface DayAppsUiState {
    data object Loading : DayAppsUiState
    data object PermissionRequired : DayAppsUiState
    data class Error(val message: String) : DayAppsUiState
    data class Content(val usage: DayUsage, val today: LocalDate) : DayAppsUiState
}

/** Apps de um dia qualquer, aberto a partir da semana. */
@HiltViewModel
class DayAppsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDayUsage: GetDayUsageUseCase,
    private val time: TimeSource,
) : ViewModel() {

    val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayAppsDestination>().epochDay)

    private val _state = MutableStateFlow<DayAppsUiState>(DayAppsUiState.Loading)
    val state: StateFlow<DayAppsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = try {
                DayAppsUiState.Content(getDayUsage(date), time.today())
            } catch (e: CancellationException) {
                throw e
            } catch (_: UsageAccessMissingException) {
                DayAppsUiState.PermissionRequired
            } catch (e: Exception) {
                DayAppsUiState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }
}

@Composable
fun DayAppsRoute(
    viewModel: DayAppsViewModel,
    onBack: () -> Unit,
    onOpenApp: (String, LocalDate) -> Unit,
    onPermissionLost: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when (val s = state) {
        DayAppsUiState.Loading -> LoadingState()
        DayAppsUiState.PermissionRequired -> LaunchedEffect(Unit) { onPermissionLost() }
        is DayAppsUiState.Error -> ErrorState(s.message, viewModel::refresh)
        is DayAppsUiState.Content -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(ScreenTimeIcons.Back, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                    Text(
                        text = dayLabel(viewModel.date, s.today).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = formatDayMonth(viewModel.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            dayAppItems(s.usage, viewModel.date, onOpenApp)
        }
    }
}
