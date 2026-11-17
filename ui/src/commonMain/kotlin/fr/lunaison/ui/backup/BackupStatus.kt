package fr.lunaison.ui.backup

import fr.lunaison.core.model.isBefore
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate

/** Ce qui a empêché la dernière sauvegarde automatique, sans jamais échouer en silence. */
enum class BackupProblem {
    /** Dossier supprimé, stockage retiré ou permission révoquée. */
    FOLDER_UNAVAILABLE,

    /** La phrase secrète ne se relit plus : il faut la définir de nouveau. */
    PHRASE_UNREADABLE,

    /** Écriture impossible (stockage plein, fournisseur de fichiers en erreur). */
    WRITE_FAILED,

    /** Le fichier écrit ne se relit pas : il n'a pas remplacé les précédents. */
    VERIFY_FAILED,

    /** La copie privée (celle que la copie dans le compte Google emporte) n'a pas pu être écrite. */
    PRIVATE_COPY_FAILED,
}

/** État de la sauvegarde automatique, tel que les réglages et l'accueil l'affichent. */
sealed interface AutoBackupStatus {
    /** Plateforme sans sauvegarde automatique (iOS) : rien n'est affiché. */
    data object Unavailable : AutoBackupStatus

    /** Aucune sauvegarde configurée ; [cardHiddenUntil] : la carte de rappel est masquée jusqu'à cette date. */
    data class NotConfigured(val cardHiddenUntil: LocalDate? = null) : AutoBackupStatus

    data class Configured(
        val folderName: String?,
        /** Date et heure de la dernière sauvegarde réussie (millisecondes depuis 1970), ou null s'il n'y en a pas encore. */
        val lastSuccessMillis: Long?,
        val problem: BackupProblem? = null,
    ) : AutoBackupStatus
}

/** Ce que l'accueil affiche au sujet de la sauvegarde. Logique pure, testée sans téléphone. */
object BackupReminder {
    /** Nombre de journées saisies à partir duquel le rappel est proposé. */
    const val MIN_LOGGED_DAYS = 14

    /** « Plus tard » masque la carte pendant ce nombre de jours. */
    const val HIDDEN_DAYS = 30

    fun shouldShowReminder(status: AutoBackupStatus, loggedDays: Int, today: LocalDate): Boolean {
        if (status !is AutoBackupStatus.NotConfigured) return false
        if (loggedDays < MIN_LOGGED_DAYS) return false
        val hiddenUntil = status.cardHiddenUntil
        return hiddenUntil == null || !today.isBefore(hiddenUntil)
    }

    fun hiddenUntilAfterLater(today: LocalDate): LocalDate = today.plusDays(HIDDEN_DAYS)

    /** Problème à signaler sur l'accueil : toujours, dès qu'une sauvegarde configurée n'a pas pu se faire. */
    fun problemToShow(status: AutoBackupStatus): BackupProblem? = (status as? AutoBackupStatus.Configured)?.problem
}

/** Pourquoi l'écran de restauration est proposé au premier lancement. */
enum class RestoreReason {
    /** Une copie chiffrée a été retrouvée dans le dossier privé de l'appli (restauration Android). */
    COPY_FOUND,

    /** Une base chiffrée est présente sans sa clé (restaurée d'un autre téléphone) : elle a été mise de côté. */
    DATABASE_UNREADABLE,
}

/** Proposition de restauration : [file] est le fichier `.pilmil` déjà sélectionné, s'il y en a un. */
class RestoreOffer(val reason: RestoreReason, val file: ByteArray? = null)
