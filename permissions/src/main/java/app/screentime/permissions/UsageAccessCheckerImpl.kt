package app.screentime.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import app.screentime.domain.repository.UsageAccessChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifica o acesso especial "Acesso ao uso" (PACKAGE_USAGE_STATS).
 *
 * Essa permissão não aparece no diálogo comum do Android: ela é um "app op"
 * que o usuário liga nas configurações. Por isso a checagem é feita no
 * AppOpsManager, e não com checkSelfPermission.
 */
@Singleton
class UsageAccessCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UsageAccessChecker {

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        // As duas formas ficaram obsoletas no SDK 37, mas seguem funcionando em
        // todas as versões suportadas e são as únicas que existem do Android 8 ao 16.
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            // Em alguns aparelhos o modo fica "padrão" e a decisão real
            // passa a ser a permissão do manifesto.
            AppOpsManager.MODE_DEFAULT ->
                context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                    PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }
}
