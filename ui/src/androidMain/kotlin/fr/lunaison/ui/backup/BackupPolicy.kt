package fr.lunaison.ui.backup

import java.io.File

/**
 * Qui a le droit de copier les données de Lunaison, et quand. Ce que le système copie n'est jamais la base ni
 * les clés : seulement le dernier fichier `.pilmil` chiffré par la phrase secrète (voir data_extraction_rules.xml).
 *
 * - Le transfert direct d'un téléphone à l'autre est toujours permis (Android 12 et plus).
 * - La copie dans le compte Google n'est permise que si la personne l'a activée, qu'une phrase secrète existe, et
 *   seulement quand Android la chiffre de bout en bout avec le verrouillage d'écran.
 * - Sous Android 12, rien ne permet de vérifier ce chiffrement ni de distinguer le transfert direct :
 *   Lunaison ne laisse alors rien partir.
 */
object BackupPolicy {

    /** Valeurs de BackupAgent.FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED et FLAG_DEVICE_TO_DEVICE_TRANSFER. */
    const val FLAG_CLIENT_SIDE_ENCRYPTION = 1
    const val FLAG_DEVICE_TO_DEVICE = 2

    /** Première version d'Android qui indique le type de sauvegarde à l'application (API 31). */
    const val MIN_SDK_WITH_FLAGS = 31

    fun allows(sdk: Int, transportFlags: Int, cloudBackupEnabled: Boolean, passphraseSet: Boolean): Boolean {
        if (sdk < MIN_SDK_WITH_FLAGS) return false
        if ((transportFlags and FLAG_DEVICE_TO_DEVICE) != 0) return true
        return cloudBackupEnabled && passphraseSet && (transportFlags and FLAG_CLIENT_SIDE_ENCRYPTION) != 0
    }

    /** Le réglage n'est proposé que là où la règle ci-dessus peut s'appliquer. */
    fun settingAvailable(sdk: Int): Boolean = sdk >= MIN_SDK_WITH_FLAGS

    /** Ce qui empêche d'activer la copie dans le compte Google, ou null si elle peut l'être. */
    fun blocker(sdk: Int, deviceSecure: Boolean, passphraseSet: Boolean): CloudCopyBlocker? = when {
        !settingAvailable(sdk) -> CloudCopyBlocker.UNSUPPORTED_ANDROID
        !passphraseSet -> CloudCopyBlocker.NO_PASSPHRASE
        !deviceSecure -> CloudCopyBlocker.NO_SCREEN_LOCK
        else -> null
    }
}

/** Raison pour laquelle le réglage « copier dans mon compte Google » n'est pas activable. */
enum class CloudCopyBlocker { UNSUPPORTED_ANDROID, NO_PASSPHRASE, NO_SCREEN_LOCK }

/**
 * Choix de la personne : sauvegarder ou non dans son compte Google. Désactivé tant qu'elle ne l'a pas décidé.
 * Le choix est un simple fichier dans le dossier exclu des sauvegardes : il n'est jamais restauré
 * sur un autre téléphone, où la personne décide de nouveau.
 */
class CloudBackupConsent(private val noBackupDir: File) {

    private val marker: File
        get() = File(noBackupDir, MARKER_NAME)

    var enabled: Boolean
        get() = marker.exists()
        set(value) {
            if (value) {
                noBackupDir.mkdirs()
                marker.createNewFile()
            } else {
                marker.delete()
            }
        }

    private companion object {
        const val MARKER_NAME = "cloud_backup_enabled"
    }
}
