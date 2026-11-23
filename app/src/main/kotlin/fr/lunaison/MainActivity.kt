package fr.lunaison

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.lunaison.reminders.ReminderWorker
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.lunaison.core.designsystem.LunaisonTheme
import fr.lunaison.ui.LunaisonApp

/** FragmentActivity plutôt que ComponentActivity : la demande d'empreinte du système en a besoin. */
class MainActivity : FragmentActivity() {

    /** Écran demandé par une notification, consommé une fois la navigation faite. */
    private var pendingLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingLink = intent.getStringExtra(ReminderWorker.EXTRA_OPEN)
        val app = application as LunaisonApplication
        val repository = app.repository
        val biometric = AndroidBiometricUnlock(this)
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
            val hasPin = settings?.pinHash != null
            LaunchedEffect(hasPin) {
                if (hasPin) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            LunaisonTheme(palette = settings?.themePalette?.name ?: "PLUM") {
                LunaisonApp(
                    repository,
                    BuildConfig.VERSION_NAME,
                    link = pendingLink,
                    onLinkHandled = { pendingLink = null },
                    decoyRepository = { app.decoyRepository },
                    biometric = biometric,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLink = intent.getStringExtra(ReminderWorker.EXTRA_OPEN)
    }
}
