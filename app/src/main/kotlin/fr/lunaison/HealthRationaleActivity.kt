package fr.lunaison

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.lunaison.core.designsystem.LunaisonTheme
import fr.lunaison.ui.HealthRationaleScreen

/**
 * Justification des permissions Health Connect, ouverte par Health Connect lui-même
 * (ACTION_SHOW_PERMISSIONS_RATIONALE, ou VIEW_PERMISSION_USAGE via l'alias du manifeste).
 * Écran d'explication seulement : il ne lit rien et n'accède à aucune donnée.
 */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunaisonTheme(palette = "PLUM") {
                HealthRationaleScreen(onClose = { finish() })
            }
        }
    }
}
