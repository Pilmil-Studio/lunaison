package fr.lunaison.ui

import androidx.compose.runtime.Composable
import fr.lunaison.core.cycle.HealthReport
import fr.lunaison.core.cycle.Prediction
import fr.lunaison.core.data.AppSettings
import fr.lunaison.core.data.BackupRestoreSummary
import fr.lunaison.core.data.Partner
import fr.lunaison.core.data.PinThrottleState
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.PartnerSnapshot
import fr.lunaison.ui.backup.AutoBackupStatus
import fr.lunaison.ui.backup.RestoreOffer
import kotlinx.datetime.LocalDate

/*
 * Frontière avec le système : tout ce que les écrans communs demandent à Android ou iOS passe ici.
 * Les implémentations vivent dans androidMain et iosMain.
 */

/** Contexte du système (Android : `Context`). Les écrans le transmettent sans l'inspecter. */
expect abstract class PlatformContext

@Composable
expect fun rememberPlatformContext(): PlatformContext

/** Ouvre un lien dans le navigateur de la personne. L'appli elle-même n'a pas accès à Internet. */
expect fun openLink(context: PlatformContext, url: String)

/** Ouvre la fiche de l'application sur le magasin d'applications. */
expect fun openStore(context: PlatformContext)

/** Faux tant que l'appli n'a pas de fiche sur le magasin de la plateforme : on masque alors les invites à la noter. */
expect val storePageAvailable: Boolean

/** Geste ou bouton « retour » du système. Sur iOS, la navigation passe par les boutons de l'appli. */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** Nom du système, pour les textes qui renvoient à ses réglages. */
expect val platformName: String

/** Message bref à l'écran (Android : Toast). */
expect fun showToast(context: PlatformContext, message: String)

/** Autorisation d'envoyer des notifications, gérée par le système. */
class NotificationAccess(
    /** Autorisé maintenant ; revérifié au retour dans l'appli. */
    val allowed: Boolean,
    /** Affiche la demande du système ; [rememberNotificationAccess] avertit ensuite via `onAnswered`. */
    val request: () -> Unit,
    /** Ouvre les réglages de l'appli dans le système. */
    val openSettings: () -> Unit,
)

@Composable
expect fun rememberNotificationAccess(onAnswered: (granted: Boolean) -> Unit = {}): NotificationAccess

/**
 * Enregistre un texte dans un fichier choisi par la personne (Android : sélecteur de document ; iOS : feuille de partage).
 * Le contenu n'est produit qu'une fois le fichier choisi.
 */
@Composable
expect fun rememberFileExporter(mimeType: String, content: suspend () -> String): (fileName: String) -> Unit

/** Sélecteur de contact du système (sans autorisation) ; `null` quand la plateforme n'en propose pas. */
@Composable
expect fun rememberContactPicker(onPicked: (name: String, phone: String) -> Unit): (() -> Unit)?

/** Prépare le message et ouvre la messagerie. Aucune autorisation : c'est la messagerie qui envoie. */
expect object ShareLauncher {
    fun send(context: PlatformContext, partner: Partner, text: String)
    fun shareAny(context: PlatformContext, text: String)
}

/** Le bilan de santé peut être exporté en PDF. */
expect val pdfReportAvailable: Boolean

/** Crée le PDF du bilan et ouvre le partage ; lève une exception en cas d'échec. */
expect suspend fun shareHealthReportPdf(
    context: PlatformContext,
    report: HealthReport,
    medicationLabel: String,
    logs: List<DayLog>,
    sections: Set<ReportSection>,
)

/**
 * Enregistre le PDF du bilan dans un fichier choisi par la personne (« Télécharger »).
 * `null` quand le partage du système propose déjà l'enregistrement (iOS : « Enregistrer dans Fichiers »).
 * [onDone] n'est appelé que si un emplacement a été choisi.
 */
@Composable
expect fun rememberHealthReportPdfSaver(
    onDone: (success: Boolean) -> Unit,
): ((report: HealthReport, medicationLabel: String, logs: List<DayLog>, sections: Set<ReportSection>) -> Unit)?

/** L'import depuis une autre appli de suivi est proposé (Android seulement). */
expect val importAvailable: Boolean

@Composable
expect fun ImportDialogs(showSource: Boolean, onDismiss: () -> Unit, actions: Actions, onResult: (String) -> Unit)

/** La sauvegarde chiffrée .pilmil et sa restauration sont disponibles sur la plateforme (Android seulement). */
expect val encryptedBackupAvailable: Boolean

/** Chiffre le document de sauvegarde avec un mot de passe et l'enregistre dans un fichier .pilmil choisi par la personne. */
@Composable
expect fun EncryptedBackupButton(
    label: String,
    fileName: String,
    onProducePayload: suspend () -> String,
    onResult: (String) -> Unit,
)

/**
 * Choisit un fichier .pilmil, demande le mot de passe et une confirmation explicite, puis appelle [onRestore]
 * avec le document déchiffré. Une erreur (mot de passe, fichier corrompu, version inconnue) laisse les données intactes.
 */
@Composable
expect fun RestoreBackupButton(
    label: String,
    onRestore: suspend (json: String) -> BackupRestoreSummary,
    onResult: (String) -> Unit,
    /** Fichier .pilmil déjà sélectionné (retrouvé sur le téléphone) : le bouton demande directement la phrase. */
    preselected: ByteArray? = null,
    /** Faux sur une base vide : rien n'est remplacé, la confirmation « remplacer les données » n'a pas lieu d'être. */
    replacesData: Boolean = true,
    /** Bouton principal (accueil d'arrivée) plutôt que bouton secondaire. */
    primary: Boolean = false,
)

/** La sauvegarde automatique vers un dossier choisi est disponible (Android seulement). */
expect val autoBackupAvailable: Boolean

/** Section « Sauvegarde automatique » des réglages : dossier, phrase secrète, état. Ne montre rien sur iOS. */
@Composable
expect fun AutoBackupSettings()

/** État de la sauvegarde automatique ; lit le système, à appeler hors du fil principal. */
expect suspend fun readAutoBackupStatus(context: PlatformContext): AutoBackupStatus

/** « Plus tard » sur la carte de rappel de l'accueil : masquée jusqu'à [date]. */
expect fun hideBackupCardUntil(context: PlatformContext, date: LocalDate)

/** Après « Tout effacer » : la copie interne de la sauvegarde ne doit pas garder ce qui vient d'être effacé. */
expect fun onAllDataErased(context: PlatformContext)

/**
 * Au premier lancement sur une base vide : y a-t-il quelque chose à restaurer ? Une copie chiffrée retrouvée sur le
 * téléphone, ou une base chiffrée venue d'un autre téléphone (mise de côté). Null s'il n'y a rien.
 */
expect suspend fun readRestoreOffer(context: PlatformContext): RestoreOffer?

/** La personne a restauré ou commencé à zéro : le message sur la base mise de côté disparaît. */
expect fun acknowledgeRestoreOffer(context: PlatformContext)

/** La sauvegarde iCloud de l'iPhone inclut les données ; la personne peut l'exclure (iOS seulement). */
expect val cloudBackupControllable: Boolean

expect fun isCloudBackupEnabled(): Boolean

expect fun setCloudBackupEnabled(enabled: Boolean)

/** Matrice de modules d'un QR Code (vrai = module sombre). */
class QrMatrix(val width: Int, val height: Int, private val modules: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * width + x]
}

/** Encode [content] en QR Code ; `null` si c'est impossible ou si la plateforme ne sait pas encore le faire. */
expect fun encodeQr(content: String): QrMatrix?

/** Lecture d'un code partenaire par la caméra, fournie par la plateforme. */
class ScannerState(
    val hasCamera: Boolean,
    val preview: @Composable () -> Unit,
)

@Composable
expect fun rememberScanner(onSnapshot: (PartnerSnapshot) -> Unit): ScannerState

/** Synchronise les rappels locaux sur la plateforme (iOS : `UNCalendarNotificationTrigger`). */
expect fun syncReminders(settings: AppSettings, prediction: Prediction?)

/** Persistance locale de l'état anti-bruteforce du code PIN. */
interface PinThrottleStore {
    fun load(): PinThrottleState
    fun save(state: PinThrottleState)
}

@Composable
expect fun rememberPinThrottleStore(): PinThrottleStore

/** Horloge monotone en millisecondes (Android : `SystemClock.elapsedRealtime()`). */
expect fun currentElapsedRealtimeMs(): Long
