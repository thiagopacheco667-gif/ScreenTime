package app.screentime.presentation.permission

import androidx.lifecycle.ViewModel
import app.screentime.domain.repository.UsageAccessChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Guarda se o acesso ao uso está concedido.
 * A tela chama [refresh] sempre que volta ao primeiro plano, porque o usuário
 * concede ou revoga o acesso fora do app, nas configurações do Android.
 */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val checker: UsageAccessChecker,
) : ViewModel() {

    private val _hasAccess = MutableStateFlow(checker.hasUsageAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    fun refresh() {
        _hasAccess.value = checker.hasUsageAccess()
    }
}
