package fr.lunaison.ui.backup

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.datetime.LocalDate

/** Le dossier choisi n'est plus accessible (permission révoquée, stockage retiré, dossier supprimé). */
class FolderUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Fichier présent dans le dossier de sauvegarde ; [handle] désigne le fichier pour le lire ou le supprimer. */
class FolderEntry(val handle: String, val name: String)

/** Dossier de sauvegarde, derrière une interface pour tester la rotation sans le système de fichiers d'Android. */
interface BackupFolder {
    /** @throws FolderUnavailableException si le dossier n'est plus accessible. */
    fun entries(): List<FolderEntry>

    /** Crée le fichier [name] et y écrit [bytes] ; rend son identifiant. */
    fun write(name: String, bytes: ByteArray): String

    fun read(handle: String): ByteArray

    fun delete(handle: String)
}

/** Dernière sauvegarde chiffrée, gardée dans un dossier privé de l'appli (voir data_extraction_rules.xml). */
interface PrivateCopy {
    fun write(bytes: ByteArray)

    fun read(): ByteArray?

    fun delete()
}

/** Copie privée : un seul fichier, remplacé par un renommage atomique. */
class PrivateCopyFile(private val directory: File) : PrivateCopy {
    private val target get() = File(directory, FILE_NAME)

    override fun write(bytes: ByteArray) {
        directory.mkdirs()
        val temporary = File(directory, "$FILE_NAME.tmp")
        java.io.FileOutputStream(temporary).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun read(): ByteArray? = target.takeIf { it.isFile }?.readBytes()

    override fun delete() {
        directory.deleteRecursively()
    }

    companion object {
        /** Dossier (dans `filesDir`) repris dans data_extraction_rules.xml. */
        const val DIRECTORY = "sauvegarde-privee"
        const val FILE_NAME = "derniere.pilmil"
    }
}

/** Ce qu'une écriture a donné. [problem] est null quand tout s'est bien passé. */
class WriteReport(val savedInFolder: Boolean, val privateCopyWritten: Boolean, val problem: BackupProblem?)

/**
 * Écrit une sauvegarde chiffrée, la relit pour la vérifier, puis seulement supprime les anciennes.
 * [seal] et [open] chiffrent et déchiffrent avec la phrase secrète (le coffre `.pilmil` de pilmil-core).
 */
class BackupWriter(
    private val seal: (payload: String, phrase: CharArray) -> ByteArray,
    private val open: (bytes: ByteArray, phrase: CharArray) -> String,
) {
    fun write(
        payload: String,
        phrase: CharArray,
        today: LocalDate,
        folder: BackupFolder?,
        privateCopy: PrivateCopy?,
    ): WriteReport {
        val sealed = try {
            seal(payload, phrase)
        } catch (e: Exception) {
            return WriteReport(false, false, BackupProblem.WRITE_FAILED)
        }
        // Vérification en mémoire avant toute écriture : le fichier doit se rouvrir avec la phrase.
        if (!opensTo(sealed, phrase, payload)) return WriteReport(false, false, BackupProblem.VERIFY_FAILED)

        val privateOk = privateCopy == null || writePrivate(privateCopy, sealed)
        if (folder == null) {
            return WriteReport(false, privateOk, if (privateOk) null else BackupProblem.PRIVATE_COPY_FAILED)
        }
        val folderProblem = writeToFolder(folder, sealed, phrase, payload, today)
        val problem = folderProblem ?: if (privateOk) null else BackupProblem.PRIVATE_COPY_FAILED
        return WriteReport(folderProblem == null, privateOk, problem)
    }

    private fun opensTo(bytes: ByteArray, phrase: CharArray, payload: String): Boolean =
        runCatching { open(bytes, phrase) == payload }.getOrDefault(false)

    private fun writePrivate(copy: PrivateCopy, sealed: ByteArray): Boolean = runCatching {
        copy.write(sealed)
        copy.read()?.contentEquals(sealed) == true
    }.getOrDefault(false)

    /** Rend le problème rencontré, ou null si le fichier est écrit, vérifié et les anciens sont supprimés. */
    private fun writeToFolder(folder: BackupFolder, sealed: ByteArray, phrase: CharArray, payload: String, today: LocalDate): BackupProblem? {
        val created: Pair<String, String> = try {
            val newName = BackupFileNames.nextName(today, folder.entries().map { it.name })
            newName to folder.write(newName, sealed)
        } catch (e: FolderUnavailableException) {
            return BackupProblem.FOLDER_UNAVAILABLE
        } catch (e: Exception) {
            return BackupProblem.WRITE_FAILED
        }
        val (name, handle) = created
        val verified = runCatching { opensTo(folder.read(handle), phrase, payload) }.getOrDefault(false)
        if (!verified) {
            // Le fichier raté est retiré ; les sauvegardes précédentes n'ont pas été touchées.
            runCatching { folder.delete(handle) }
            return BackupProblem.VERIFY_FAILED
        }
        // Rotation : seulement après vérification. Un échec de suppression n'est pas une perte de données.
        runCatching {
            val entries = folder.entries()
            val doomed = BackupFileNames.toDelete(entries.map { it.name }, justWritten = entries.firstOrNull { it.handle == handle }?.name ?: name)
            entries.filter { it.name in doomed }.forEach { runCatching { folder.delete(it.handle) } }
        }
        return null
    }
}
