package app.screentime.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Abre a tela do sistema onde o usuário liga o "Acesso ao uso".
 *
 * Fabricantes mudam essa tela, então há três tentativas:
 * 1. A página do próprio ScreenTime (Android 10+ em muitos aparelhos).
 * 2. A lista geral de apps com acesso ao uso.
 * 3. As configurações principais, como último recurso.
 *
 * @return true se alguma tela abriu.
 */
object UsageAccessSettings {

    fun open(context: Context): Boolean {
        val candidates = listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null)),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                // tenta a próxima
            } catch (_: SecurityException) {
                // alguns fabricantes protegem a tela por pacote; tenta a próxima
            }
        }
        return false
    }

    /** Abre a página de informações do app (usada para "Permitir configurações restritas"). */
    fun openAppDetails(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
