package fr.lunaison.ui.backup

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** Dossier en mémoire : la rotation et les états d'erreur se testent sans téléphone. */
private class MemoryFolder : BackupFolder {
    val files = linkedMapOf<String, ByteArray>()
    var unavailable = false
    var failWrite = false
    var corruptWrites = false

    override fun entries(): List<FolderEntry> {
        if (unavailable) throw FolderUnavailableException("absent")
        return files.keys.map { FolderEntry(it, it) }
    }

    override fun write(name: String, bytes: ByteArray): String {
        if (unavailable) throw FolderUnavailableException("absent")
        if (failWrite) error("stockage plein")
        files[name] = if (corruptWrites) bytes.copyOf(bytes.size / 2) else bytes
        return name
    }

    override fun read(handle: String): ByteArray = files.getValue(handle)

    override fun delete(handle: String) {
        files.remove(handle)
    }
}

private class MemoryCopy : PrivateCopy {
    var bytes: ByteArray? = null
    var failing = false

    override fun write(bytes: ByteArray) {
        if (failing) error("disque")
        this.bytes = bytes
    }

    override fun read() = bytes

    override fun delete() {
        bytes = null
    }
}

class BackupWriterTest {

    private val today = LocalDate(2026, 11, 17)
    private val phrase = "une phrase secrète".toCharArray()
    private val folder = MemoryFolder()
    private val copy = MemoryCopy()

    // Chiffrement simulé : le contenu n'est lisible qu'avec la bonne phrase.
    private val writer = BackupWriter(
        seal = { payload, secret -> "S|${String(secret)}|$payload".toByteArray() },
        open = { bytes, secret ->
            val text = String(bytes)
            val prefix = "S|${String(secret)}|"
            check(text.startsWith(prefix)) { "phrase fausse" }
            text.removePrefix(prefix)
        },
    )

    private fun existing(vararg days: String) = days.forEach { folder.files["sauvegarde-2026-11-$it.pilmil"] = byteArrayOf(1) }

    @Test
    fun `ecriture reussie dans le dossier et dans la copie privee`() {
        val report = writer.write("donnees", phrase, today, folder, copy)

        assertNull(report.problem)
        assertTrue(report.savedInFolder)
        assertEquals(listOf("sauvegarde-2026-11-17.pilmil"), folder.files.keys.toList())
        assertContentEquals(folder.files.getValue("sauvegarde-2026-11-17.pilmil"), copy.bytes)
    }

    @Test
    fun `la rotation garde les 3 dernieres apres ecriture`() {
        existing("12", "14", "15", "16")

        val report = writer.write("donnees", phrase, today, folder, copy)

        assertNull(report.problem)
        assertEquals(
            setOf("sauvegarde-2026-11-15.pilmil", "sauvegarde-2026-11-16.pilmil", "sauvegarde-2026-11-17.pilmil"),
            folder.files.keys,
        )
    }

    @Test
    fun `un fichier etranger du dossier n est jamais touche`() {
        folder.files["releve.pdf"] = byteArrayOf(9)
        existing("12", "14", "15", "16")

        writer.write("donnees", phrase, today, folder, copy)

        assertTrue("releve.pdf" in folder.files)
    }

    @Test
    fun `dossier inaccessible - probleme signale, copie privee tout de meme ecrite`() {
        folder.unavailable = true

        val report = writer.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.FOLDER_UNAVAILABLE, report.problem)
        assertFalse(report.savedInFolder)
        assertTrue(copy.bytes != null)
    }

    @Test
    fun `ecriture impossible - les anciennes sauvegardes sont intactes`() {
        existing("14", "15", "16")
        folder.failWrite = true

        val report = writer.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.WRITE_FAILED, report.problem)
        assertEquals(3, folder.files.size)
    }

    @Test
    fun `fichier ecrit illisible - retire, anciennes sauvegardes intactes`() {
        existing("14", "15", "16")
        folder.corruptWrites = true

        val report = writer.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.VERIFY_FAILED, report.problem)
        assertEquals(
            setOf("sauvegarde-2026-11-14.pilmil", "sauvegarde-2026-11-15.pilmil", "sauvegarde-2026-11-16.pilmil"),
            folder.files.keys,
        )
    }

    @Test
    fun `chiffrement qui echoue - rien n est ecrit`() {
        val broken = BackupWriter(seal = { _, _ -> error("moteur") }, open = { _, _ -> "" })

        val report = broken.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.WRITE_FAILED, report.problem)
        assertTrue(folder.files.isEmpty())
        assertNull(copy.bytes)
    }

    @Test
    fun `chiffrement non relisible - rien n est ecrit`() {
        val lossy = BackupWriter(seal = { p, _ -> p.toByteArray() }, open = { _, _ -> "autre chose" })

        val report = lossy.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.VERIFY_FAILED, report.problem)
        assertTrue(folder.files.isEmpty())
    }

    @Test
    fun `copie privee en echec - signalee, mais la sauvegarde du dossier est faite`() {
        copy.failing = true

        val report = writer.write("donnees", phrase, today, folder, copy)

        assertEquals(BackupProblem.PRIVATE_COPY_FAILED, report.problem)
        assertTrue(report.savedInFolder)
        assertEquals(1, folder.files.size)
    }

    @Test
    fun `deuxieme sauvegarde le meme jour - nouveau nom, ancien fichier conserve`() {
        writer.write("v1", phrase, today, folder, copy)
        writer.write("v2", phrase, today, folder, copy)

        assertEquals(setOf("sauvegarde-2026-11-17.pilmil", "sauvegarde-2026-11-17-2.pilmil"), folder.files.keys)
    }

    @Test
    fun `la copie privee est remplacee atomiquement et relue`() {
        val dir = Files.createTempDirectory("lunaison-privee").toFile()
        try {
            val file = PrivateCopyFile(dir.resolve(PrivateCopyFile.DIRECTORY))
            assertNull(file.read())
            file.write(byteArrayOf(1, 2, 3))
            file.write(byteArrayOf(4, 5))
            assertContentEquals(byteArrayOf(4, 5), file.read())
            assertFalse(dir.resolve(PrivateCopyFile.DIRECTORY).list().orEmpty().any { it.endsWith(".tmp") })
            file.delete()
            assertNull(file.read())
        } finally {
            dir.deleteRecursively()
        }
    }
}
