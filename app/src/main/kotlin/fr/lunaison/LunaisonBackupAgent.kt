package fr.lunaison

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput
import android.os.Build
import fr.lunaison.ui.backup.AutoBackup
import fr.lunaison.ui.backup.BackupPolicy
import fr.lunaison.ui.backup.CloudBackupConsent

/**
 * Filtre les sauvegardes d'Android selon [BackupPolicy] : rien ne part vers le compte Google sans le choix de la
 * personne, sans phrase secrète, ni sans chiffrement de bout en bout. Ce qui part, sur le nuage comme dans le transfert
 * direct, est uniquement ce que data_extraction_rules.xml inclut : le dernier fichier `.pilmil` (déjà chiffré par la
 * phrase secrète) du dossier privé de l'appli. Ni la base, ni les clés, ni la phrase.
 */
class LunaisonBackupAgent : BackupAgentHelper() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) data.transportFlags else 0
        val consent = CloudBackupConsent(noBackupFilesDir)
        if (BackupPolicy.allows(Build.VERSION.SDK_INT, flags, consent.enabled, AutoBackup.isConfigured(this))) {
            super.onFullBackup(data)
        }
    }
}
