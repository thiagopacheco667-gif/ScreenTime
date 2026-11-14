package app.screentime.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.permissions.BatterySettings
import app.screentime.permissions.UsageAccessSettings
import app.screentime.presentation.R
import app.screentime.presentation.common.dayLabel
import app.screentime.presentation.common.formatClock
import app.screentime.presentation.common.formatDayMonth
import app.screentime.ui.component.BulletRow
import app.screentime.ui.component.InfoCard
import app.screentime.ui.component.NumberedStep
import app.screentime.ui.icons.ScreenTimeIcons

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    // Volta das configurações do sistema → relê o status da bateria.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_title)) },
            text = { Text(stringResource(R.string.settings_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text(stringResource(R.string.settings_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(ScreenTimeIcons.Back, contentDescription = stringResource(R.string.nav_back))
        }
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        // Segundo plano e bateria
        InfoCard(title = stringResource(R.string.settings_background_title)) {
            StatusPill(
                ok = state.ignoringBattery,
                text = stringResource(
                    if (state.ignoringBattery) R.string.settings_battery_ok else R.string.settings_battery_restricted,
                ),
            )
            Text(
                text = stringResource(R.string.settings_background_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            val last = state.storage?.lastBackgroundSyncAt
            Text(
                text = if (last == null) {
                    stringResource(R.string.settings_last_sync_never)
                } else {
                    val date = last.atZone(state.zone).toLocalDate()
                    stringResource(R.string.settings_last_sync, dayLabel(date, state.today).lowercase(), formatClock(last, state.zone))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { BatterySettings.openOptimizationSettings(context) },
                shape = MaterialTheme.shapes.medium,
            ) { Text(stringResource(R.string.settings_open_battery)) }
        }

        if (state.isXiaomiFamily) {
            InfoCard(title = stringResource(R.string.settings_xiaomi_title)) {
                NumberedStep(1, stringResource(R.string.settings_xiaomi_1))
                NumberedStep(2, stringResource(R.string.settings_xiaomi_2))
                NumberedStep(3, stringResource(R.string.settings_xiaomi_3))
                NumberedStep(4, stringResource(R.string.settings_xiaomi_4))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { UsageAccessSettings.openAppDetails(context) },
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(stringResource(R.string.settings_open_app_info)) }
                    OutlinedButton(
                        onClick = { BatterySettings.openXiaomiAutostart(context) },
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(stringResource(R.string.settings_open_autostart)) }
                }
            }
        }

        // Histórico salvo
        InfoCard(title = stringResource(R.string.settings_history_title)) {
            val storage = state.storage
            val oldest = storage?.oldestDate
            Text(
                text = if (storage == null || storage.daysStored == 0 || oldest == null) {
                    stringResource(R.string.settings_history_empty)
                } else {
                    pluralStringResource(
                        R.plurals.settings_history_days,
                        storage.daysStored,
                        storage.daysStored,
                        formatDayMonth(oldest),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.settings_history_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = !state.clearing && (state.storage?.daysStored ?: 0) > 0,
                shape = MaterialTheme.shapes.medium,
            ) { Text(stringResource(R.string.settings_clear_button)) }
        }

        // Critérios do cálculo
        InfoCard(title = stringResource(R.string.settings_method_title)) {
            BulletRow(stringResource(R.string.settings_method_1))
            BulletRow(stringResource(R.string.settings_method_2))
            BulletRow(stringResource(R.string.settings_method_3))
            BulletRow(stringResource(R.string.settings_method_4))
            BulletRow(stringResource(R.string.settings_method_5))
            BulletRow(stringResource(R.string.settings_method_6))
            Text(
                text = stringResource(R.string.settings_method_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoCard(title = stringResource(R.string.perm_privacy_title)) {
            BulletRow(stringResource(R.string.perm_privacy_1))
            BulletRow(stringResource(R.string.perm_privacy_2))
            BulletRow(stringResource(R.string.settings_privacy_3))
        }

        InfoCard(title = stringResource(R.string.settings_tools_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenDiagnostics, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.settings_diagnostics))
                }
                OutlinedButton(
                    onClick = { UsageAccessSettings.open(context) },
                    shape = MaterialTheme.shapes.medium,
                ) { Text(stringResource(R.string.granted_open_settings)) }
            }
        }
    }
}

@Composable
private fun StatusPill(ok: Boolean, text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
