package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.of
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Fichiers Clue : JSON, `.cluedata`, archive ZipCrypto, archive AES (refusée), et fusion sans écrasement. */
class ClueFileAndroidTest {
    private val clueJson = """{"2024-03-05": {"period": "heavy"}, "2024-03-06": {"period": "light"}}"""
    private val password = "mot-de-passe-clue"

    private fun d(day: Int) = LocalDate.of(2024, 3, day)

    /** Archive à une entrée, méthode « stored », chiffrée AES (méthode 99 et champ supplémentaire 0x9901) : structure seulement. */
    private fun aesArchive(withMethod99: Boolean = true, withExtra: Boolean = true): ByteArray {
        val name = "clue.json".toByteArray()
        val extra = if (withExtra) {
            ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN).putShort(0x9901.toShort()).putShort(7).putShort(2).putShort(0x4541).put(3).putShort(8).array()
        } else {
            ByteArray(0)
        }
        val payload = ByteArray(40) { it.toByte() }
        val header = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(0x04034B50).putShort(51).putShort(1).putShort((if (withMethod99) 99 else 8).toShort())
            .putShort(0).putShort(0).putInt(0).putInt(payload.size).putInt(payload.size)
            .putShort(name.size.toShort()).putShort(extra.size.toShort())
        return ByteArrayOutputStream().apply { write(header.array()); write(name); write(extra); write(payload) }.toByteArray()
    }

    private fun zipCrypto(vararg entries: Pair<String, String>): ByteArray =
        PeriodCalendarImporterTest().zipEntries(entries.toList(), password)

    // --- Detection AES / ZipCrypto -----------------------------------------------------------------

    @Test
    fun `une archive AES est detectee par la methode 99 ou par le champ 0x9901`() {
        assertTrue(PeriodCalendarImporter.isAesEncryptedZip(aesArchive()))
        assertTrue(PeriodCalendarImporter.isAesEncryptedZip(aesArchive(withMethod99 = true, withExtra = false)))
        assertTrue(PeriodCalendarImporter.isAesEncryptedZip(aesArchive(withMethod99 = false, withExtra = true)))
    }

    @Test
    fun `une archive ZipCrypto ou non chiffree n'est pas AES`() {
        assertFalse(PeriodCalendarImporter.isAesEncryptedZip(zipCrypto("clue.json" to clueJson)))
        val plain = PeriodCalendarImporterTest().zipEntries(listOf("clue.json" to clueJson), null)
        assertFalse(PeriodCalendarImporter.isAesEncryptedZip(plain))
        assertFalse(PeriodCalendarImporter.isAesEncryptedZip(ByteArray(0)))
        assertFalse(PeriodCalendarImporter.isAesEncryptedZip("{}".toByteArray()))
        // En-tête tronqué : pas d'exception.
        assertFalse(PeriodCalendarImporter.isAesEncryptedZip(aesArchive().copyOf(20)))
    }

    @Test
    fun `une archive AES donne l'erreur de chiffrement avant toute demande de mot de passe`() {
        assertFailsWith<UnsupportedEncryptionException> { previewClueFile(aesArchive(), null) }
        assertFailsWith<UnsupportedEncryptionException> { previewClueFile(aesArchive(), password) }
        assertFailsWith<UnsupportedEncryptionException> { PeriodCalendarImporter.extractZip(aesArchive(), password) }
    }

    @Test
    fun `le message d'erreur AES invite a decompresser puis importer le JSON`() {
        val message = UnsupportedEncryptionException().message.orEmpty()
        assertTrue("décompressez l'archive" in message && "JSON" in message)
    }

    // --- Lecture des fichiers ----------------------------------------------------------------------

    @Test
    fun `un fichier json ou cluedata se lit directement`() {
        val preview = previewClueFile(clueJson.toByteArray(), null)
        assertEquals(ImportSource.CLUE, preview.source)
        assertEquals(listOf(Flow.HEAVY, Flow.LIGHT), preview.dayLogs.map { it.flow })
        assertEquals(1, preview.cyclesCount)
    }

    @Test
    fun `une archive ZipCrypto se lit avec le mot de passe et ignore les autres entrees`() {
        val zip = zipCrypto("settings.txt" to "pas du json", "clue.json" to clueJson, "notes.cluedata" to """{"2024-04-01": {"period": "medium"}}""")
        val preview = previewClueFile(zip, password)
        assertEquals(listOf(d(5), d(6), LocalDate.of(2024, 4, 1)), preview.dayLogs.map { it.date })
    }

    @Test
    fun `une archive ZipCrypto sans ou avec un mauvais mot de passe est refusee`() {
        val zip = zipCrypto("clue.json" to clueJson)
        assertFailsWith<PasswordRequiredException> { previewClueFile(zip, null) }
        assertFailsWith<InvalidPasswordException> { previewClueFile(zip, "faux") }
    }

    @Test
    fun `fichier vide, illisible ou sans donnees`() {
        assertFailsWith<NothingToImportException> { previewClueFile(ByteArray(0), null) }
        assertFailsWith<UnreadableImportException> { previewClueFile("n'importe quoi".toByteArray(), null) }
        assertFailsWith<NothingToImportException> { previewClueFile("""{"settings": {"unit": "kg"}}""".toByteArray(), null) }
        // Archive sans aucun fichier JSON : rien à lire.
        val noJson = PeriodCalendarImporterTest().zipEntries(listOf("a.txt" to "x"), null)
        assertFailsWith<UnreadableImportException> { previewClueFile(noJson, null) }
    }

    // --- Fusion sans ecrasement ---------------------------------------------------------------------

    @Test
    fun `l'import ecrit les nouvelles journees et ne touche pas aux saisies de Lunaison`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        repo.save(DayLog(date = d(5), flow = Flow.LIGHT, pain = Pain.SEVERE, note = "ma note"))
        val preview = previewClueFile(clueJson.toByteArray(), null)

        val plan = repo.planImport(preview)
        assertEquals(1, plan.keptExisting)
        assertEquals(listOf(d(6)), plan.toWrite.map { it.date })
        // L'aperçu n'écrit rien.
        assertEquals(1, repo.logsOnce().size)

        assertEquals(1, repo.applyImport(plan))
        val logs = repo.logsOnce().associateBy { it.date }
        assertEquals(Flow.LIGHT, logs.getValue(d(5)).flow)
        assertEquals("ma note", logs.getValue(d(5)).note)
        assertEquals(Pain.SEVERE, logs.getValue(d(5)).pain)
        assertEquals(Flow.LIGHT, logs.getValue(d(6)).flow)
    }

    @Test
    fun `une journee saisie entre l'apercu et la confirmation n'est pas ecrasee`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val plan = repo.planImport(previewClueFile(clueJson.toByteArray(), null))
        assertEquals(2, plan.toWrite.size)
        repo.save(DayLog(date = d(6), flow = Flow.HEAVY, note = "saisie tardive"))
        assertEquals(1, repo.applyImport(plan))
        assertEquals("saisie tardive", repo.logsOnce().first { it.date == d(6) }.note)
        assertEquals(Flow.HEAVY, repo.logsOnce().first { it.date == d(6) }.flow)
    }

    @Test
    fun `un deuxieme import identique ne change plus rien`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val preview = previewClueFile(clueJson.toByteArray(), null)
        repo.applyImport(repo.planImport(preview))
        val again = repo.planImport(preview)
        assertTrue(again.toWrite.isEmpty())
        assertEquals(2, again.keptExisting)
    }

    @Test
    fun `la source de sante indisponible repond non disponible et ne lit rien`() = runBlocking {
        val source = platformHealthDataSource(null)
        assertEquals(HealthAvailability.NOT_SUPPORTED, source.availability())
        assertFalse(source.hasPermission())
        assertEquals(HealthReadResult.Unavailable(HealthAvailability.NOT_SUPPORTED), source.read())
    }
}
