package app.screentime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import app.screentime.presentation.ScreenTimeRoot
import app.screentime.ui.theme.ScreenTimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Aumenta a cada pedido de "abrir Ajustes" vindo do widget (engrenagem). */
    private val openSettingsRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleWidgetIntent(intent)
        setContent {
            ScreenTimeTheme {
                ScreenTimeRoot(openSettingsRequest = openSettingsRequests.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.data?.scheme == "screentime" && intent.data?.host == "settings") {
            openSettingsRequests.intValue += 1
        }
    }
}
