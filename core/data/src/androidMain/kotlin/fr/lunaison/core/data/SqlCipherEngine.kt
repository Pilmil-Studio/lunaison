package fr.lunaison.core.data

import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Conversion avec SQLCipher : la base chiffrée est ouverte avec sa clé, la base en clair y est attachée
 * sans clé, puis `sqlcipher_export` recopie schéma et données. La version du schéma (`user_version`),
 * que l'export ne recopie pas, est reportée à la main pour que Room ne rejoue aucune migration.
 */
internal class SqlCipherEngine : EncryptionEngine {

    override fun inspectPlaintext(file: File): DatabaseSnapshot {
        val db = open(file, ByteArray(0), create = false)
        try {
            // Quitte le mode WAL : le journal est reporté dans le fichier principal puis supprimé.
            val mode = db.rawQuery("PRAGMA journal_mode = DELETE", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            check(mode.equals("delete", ignoreCase = true)) { "Journal WAL non vidé" }
            return snapshot(db)
        } finally {
            db.close()
        }
    }

    override fun exportEncrypted(source: File, target: File, passphrase: ByteArray, userVersion: Int) {
        val db = open(target, passphrase, create = true)
        try {
            db.execSQL("ATTACH DATABASE ? AS clair KEY ''", arrayOf<Any?>(source.path))
            db.rawQuery("SELECT sqlcipher_export('main', 'clair')", arrayOf<String>()).use { it.moveToFirst() }
            db.execSQL("DETACH DATABASE clair")
            db.version = userVersion
        } finally {
            db.close()
        }
    }

    override fun inspectEncrypted(file: File, passphrase: ByteArray): DatabaseSnapshot {
        val db = open(file, passphrase, create = false)
        try {
            val result = db.rawQuery("PRAGMA quick_check", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            check(result == "ok") { "Contrôle d'intégrité en échec" }
            return snapshot(db)
        } finally {
            db.close()
        }
    }

    private fun open(file: File, passphrase: ByteArray, create: Boolean): SQLiteDatabase {
        // Sans collations localisées : la connexion ne crée pas de table android_metadata,
        // qui ferait échouer l'export (la table vient déjà de la base d'origine).
        var flags = SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        if (create) flags = flags or SQLiteDatabase.CREATE_IF_NECESSARY
        return SQLiteDatabase.openDatabase(file.path, passphrase, null, flags, null, null)
    }

    private fun snapshot(db: SQLiteDatabase): DatabaseSnapshot {
        val tables = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
            arrayOf<String>(),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        val counts = tables.associateWith { table ->
            val quoted = "\"" + table.replace("\"", "\"\"") + "\""
            db.rawQuery("SELECT COUNT(*) FROM $quoted", arrayOf<String>()).use { cursor ->
                check(cursor.moveToFirst()) { "Comptage impossible" }
                cursor.getLong(0)
            }
        }
        return DatabaseSnapshot(db.version, counts)
    }

    companion object {
        /** Charge la bibliothèque native une seule fois ; renvoie false si elle est absente ou incompatible. */
        fun loadNativeLibrary(): Boolean = loaded

        private val loaded: Boolean by lazy {
            try {
                System.loadLibrary("sqlcipher")
                true
            } catch (e: UnsatisfiedLinkError) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
    }
}
