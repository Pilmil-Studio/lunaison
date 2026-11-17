package fr.lunaison.ui.backup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.lunaison.core.data.BackupPassphraseStore
import fr.lunaison.core.data.LunaisonRepositoryProvider
import fr.lunaison.core.data.PassphraseState
import fr.lunaison.core.model.now
import fr.pilmil.core.platform.backup.AndroidBackupVault
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "Lunaison"

/**
 * Réglages et état de la sauvegarde automatique, dans un fichier de préférences privé qui n'est inclus dans aucune
 * sauvegarde du système (voir data_extraction_rules.xml). La phrase secrète n'est PAS ici : voir [BackupPassphraseStore].
 */
class AutoBackupConfig(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var folderUri: String?
        get() = prefs.getString(FOLDER_URI, null)
        set(value) = put { putString(FOLDER_URI, value) }

    var folderName: String?
        get() = prefs.getString(FOLDER_NAME, null)
        set(value) = put { putString(FOLDER_NAME, value) }

    /** Millisecondes depuis 1970 de la dernière sauvegarde réussie, 0 s'il n'y en a pas. */
    var lastSuccessMillis: Long
        get() = prefs.getLong(LAST_SUCCESS_MILLIS, 0L)
        set(value) = put { putLong(LAST_SUCCESS_MILLIS, value) }

    var lastSuccessDate: LocalDate?
        get() = prefs.getString(LAST_SUCCESS_DATE, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = put { putString(LAST_SUCCESS_DATE, value?.toString()) }

    /** Empreinte des données lors de la dernière sauvegarde réussie. */
    var lastFingerprint: String?
        get() = prefs.getString(LAST_FINGERPRINT, null)
        set(value) = put { putString(LAST_FINGERPRINT, value) }

    var problem: BackupProblem?
        get() = prefs.getString(PROBLEM, null)?.let { name -> BackupProblem.entries.firstOrNull { it.name == name } }
        set(value) = put { putString(PROBLEM, value?.name) }

    /** « Plus tard » sur la carte de l'accueil : masquée jusqu'à cette date. */
    var cardHiddenUntil: LocalDate?
        get() = prefs.getString(CARD_HIDDEN_UNTIL, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = put { putString(CARD_HIDDEN_UNTIL, value?.toString()) }

    /** Oublie le dossier et l'état de la sauvegarde (désactivation). La date de masquage de la carte est conservée. */
    fun clearBackupState() = put {
        remove(FOLDER_URI)
        remove(FOLDER_NAME)
        remove(LAST_SUCCESS_MILLIS)
        remove(LAST_SUCCESS_DATE)
        remove(LAST_FINGERPRINT)
        remove(PROBLEM)
    }

    @SuppressLint("ApplySharedPref")
    private fun put(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).commit()
    }

    private companion object {
        const val PREFS_NAME = "lunaison_sauvegarde_auto"
        const val FOLDER_URI = "dossier"
        const val FOLDER_NAME = "dossier_nom"
        const val LAST_SUCCESS_MILLIS = "derniere_reussite"
        const val LAST_SUCCESS_DATE = "derniere_reussite_jour"
        const val LAST_FINGERPRINT = "empreinte"
        const val PROBLEM = "probleme"
        const val CARD_HIDDEN_UNTIL = "carte_masquee_jusqu_au"
    }
}

/** Résultat d'une exécution de la sauvegarde, pour l'afficher à la personne. */
sealed interface RunResult {
    data object Saved : RunResult

    data class Skipped(val reason: SkipReason) : RunResult

    /** [problem] est affiché dans les réglages et sur l'accueil. */
    data class Failed(val problem: BackupProblem) : RunResult
}

/** Sauvegarde automatique : configuration, exécution, état. Tout le travail se fait hors du fil principal. */
object AutoBackup {
    private val mutex = Mutex()

    private fun privateCopy(context: Context) = PrivateCopyFile(File(context.applicationContext.filesDir, PrivateCopyFile.DIRECTORY))

    private fun safFolder(context: Context, uri: String) = SafFolder(context.applicationContext, Uri.parse(uri))

    /** État affiché : lit aussi le système pour savoir si le dossier est toujours accessible. */
    suspend fun status(context: Context): AutoBackupStatus = withContext(Dispatchers.IO) {
        // Hors d'un vrai téléphone (captures d'écran), le système peut manquer : l'état est alors simplement indisponible.
        runCatching { readStatus(context) }.getOrDefault(AutoBackupStatus.Unavailable)
    }

    private fun readStatus(context: Context): AutoBackupStatus {
        val config = AutoBackupConfig(context)
        val state = BackupPassphraseStore(context).state()
        if (state == PassphraseState.ABSENT) return AutoBackupStatus.NotConfigured(config.cardHiddenUntil)
        val uri = config.folderUri
        val problem = when {
            state == PassphraseState.UNREADABLE -> BackupProblem.PHRASE_UNREADABLE
            uri != null && !safFolder(context, uri).isUsable() -> BackupProblem.FOLDER_UNAVAILABLE
            else -> config.problem
        }
        return AutoBackupStatus.Configured(
            folderName = config.folderName,
            lastSuccessMillis = config.lastSuccessMillis.takeIf { it > 0 },
            problem = problem,
        )
    }

    /** La phrase secrète est définie : la sauvegarde automatique est active. */
    fun isConfigured(context: Context): Boolean =
        runCatching { BackupPassphraseStore(context).state() != PassphraseState.ABSENT }.getOrDefault(false)

    /**
     * Active la sauvegarde : garde l'accès au dossier, enveloppe la phrase par le Keystore, planifie la tâche
     * quotidienne et fait tout de suite une première sauvegarde.
     */
    suspend fun configure(context: Context, folder: Uri, phrase: CharArray): RunResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val config = AutoBackupConfig(app)
        val previousFolder = config.folderUri
        try {
            SafFolder.persist(app, folder)
        } catch (e: SecurityException) {
            return@withContext RunResult.Failed(BackupProblem.FOLDER_UNAVAILABLE)
        }
        val saf = SafFolder(app, folder)
        if (!saf.isUsable()) {
            SafFolder.release(app, folder)
            return@withContext RunResult.Failed(BackupProblem.FOLDER_UNAVAILABLE)
        }
        try {
            BackupPassphraseStore(app).save(phrase)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Phrase de la sauvegarde non enregistrée : ${e.javaClass.simpleName}")
            return@withContext RunResult.Failed(BackupProblem.PHRASE_UNREADABLE)
        }
        if (previousFolder != null && previousFolder != folder.toString()) SafFolder.release(app, Uri.parse(previousFolder))
        config.folderUri = folder.toString()
        config.folderName = saf.displayName()
        config.lastSuccessMillis = 0L
        config.lastSuccessDate = null
        config.lastFingerprint = null
        config.problem = null
        AutoBackupScheduler.schedule(app)
        execute(app, force = true)
    }

    /** Nouvelle phrase : les prochains fichiers l'emploient ; les précédents restent protégés par l'ancienne. */
    suspend fun changePassphrase(context: Context, phrase: CharArray): RunResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        try {
            BackupPassphraseStore(app).save(phrase)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Phrase de la sauvegarde non enregistrée : ${e.javaClass.simpleName}")
            return@withContext RunResult.Failed(BackupProblem.PHRASE_UNREADABLE)
        }
        AutoBackupConfig(app).problem = null
        AutoBackupScheduler.schedule(app)
        execute(app, force = true)
    }

    /** Nouveau dossier (par exemple quand l'ancien n'est plus accessible), puis sauvegarde immédiate. */
    suspend fun changeFolder(context: Context, folder: Uri): RunResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val config = AutoBackupConfig(app)
        val previous = config.folderUri
        try {
            SafFolder.persist(app, folder)
        } catch (e: SecurityException) {
            return@withContext RunResult.Failed(BackupProblem.FOLDER_UNAVAILABLE)
        }
        val saf = SafFolder(app, folder)
        if (!saf.isUsable()) {
            SafFolder.release(app, folder)
            return@withContext RunResult.Failed(BackupProblem.FOLDER_UNAVAILABLE)
        }
        if (previous != null && previous != folder.toString()) SafFolder.release(app, Uri.parse(previous))
        config.folderUri = folder.toString()
        config.folderName = saf.displayName()
        config.problem = null
        execute(app, force = true)
    }

    /**
     * Désactive : la phrase et sa clé d'enveloppe sont oubliées, la copie interne supprimée, la tâche annulée.
     * Les fichiers déjà écrits dans le dossier de la personne restent à sa disposition.
     */
    suspend fun disable(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val config = AutoBackupConfig(app)
        AutoBackupScheduler.cancel(app)
        config.folderUri?.let { SafFolder.release(app, Uri.parse(it)) }
        runCatching { BackupPassphraseStore(app).clear() }
        config.clearBackupState()
        runCatching { privateCopy(app).delete() }
        // Sans phrase, la copie dans le compte Google n'a plus rien à emporter : le réglage est remis à « désactivé ».
        app.noBackupFilesDir?.let { CloudBackupConsent(it).enabled = false }
        Unit
    }

    /** « Tout effacer » : la copie interne ne doit pas garder ce que la personne vient d'effacer. */
    fun onAllDataErased(context: Context) {
        runCatching { privateCopy(context.applicationContext).delete() }
    }

    /**
     * Écrit une sauvegarde si la règle le décide ([force] : « Sauvegarder maintenant », fin de la configuration).
     * Une seule exécution à la fois ; le résultat est mémorisé pour les réglages et l'accueil.
     */
    suspend fun execute(context: Context, force: Boolean): RunResult = withContext(Dispatchers.Default) {
        mutex.withLock { runLocked(context.applicationContext, force) }
    }

    private suspend fun runLocked(app: Context, force: Boolean): RunResult {
        val config = AutoBackupConfig(app)
        val store = BackupPassphraseStore(app)
        when (runCatching { store.state() }.getOrDefault(PassphraseState.UNREADABLE)) {
            PassphraseState.ABSENT -> return RunResult.Skipped(SkipReason.NOT_CONFIGURED)
            PassphraseState.UNREADABLE -> {
                config.problem = BackupProblem.PHRASE_UNREADABLE
                return RunResult.Failed(BackupProblem.PHRASE_UNREADABLE)
            }
            PassphraseState.READABLE -> Unit
        }
        val repository = (app as? LunaisonRepositoryProvider)?.repository
            ?: return RunResult.Failed(BackupProblem.WRITE_FAILED)
        val payload = repository.exportBackupJson()
        val today = LocalDate.now()
        val fingerprint = BackupPlanner.fingerprint(payload)
        val decision = BackupPlanner.decide(
            configured = true,
            hasData = repository.logsOnce().isNotEmpty(),
            today = today,
            lastSuccessDate = config.lastSuccessDate,
            lastFingerprint = config.lastFingerprint,
            fingerprint = fingerprint,
            force = force || config.problem != null,
        )
        if (decision is BackupDecision.Skip) return RunResult.Skipped(decision.reason)

        val phrase = store.read() ?: run {
            config.problem = BackupProblem.PHRASE_UNREADABLE
            return RunResult.Failed(BackupProblem.PHRASE_UNREADABLE)
        }
        val report = try {
            val vault = AndroidBackupVault()
            val saf = config.folderUri?.let { safFolder(app, it) }
            BackupWriter(
                seal = { text, secret -> vault.encrypt(text, secret) },
                open = { bytes, secret -> vault.decrypt(bytes, secret) },
            ).write(payload, phrase, today, saf, privateCopy(app))
        } finally {
            phrase.fill('\u0000')
        }
        config.problem = report.problem
        val mainTargetDone = report.savedInFolder || (config.folderUri == null && report.privateCopyWritten)
        if (mainTargetDone) {
            config.lastSuccessMillis = System.currentTimeMillis()
            config.lastSuccessDate = today
            config.lastFingerprint = fingerprint
        }
        // Seul le type de problème est journalisé : ni phrase, ni chemin, ni contenu.
        report.problem?.let { Log.w(LOG_TAG, "Sauvegarde automatique : ${it.name}") }
        return report.problem?.let { RunResult.Failed(it) } ?: RunResult.Saved
    }
}

/** Tâche WorkManager quotidienne, sans contrainte de réseau : rien ne sort du téléphone. */
object AutoBackupScheduler {
    private const val WORK_NAME = "sauvegarde-automatique"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    /** Au démarrage de l'appli : la tâche existe si une phrase est définie (elle survit normalement au redémarrage). */
    fun ensureScheduled(context: Context) {
        if (AutoBackup.isConfigured(context)) schedule(context)
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Un échec est mémorisé et signalé dans les réglages et sur l'accueil ; réessayer tout de suite n'y changerait rien.
        AutoBackup.execute(applicationContext, force = false)
        return Result.success()
    }
}
