package app.screentime.data.source

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nomes dos apps e lista de pacotes que não contam como uso.
 * Os nomes ficam em memória; apps invisíveis ao sistema (Android 11+) ou
 * desinstalados ficam sem nome e aparecem pelo pacote.
 */
@Singleton
class AppInfoSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pm: PackageManager = context.packageManager
    private val labels = ConcurrentHashMap<String, Optional>()

    private data class Optional(val value: String?)

    fun labelOf(packageName: String): String? =
        labels.getOrPut(packageName) { Optional(loadLabel(packageName)) }.value

    private fun loadLabel(packageName: String): String? = try {
        val info: ApplicationInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * Pacotes que não contam como uso: os launchers (tela inicial) e a
     * interface do sistema. No Poco/Xiaomi o launcher é com.miui.home.
     */
    fun excludedPackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(home, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(home, 0)
        }.mapNotNull { it.activityInfo?.packageName }
        return launchers.toSet() + ALWAYS_EXCLUDED
    }

    private companion object {
        val ALWAYS_EXCLUDED = setOf(
            "com.android.systemui",
            "android",
        )
    }
}
