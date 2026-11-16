package fr.lunaison.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionComplete
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

private const val BACKUP_EXCLUDED_KEY = "lunaison.backupExcluded"

/**
 * Dossier dédié de la base, dans « Application Support » de l'appli (jamais dans « Documents »,
 * visible dans l'appli Fichiers). Le réglage de sauvegarde s'applique au dossier entier, donc aussi
 * aux fichiers -wal et -shm que SQLite recrée à volonté.
 */
@OptIn(ExperimentalForeignApi::class)
private fun databaseDirectory(): String {
    val support = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directory = requireNotNull(support?.path) { "Dossier de données introuvable" } + "/Lunaison"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return directory
}

/**
 * Sauvegarde iCloud (et des sauvegardes de l'iPhone) : incluse par défaut, car elle reste sous le
 * contrôle de l'utilisateur et de son compte Apple, pas d'un tiers. Il peut l'exclure dans les réglages.
 */
fun isBackupExcluded(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey(BACKUP_EXCLUDED_KEY)

@OptIn(ExperimentalForeignApi::class)
fun setBackupExcluded(excluded: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(excluded, forKey = BACKUP_EXCLUDED_KEY)
    applyBackupPolicy(databaseDirectory())
}

@OptIn(ExperimentalForeignApi::class)
private fun applyBackupPolicy(directory: String) {
    NSURL.fileURLWithPath(directory).setResourceValue(
        value = NSNumber(bool = isBackupExcluded()),
        forKey = NSURLIsExcludedFromBackupKey,
        error = null,
    )
}

/**
 * Protection des données d'iOS : les fichiers de la base ne sont lisibles que lorsque l'iPhone est
 * déverrouillé (classe « Complete », chiffrée avec une clé dérivée du code de l'appareil). Appliquée au
 * dossier (les fichiers -wal et -shm créés ensuite en héritent) et aux fichiers déjà présents.
 * Ce n'est pas un chiffrement propre à l'appli comme SQLCipher sur Android : voir documentation/chiffrement.md.
 */
@OptIn(ExperimentalForeignApi::class)
private fun applyFileProtection(directory: String) {
    val manager = NSFileManager.defaultManager
    val attributes = mapOf<Any?, Any?>(NSFileProtectionKey to NSFileProtectionComplete)
    manager.setAttributes(attributes, ofItemAtPath = directory, error = null)
    manager.contentsOfDirectoryAtPath(directory, error = null)?.forEach { name ->
        manager.setAttributes(attributes, ofItemAtPath = "$directory/$name", error = null)
    }
}

fun LunaisonDatabase.Companion.create(): LunaisonDatabase {
    val directory = databaseDirectory()
    applyBackupPolicy(directory)
    applyFileProtection(directory)
    return Room.databaseBuilder<LunaisonDatabase>(name = "$directory/lunaison.db")
        .withMigrations()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}

fun LunaisonRepository.Companion.create(): LunaisonRepository =
    LunaisonRepository(LunaisonDatabase.create().dao())
