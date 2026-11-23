package fr.lunaison.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileNotFoundException

/**
 * Dossier choisi par la personne avec le sélecteur du système (ACTION_OPEN_DOCUMENT_TREE). L'accès passe par une
 * permission d'URI persistante sur ce seul dossier : aucun accès au stockage global, aucune permission au manifeste.
 */
class SafFolder(private val context: Context, private val treeUri: Uri) : BackupFolder {

    private val resolver get() = context.contentResolver
    private val rootDocumentId: String get() = DocumentsContract.getTreeDocumentId(treeUri)
    private val rootUri: Uri get() = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)

    private fun documentUri(handle: String): Uri = Uri.parse(handle)

    /** La permission persistante est toujours là, en écriture, et le dossier existe encore. */
    fun isUsable(): Boolean {
        val granted = resolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission && it.isWritePermission }
        if (!granted) return false
        return runCatching {
            resolver.query(rootUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { it.moveToFirst() } == true
        }.getOrDefault(false)
    }

    /** Nom du dossier tel que le système l'affiche, ou null. */
    fun displayName(): String? = runCatching {
        resolver.query(rootUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()

    override fun entries(): List<FolderEntry> {
        if (!isUsable()) throw FolderUnavailableException("Dossier inaccessible")
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return try {
            resolver.query(children, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0) ?: continue
                        val name = cursor.getString(1) ?: continue
                        add(FolderEntry(DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString(), name))
                    }
                }
            } ?: throw FolderUnavailableException("Dossier illisible")
        } catch (e: SecurityException) {
            throw FolderUnavailableException("Permission révoquée", e)
        } catch (e: IllegalArgumentException) {
            throw FolderUnavailableException("Dossier introuvable", e)
        }
    }

    override fun write(name: String, bytes: ByteArray): String {
        val createdOrNull: Uri? = try {
            DocumentsContract.createDocument(resolver, rootUri, MIME_TYPE, name)
        } catch (e: FileNotFoundException) {
            throw FolderUnavailableException("Dossier introuvable", e)
        } catch (e: SecurityException) {
            throw FolderUnavailableException("Permission révoquée", e)
        }
        val created = createdOrNull ?: throw FolderUnavailableException("Création du fichier refusée")
        try {
            val out = resolver.openOutputStream(created, "wt") ?: error("Ouverture en écriture impossible")
            out.use { it.write(bytes) }
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, created) }
            throw e
        }
        return created.toString()
    }

    override fun read(handle: String): ByteArray =
        resolver.openInputStream(documentUri(handle))?.use { it.readBytes() } ?: error("Lecture impossible")

    override fun delete(handle: String) {
        DocumentsContract.deleteDocument(resolver, documentUri(handle))
    }

    companion object {
        const val MIME_TYPE = "application/octet-stream"
        const val PERMISSION_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /** Garde l'accès au dossier après le redémarrage du téléphone. */
        fun persist(context: Context, uri: Uri) {
            context.contentResolver.takePersistableUriPermission(uri, PERMISSION_FLAGS)
        }

        /** Relâche l'accès à un dossier que la personne n'utilise plus. */
        fun release(context: Context, uri: Uri) {
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, PERMISSION_FLAGS) }
        }
    }
}
