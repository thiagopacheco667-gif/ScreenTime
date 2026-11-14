package app.screentime.presentation.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.screentime.presentation.R
import app.screentime.ui.component.BulletRow
import app.screentime.ui.component.InfoCard
import app.screentime.ui.component.NumberedStep
import app.screentime.ui.theme.ScreenTimeTheme

/**
 * Explica a permissão e leva o usuário até a tela certa das configurações.
 *
 * @param settingsFailed true quando nenhuma tela de configurações abriu.
 */
@Composable
fun PermissionScreen(
    settingsFailed: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.perm_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.perm_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        InfoCard(title = stringResource(R.string.perm_reads_title)) {
            BulletRow(stringResource(R.string.perm_reads_1))
            BulletRow(stringResource(R.string.perm_reads_2))
            BulletRow(stringResource(R.string.perm_reads_3))
        }

        InfoCard(title = stringResource(R.string.perm_never_title)) {
            BulletRow(
                stringResource(R.string.perm_never_1),
                dotColor = MaterialTheme.colorScheme.outline,
            )
            BulletRow(
                stringResource(R.string.perm_never_2),
                dotColor = MaterialTheme.colorScheme.outline,
            )
        }

        InfoCard(title = stringResource(R.string.perm_privacy_title)) {
            BulletRow(stringResource(R.string.perm_privacy_1))
            BulletRow(stringResource(R.string.perm_privacy_2))
        }

        InfoCard(title = stringResource(R.string.perm_steps_title)) {
            NumberedStep(1, stringResource(R.string.perm_step_1))
            NumberedStep(2, stringResource(R.string.perm_step_2))
            NumberedStep(3, stringResource(R.string.perm_step_3))
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.perm_open_settings))
        }

        if (settingsFailed) {
            Text(
                text = stringResource(R.string.perm_settings_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        InfoCard(title = stringResource(R.string.perm_restricted_title)) {
            Text(
                text = stringResource(R.string.perm_restricted_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onOpenAppInfo) {
                Text(stringResource(R.string.perm_open_app_info))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionScreenPreview() {
    ScreenTimeTheme {
        PermissionScreen(settingsFailed = false, onOpenSettings = {}, onOpenAppInfo = {})
    }
}
