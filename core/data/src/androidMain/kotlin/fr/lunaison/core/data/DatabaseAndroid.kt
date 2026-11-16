package fr.lunaison.core.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import java.io.File
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

private const val DATABASE_NAME = "lunaison.db"
private const val LOG_TAG = "Lunaison"

/**
 * Base Android : même fichier (`lunaison.db`) qu'avant, chiffré au repos par SQLCipher.
 * Une base en clair laissée par une version précédente est convertie au premier lancement
 * (voir [DatabaseEncryptor] et documentation/chiffrement.md). Si la conversion échoue, la base
 * d'origine reste utilisée telle quelle et la conversion est retentée au lancement suivant.
 */
fun LunaisonDatabase.Companion.create(context: Context): LunaisonDatabase {
    val appContext = context.applicationContext
    val file = appContext.getDatabasePath(DATABASE_NAME)
    file.parentFile?.mkdirs()
    val builder = Room.databaseBuilder<LunaisonDatabase>(
        context = appContext,
        name = file.absolutePath,
    ).withMigrations()
    when (val prepared = prepareEncryption(appContext, file)) {
        is PreparedDatabase.Encrypted -> {
            val passphrase = sqlCipherRawKey(prepared.key)
            prepared.key.fill(0)
            builder.openHelperFactory(SupportOpenHelperFactory(passphrase))
        }
        PreparedDatabase.Plaintext -> Unit
    }
    return builder.build()
}

private fun prepareEncryption(context: Context, file: File): PreparedDatabase {
    val files = DatabaseFiles(file)
    if (!SqlCipherEngine.loadNativeLibrary()) {
        Log.w(LOG_TAG, "Moteur de chiffrement indisponible : base laissée en clair")
        val state = files.observe()
        if ((state.main == DatabaseFileKind.ABSENT || state.main == DatabaseFileKind.EMPTY) && state.previousExists) {
            files.restorePrevious()
        }
        check(files.observe().main != DatabaseFileKind.ENCRYPTED) { "Base chiffrée mais moteur de chiffrement indisponible" }
        return PreparedDatabase.Plaintext
    }
    val encryptor = DatabaseEncryptor(
        files = files,
        keys = androidDatabaseKeyManager(context),
        engine = SqlCipherEngine(),
        log = { message -> Log.i(LOG_TAG, message) },
    )
    return encryptor.prepare()
}

fun LunaisonRepository.Companion.create(context: Context): LunaisonRepository =
    LunaisonRepository(LunaisonDatabase.create(context).dao())
