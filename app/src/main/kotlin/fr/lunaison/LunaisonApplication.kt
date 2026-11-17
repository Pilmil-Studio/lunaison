package fr.lunaison

import android.app.Application
import fr.lunaison.core.data.LunaisonRepository
import fr.lunaison.core.data.LunaisonRepositoryProvider
import fr.lunaison.reminders.ReminderScheduler
import fr.lunaison.ui.backup.AutoBackupScheduler
import fr.lunaison.widget.LunaisonWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import fr.lunaison.core.data.create

/** Assemblage des dépendances, sans framework d'injection : une seule source de données. */
class LunaisonApplication : Application(), LunaisonRepositoryProvider {

    override val repository: LunaisonRepository by lazy { LunaisonRepository.create(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // La sauvegarde automatique, si la personne l'a activée : la tâche quotidienne doit exister (hors du fil principal, le Keystore est lu).
        scope.launch { AutoBackupScheduler.ensureScheduled(this@LunaisonApplication) }
        // Un seul endroit replanifie les rappels : à chaque changement des journées notées ou des réglages.
        scope.launch {
            combine(repository.logs, repository.settings) { logs, settings -> logs to settings }
                .distinctUntilChanged()
                .collect {
                    ReminderScheduler.reschedule(this@LunaisonApplication, repository)
                    LunaisonWidget.updateAll(this@LunaisonApplication, repository)
                }
        }
    }
}
