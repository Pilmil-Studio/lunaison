package fr.lunaison.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.lunaison.core.model.tr

/**
 * Écran de justification des permissions Health Connect (ACTION_SHOW_PERMISSIONS_RATIONALE,
 * VIEW_PERMISSION_USAGE). Explique en clair ce que Lunaison lit, pourquoi, et ce qu'elle ne fait pas.
 */
@Composable
fun HealthRationaleScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                tr("Pourquoi Lunaison lit Health Connect", "Why Lunaison reads Health Connect", pl = "Dlaczego Lunaison czyta Health Connect", uk = "Чому Lunaison читає Health Connect", es = "Por qué Lunaison lee Health Connect", de = "Warum Lunaison Health Connect liest"),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                tr(
                    fr = "Lunaison demande une seule autorisation, en lecture : les données de règles (périodes et flux). Elle sert uniquement à importer votre historique depuis une autre appli, quand vous le demandez dans les réglages, rubrique d'import.",
                    en = "Lunaison asks for a single read permission: menstruation data (periods and flow). It is only used to import your history from another app, when you ask for it in the settings import section.",
                    pl = "Lunaison prosi o jedno uprawnienie do odczytu: dane o miesiączkach (okresy i natężenie). Służy tylko do zaimportowania historii z innej aplikacji, na Twoją prośbę, w ustawieniach importu.",
                    uk = "Lunaison просить один дозвіл на читання: дані про менструації (періоди й інтенсивність). Він потрібен лише для імпорту вашої історії з іншого застосунку, коли ви цього просите в розділі імпорту налаштувань.",
                    es = "Lunaison pide un único permiso de lectura: los datos de menstruación (periodos y flujo). Solo sirve para importar tu historial desde otra aplicación, cuando lo pides en la sección de importación de los ajustes.",
                    de = "Lunaison fragt nur eine Leseberechtigung an: Menstruationsdaten (Perioden und Stärke). Sie dient allein dazu, deinen Verlauf aus einer anderen App zu importieren, wenn du es in den Einstellungen beim Import verlangst.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Avant toute écriture, Lunaison vous montre un aperçu et vous demandez confirmation. Vos saisies dans Lunaison ne sont jamais écrasées. Lunaison n'écrit rien dans Health Connect.",
                    en = "Before anything is written, Lunaison shows you a preview and asks for your confirmation. What you logged in Lunaison is never overwritten. Lunaison writes nothing to Health Connect.",
                    pl = "Zanim cokolwiek zostanie zapisane, Lunaison pokazuje podgląd i prosi o potwierdzenie. To, co zapisano w Lunaison, nigdy nie jest nadpisywane. Lunaison nic nie zapisuje w Health Connect.",
                    uk = "Перш ніж щось записати, Lunaison показує попередній перегляд і просить підтвердження. Ваші записи в Lunaison ніколи не перезаписуються. Lunaison нічого не записує в Health Connect.",
                    es = "Antes de escribir nada, Lunaison te muestra una vista previa y pide confirmación. Lo que anotaste en Lunaison nunca se sobrescribe. Lunaison no escribe nada en Health Connect.",
                    de = "Bevor etwas geschrieben wird, zeigt Lunaison eine Vorschau und bittet um Bestätigung. Was du in Lunaison eingetragen hast, wird nie überschrieben. Lunaison schreibt nichts in Health Connect.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Lunaison n'a pas d'accès à Internet : les données lues restent sur ce téléphone et ne sont partagées avec personne. Vous pouvez retirer l'autorisation à tout moment dans les réglages de Health Connect.",
                    en = "Lunaison has no Internet access: the data it reads stays on this phone and is shared with no one. You can remove the permission at any time in Health Connect settings.",
                    pl = "Lunaison nie ma dostępu do internetu: odczytane dane zostają na tym telefonie i nie są nikomu udostępniane. Zgodę możesz cofnąć w każdej chwili w ustawieniach Health Connect.",
                    uk = "Lunaison не має доступу до інтернету: прочитані дані залишаються на цьому телефоні й нікому не передаються. Дозвіл можна відкликати будь-коли в налаштуваннях Health Connect.",
                    es = "Lunaison no tiene acceso a Internet: los datos leídos se quedan en este teléfono y no se comparten con nadie. Puedes retirar el permiso en cualquier momento en los ajustes de Health Connect.",
                    de = "Lunaison hat keinen Internetzugriff: Die gelesenen Daten bleiben auf diesem Telefon und werden mit niemandem geteilt. Du kannst die Berechtigung jederzeit in den Health-Connect-Einstellungen entziehen.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { openLink(context, Links.PRIVACY) }) {
                Text(tr("Lire la politique de confidentialité", "Read the privacy policy", pl = "Przeczytaj politykę prywatności", uk = "Прочитати політику конфіденційності", es = "Leer la política de privacidad", de = "Datenschutzerklärung lesen"))
            }
            TextButton(onClick = onClose) {
                Text(tr("Fermer", "Close", pl = "Zamknij", uk = "Закрити", es = "Cerrar", de = "Schließen"))
            }
        }
    }
}
