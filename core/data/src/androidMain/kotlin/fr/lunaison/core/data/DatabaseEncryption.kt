package fr.lunaison.core.data

import java.io.File
import java.io.RandomAccessFile

/*
 * Chiffrement de la base au repos et conversion des bases existantes en clair.
 *
 * Fichiers, dans le dossier des bases de l'appli :
 * - `lunaison.db` : la base utilisée par Room (en clair avant la conversion, chiffrée après) ;
 * - `lunaison.db-chiffrement` : copie chiffrée en cours de fabrication, jamais utilisée telle quelle ;
 * - `lunaison.db-ancienne` : base en clair mise de côté pendant l'échange, effacée ensuite.
 *
 * Chaque étape laisse les fichiers dans un état que [planNextStep] sait reprendre : si l'appli est
 * tuée au milieu, le lancement suivant repart de l'état observé, sans jamais perdre la base d'origine.
 */

/** Nature d'un fichier de base, d'après ses 16 premiers octets. */
internal enum class DatabaseFileKind { ABSENT, EMPTY, PLAINTEXT, ENCRYPTED }

private val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()

/**
 * Une base SQLite en clair commence toujours par « SQLite format 3 » suivi d'un octet nul.
 * Une base SQLCipher commence par un sel aléatoire. Tout autre contenu est traité comme chiffré :
 * il n'est alors jamais ouvert sans clé, ni effacé.
 */
internal fun classifyDatabaseHeader(length: Long, header: ByteArray): DatabaseFileKind = when {
    length == 0L -> DatabaseFileKind.EMPTY
    header.size >= SQLITE_HEADER.size && header.copyOf(SQLITE_HEADER.size).contentEquals(SQLITE_HEADER) ->
        DatabaseFileKind.PLAINTEXT
    else -> DatabaseFileKind.ENCRYPTED
}

/** Ce que l'on observe sur le disque avant chaque étape. */
internal data class MigrationState(
    val main: DatabaseFileKind,
    val pendingExists: Boolean,
    val previousExists: Boolean,
)

internal enum class MigrationStep {
    /** L'échange a été interrompu après la mise de côté de la base en clair : on la remet en place. */
    RESTORE_PREVIOUS,

    /** Copie chiffrée laissée par une tentative interrompue : jamais fiable, on la jette. */
    DISCARD_PENDING,

    /** Base en clair encore présente à côté de la base chiffrée : on vérifie la base chiffrée, puis on l'efface. */
    WIPE_PREVIOUS,

    /**
     * État anormal : une base en clair active et une autre mise de côté. Aucune des deux n'est effacée :
     * l'ancienne est renommée pour ne pas être écrasée, puis la base active est convertie.
     */
    SET_ASIDE_PREVIOUS,

    /** Base en clair : on la convertit. */
    ENCRYPT,

    /** Plus rien à faire : Room ouvre la base chiffrée (ou en crée une). */
    OPEN,
}

/** Machine d'états de la conversion : fonction pure, testée sur chaque état intermédiaire. */
internal fun planNextStep(state: MigrationState): MigrationStep = when {
    (state.main == DatabaseFileKind.ABSENT || state.main == DatabaseFileKind.EMPTY) && state.previousExists ->
        MigrationStep.RESTORE_PREVIOUS
    state.pendingExists -> MigrationStep.DISCARD_PENDING
    state.previousExists && state.main == DatabaseFileKind.ENCRYPTED -> MigrationStep.WIPE_PREVIOUS
    state.previousExists -> MigrationStep.SET_ASIDE_PREVIOUS
    state.main == DatabaseFileKind.PLAINTEXT -> MigrationStep.ENCRYPT
    else -> MigrationStep.OPEN
}

/** Version du schéma et nombre de lignes de chaque table : de quoi vérifier qu'une copie est complète. */
internal data class DatabaseSnapshot(val userVersion: Int, val rowCounts: Map<String, Long>)

/** Accès au moteur SQLCipher, derrière une interface pour tester l'enchaînement sans moteur natif. */
internal interface EncryptionEngine {
    /** Ouvre la base en clair, la fait sortir du mode WAL (tout est alors dans le fichier principal) et la décrit. */
    fun inspectPlaintext(file: File): DatabaseSnapshot

    /** Copie toute la base en clair [source] dans une nouvelle base chiffrée [target], version du schéma comprise. */
    fun exportEncrypted(source: File, target: File, passphrase: ByteArray, userVersion: Int)

    /** Ouvre une base chiffrée avec [passphrase], vérifie son intégrité et la décrit. */
    fun inspectEncrypted(file: File, passphrase: ByteArray): DatabaseSnapshot
}

/** Résultat de la préparation : comment Room doit ouvrir la base. */
internal sealed interface PreparedDatabase {
    /** Base chiffrée avec cette clé de 256 bits. */
    class Encrypted(val key: ByteArray) : PreparedDatabase

    /** La conversion a échoué : la base d'origine, intacte et en clair, reste utilisée jusqu'au prochain essai. */
    data object Plaintext : PreparedDatabase
}

private val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")

/** Opérations sur les fichiers de la base. Chaque déplacement est un renommage atomique dans le même dossier. */
internal class DatabaseFiles(val main: File) {
    val pending = File(main.path + "-chiffrement")
    val previous = File(main.path + "-ancienne")

    /** Posé quand une base chiffrée arrive sans sa clé : l'appli propose alors de restaurer un fichier `.pilmil`. */
    val keyLostMarker = File(main.path + "-cle-perdue")

    fun observe(): MigrationState {
        var kind = kindOf(main)
        // Fichier principal vide mais journal WAL rempli : les données sont dans le journal, en clair.
        if (kind == DatabaseFileKind.EMPTY && File(main.path + "-wal").length() > 0) kind = DatabaseFileKind.PLAINTEXT
        return MigrationState(kind, pending.exists(), previous.exists())
    }

    fun kindOf(file: File): DatabaseFileKind {
        if (!file.exists()) return DatabaseFileKind.ABSENT
        val header = ByteArray(SQLITE_HEADER.size)
        val read = file.inputStream().use { input -> input.read(header) }
        return classifyDatabaseHeader(file.length(), if (read <= 0) ByteArray(0) else header.copyOf(read))
    }

    fun restorePrevious() {
        deleteWithSidecars(main)
        move(previous, main)
    }

    fun deletePending() = deleteWithSidecars(pending)

    /** Journaux du fichier principal : à supprimer avant d'y placer un autre contenu. */
    fun mainSidecarsHoldData(): Boolean = sidecars(main).any { it.name.endsWith("-wal") && it.length() > 0 }

    fun deleteMainSidecars() = sidecars(main).forEach { delete(it) }

    /** Échange : base en clair mise de côté, puis copie chiffrée vérifiée mise à sa place. */
    fun swapInPending() {
        move(main, previous)
        move(pending, main)
    }

    /** Écrase la base en clair mise de côté avec des zéros, puis la supprime. */
    fun wipePrevious() {
        overwriteWithZeros(previous)
        deleteWithSidecars(previous)
    }

    /** Base chiffrée qu'aucune clé ne permet plus d'ouvrir : mise de côté, jamais effacée. */
    fun quarantineMain(): File = setAside(main, "illisible")

    fun markKeyLost() {
        keyLostMarker.writeText("1")
    }

    /** Ancienne base en clair d'origine inconnue : renommée, jamais effacée. */
    fun setAsidePrevious(): File = setAside(previous, "conservee")

    private fun setAside(file: File, label: String): File {
        val target = File(file.parentFile, "${file.name}-$label-${System.currentTimeMillis()}")
        move(file, target)
        sidecars(file).forEach { sidecar ->
            if (sidecar.exists()) move(sidecar, File(target.path + sidecar.name.removePrefix(file.name)))
        }
        return target
    }

    private fun sidecars(file: File) = SIDECAR_SUFFIXES.map { File(file.path + it) }

    private fun deleteWithSidecars(file: File) {
        delete(file)
        sidecars(file).forEach { delete(it) }
    }

    private fun delete(file: File) {
        check(!file.exists() || file.delete()) { "Suppression impossible : ${file.name}" }
    }

    private fun move(from: File, to: File) {
        check(from.renameTo(to)) { "Renommage impossible : ${from.name} vers ${to.name}" }
    }

    private fun overwriteWithZeros(file: File) {
        if (!file.exists()) return
        RandomAccessFile(file, "rw").use { raf ->
            val zeros = ByteArray(BLOCK)
            var remaining = raf.length()
            raf.seek(0)
            while (remaining > 0) {
                val chunk = minOf(remaining, BLOCK.toLong()).toInt()
                raf.write(zeros, 0, chunk)
                remaining -= chunk
            }
            raf.fd.sync()
        }
    }

    private companion object {
        const val BLOCK = 64 * 1024
    }
}

/**
 * Prépare la base avant que Room ne l'ouvre : conversion si elle est en clair, puis clé à utiliser.
 * Garanties :
 * - la base d'origine n'est mise de côté qu'une fois la copie chiffrée vérifiée (version et lignes) ;
 * - en cas d'échec, l'appli continue avec la base d'origine en clair, jamais avec une base vide ;
 * - aucun message ne contient la clé.
 */
internal class DatabaseEncryptor(
    private val files: DatabaseFiles,
    private val keys: DatabaseKeyManager,
    private val engine: EncryptionEngine,
    private val log: (String) -> Unit = {},
) {
    fun prepare(): PreparedDatabase {
        repeat(MAX_STEPS) {
            val state = files.observe()
            when (planNextStep(state)) {
                MigrationStep.RESTORE_PREVIOUS -> {
                    log("Conversion interrompue : base d'origine remise en place")
                    files.restorePrevious()
                }
                MigrationStep.DISCARD_PENDING -> files.deletePending()
                MigrationStep.WIPE_PREVIOUS -> checkThenWipePrevious()
                MigrationStep.SET_ASIDE_PREVIOUS -> {
                    val kept = files.setAsidePrevious()
                    log("Ancienne base en clair conservée sous ${kept.name}")
                }
                MigrationStep.ENCRYPT -> if (!encrypt()) return fallBackToPlaintext()
                MigrationStep.OPEN -> return open(state.main)
            }
        }
        error("Préparation de la base : état des fichiers inattendu")
    }

    private fun encrypt(): Boolean {
        var key: ByteArray? = null
        var passphrase: ByteArray? = null
        return try {
            key = keys.existing() ?: keys.create()
            passphrase = sqlCipherRawKey(key)
            val source = engine.inspectPlaintext(files.main)
            engine.exportEncrypted(files.main, files.pending, passphrase, source.userVersion)
            val copy = engine.inspectEncrypted(files.pending, passphrase)
            check(copy == source) { "La copie chiffrée ne correspond pas à la base d'origine" }
            check(!files.mainSidecarsHoldData()) { "Journal WAL non vidé" }
            files.deleteMainSidecars()
            files.swapInPending()
            log("Base convertie en base chiffrée")
            true
        } catch (e: Exception) {
            log("Conversion reportée au prochain lancement : ${e.javaClass.simpleName}")
            false
        } catch (e: LinkageError) {
            log("Conversion reportée au prochain lancement : ${e.javaClass.simpleName}")
            false
        } finally {
            key?.fill(0)
            passphrase?.fill(0)
        }
    }

    /** Remet la base d'origine en place si besoin, puis vérifie qu'elle est bien là avant de l'utiliser. */
    private fun fallBackToPlaintext(): PreparedDatabase {
        runCatching {
            val state = files.observe()
            if ((state.main == DatabaseFileKind.ABSENT || state.main == DatabaseFileKind.EMPTY) && state.previousExists) {
                files.restorePrevious()
            }
            files.deletePending()
        }
        check(files.observe().main == DatabaseFileKind.PLAINTEXT) { "Base d'origine introuvable après un échec de conversion" }
        return PreparedDatabase.Plaintext
    }

    /** La base en clair mise de côté n'est effacée que si la base active s'ouvre avec la clé. */
    private fun checkThenWipePrevious() {
        if (!canOpenMain()) {
            log("Base chiffrée inutilisable : retour à la base d'origine")
            files.quarantineMain()
            return
        }
        files.wipePrevious()
    }

    private fun canOpenMain(): Boolean {
        val key = runCatching { keys.existing() }.getOrNull() ?: return false
        val passphrase = sqlCipherRawKey(key)
        return try {
            runCatching { engine.inspectEncrypted(files.main, passphrase) }.isSuccess
        } finally {
            key.fill(0)
            passphrase.fill(0)
        }
    }

    private fun open(main: DatabaseFileKind): PreparedDatabase {
        if (main == DatabaseFileKind.ENCRYPTED) {
            // Une erreur passagère du Keystore remonte : mieux vaut ne pas démarrer que perdre la base.
            keys.existing()?.let { return PreparedDatabase.Encrypted(it) }
            val moved = files.quarantineMain()
            log("Clé de la base introuvable : base illisible mise de côté (${moved.name})")
            // Sans effet sur la suite si le repère ne s'écrit pas : l'appli démarre de toute façon sur une base neuve.
            runCatching { files.markKeyLost() }
            return PreparedDatabase.Encrypted(keys.create())
        }
        // Pas encore de base : des journaux orphelins n'ont rien à apporter à une base neuve.
        files.deleteMainSidecars()
        val key = runCatching { keys.existing() }.getOrNull() ?: keys.create()
        return PreparedDatabase.Encrypted(key)
    }

    private companion object {
        const val MAX_STEPS = 8
    }
}
