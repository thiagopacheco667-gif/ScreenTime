package app.screentime.permissions

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Atalhos para as configurações de bateria, que decidem se a atualização em
 * segundo plano (worker e widgets) roda no horário.
 */
object BatterySettings {

    /** true se o app está liberado da otimização de bateria do Android. */
    fun isIgnoringOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Aparelhos Xiaomi, Redmi e Poco (HyperOS/MIUI), que têm controles próprios de bateria. */
    val isXiaomiFamily: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    /** Abre a lista de otimização de bateria; se não existir, as informações do app. */
    fun openOptimizationSettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) ||
            UsageAccessSettings.openAppDetails(context)

    /** Abre a tela de "Início automático" do HyperOS; se não abrir, as informações do app. */
    fun openXiaomiAutostart(context: Context): Boolean {
        val intent = Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        )
        return start(context, intent) || UsageAccessSettings.openAppDetails(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
