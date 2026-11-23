package fr.lunaison

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import fr.lunaison.core.data.AppSettings
import fr.lunaison.core.model.tr
import fr.lunaison.reminders.ReminderWorker
import fr.lunaison.ui.LINK_LOG
import fr.lunaison.ui.LINK_PERIOD

/**
 * Raccourcis de l'icône (appui long) que les assistants du téléphone savent aussi ouvrir. Ils ne font qu'ouvrir
 * Lunaison sur le bon écran : le code est demandé comme d'habitude et rien n'est enregistré sans la personne.
 * En mode discret, ils sont retirés, car leurs libellés parlent du cycle.
 */
object AppShortcuts {

    fun update(context: Context, settings: AppSettings) {
        if (settings.discreetNotifications || !settings.onboarded) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }
        val shortcuts = listOf(
            shortcut(
                context,
                id = "noter",
                link = LINK_LOG,
                label = tr("Noter ma journée", "Log my day", pl = "Zanotuj mój dzień", uk = "Записати мій день", es = "Anotar mi día", de = "Meinen Tag eintragen"),
            ),
            shortcut(
                context,
                id = "regles",
                link = LINK_PERIOD,
                label = tr("Mes règles ont commencé", "My period has started", pl = "Zaczęła się miesiączka", uk = "Місячні почалися", es = "Me ha venido la regla", de = "Meine Periode hat begonnen"),
            ),
        )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun shortcut(context: Context, id: String, link: String, label: String): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ReminderWorker.EXTRA_OPEN, link)
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }
}
