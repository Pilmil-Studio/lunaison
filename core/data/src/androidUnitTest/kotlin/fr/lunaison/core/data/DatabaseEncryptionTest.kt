package fr.lunaison.core.data

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Machine d'états de la conversion : chaque état intermédiaire possible après un arrêt brutal. */
class MigrationPlanTest {

    private fun state(main: DatabaseFileKind, pending: Boolean = false, previous: Boolean = false) =
        planNextStep(MigrationState(main, pending, previous))

    @Test
    fun `base en clair seule a convertir`() = assertEquals(MigrationStep.ENCRYPT, state(DatabaseFileKind.PLAINTEXT))

    @Test
    fun `base chiffree seule a ouvrir`() = assertEquals(MigrationStep.OPEN, state(DatabaseFileKind.ENCRYPTED))

    @Test
    fun `installation neuve`() = assertEquals(MigrationStep.OPEN, state(DatabaseFileKind.ABSENT))

    @Test
    fun `copie partielle laissee par un arret pendant l export`() =
        assertEquals(MigrationStep.DISCARD_PENDING, state(DatabaseFileKind.PLAINTEXT, pending = true))

    @Test
    fun `arret entre les deux renommages`() {
        assertEquals(MigrationStep.RESTORE_PREVIOUS, state(DatabaseFileKind.ABSENT, pending = true, previous = true))
        assertEquals(MigrationStep.RESTORE_PREVIOUS, state(DatabaseFileKind.EMPTY, previous = true))
    }

    @Test
    fun `arret avant l effacement de l ancienne base`() =
        assertEquals(MigrationStep.WIPE_PREVIOUS, state(DatabaseFileKind.ENCRYPTED, previous = true))

    @Test
    fun `etat anormal sans effacement`() =
        assertEquals(MigrationStep.SET_ASIDE_PREVIOUS, state(DatabaseFileKind.PLAINTEXT, previous = true))

    @Test
    fun `reconnaissance de l en-tete`() {
        val header = "SQLite format 3\u0000".encodeToByteArray()
        assertEquals(DatabaseFileKind.PLAINTEXT, classifyDatabaseHeader(4096, header))
        assertEquals(DatabaseFileKind.ENCRYPTED, classifyDatabaseHeader(4096, Random(1).nextBytes(16)))
        assertEquals(DatabaseFileKind.ENCRYPTED, classifyDatabaseHeader(3, byteArrayOf(1, 2, 3)))
        assertEquals(DatabaseFileKind.EMPTY, classifyDatabaseHeader(0, ByteArray(0)))
    }

    @Test
    fun `format de cle brute SQLCipher`() {
        val key = ByteArray(DATABASE_KEY_BYTES) { it.toByte() }
        val raw = sqlCipherRawKey(key).decodeToString()
        assertEquals(67, raw.length)
        assertTrue(raw.startsWith("x'000102") && raw.endsWith("1F'"))
        assertFailsWith<IllegalArgumentException> { sqlCipherRawKey(ByteArray(16)) }
    }
}

/** Gestion de la clé derrière des interfaces : sans Keystore. */
class DatabaseKeyManagerTest {

    private val wrapper = FakeKeyWrapper()
    private val store = FakeKeyStore()
    private val manager = DatabaseKeyManager(wrapper, store) { size -> Random.nextBytes(size) }

    @Test
    fun `cle creee puis relue a l identique`() {
        val key = manager.create().copyOf()
        assertEquals(DATABASE_KEY_BYTES, key.size)
        assertFalse(store.value.contentEquals(key), "la clé n'est jamais stockée en clair")
        assertContentEquals(key, manager.existing())
    }

    @Test
    fun `cle d enveloppe disparue`() {
        manager.create()
        wrapper.present = false
        assertNull(manager.existing())
    }

    @Test
    fun `cle enveloppee alteree`() {
        manager.create()
        store.value = store.value?.also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(manager.existing())
    }

    @Test
    fun `aucune cle au depart`() = assertNull(manager.existing())
}

/** Enchaînement complet sur de vrais fichiers, avec un moteur simulé. */
class DatabaseEncryptorTest {

    private val dir: File = Files.createTempDirectory("lunaison-chiffrement").toFile()
    private val files = DatabaseFiles(File(dir, "lunaison.db"))
    private val engine = FakeEngine()
    private val keys = DatabaseKeyManager(FakeKeyWrapper(), FakeKeyStore()) { size -> Random.nextBytes(size) }
    private val encryptor = DatabaseEncryptor(files, keys, engine)

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun writePlaintext(file: File, rows: Int) = file.writeBytes(FakeEngine.plaintext(8, rows))

    @Test
    fun `conversion d une base existante`() {
        writePlaintext(files.main, rows = 42)
        File(files.main.path + "-shm").writeBytes(ByteArray(8))

        val prepared = assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())

        assertEquals(DatabaseFileKind.ENCRYPTED, files.kindOf(files.main))
        assertEquals(DatabaseSnapshot(8, mapOf("day_logs" to 42L)), engine.inspectEncrypted(files.main, sqlCipherRawKey(prepared.key)))
        assertFalse(files.previous.exists())
        assertFalse(files.pending.exists())
        assertFalse(File(files.main.path + "-shm").exists())
    }

    @Test
    fun `deuxieme lancement sans nouvelle conversion`() {
        writePlaintext(files.main, rows = 3)
        val first = assertIs<PreparedDatabase.Encrypted>(encryptor.prepare()).key.copyOf()
        val second = assertIs<PreparedDatabase.Encrypted>(encryptor.prepare()).key
        assertContentEquals(first, second)
        assertEquals(1, engine.exports)
    }

    @Test
    fun `echec de l export la base d origine reste intacte`() {
        writePlaintext(files.main, rows = 5)
        val original = files.main.readBytes()
        engine.failExport = true

        assertEquals(PreparedDatabase.Plaintext, encryptor.prepare())
        assertContentEquals(original, files.main.readBytes())
        assertFalse(files.pending.exists())
        assertFalse(files.previous.exists())

        engine.failExport = false
        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        assertEquals(DatabaseFileKind.ENCRYPTED, files.kindOf(files.main))
    }

    @Test
    fun `copie incomplete refusee`() {
        writePlaintext(files.main, rows = 5)
        val original = files.main.readBytes()
        engine.dropRows = true
        assertEquals(PreparedDatabase.Plaintext, encryptor.prepare())
        assertContentEquals(original, files.main.readBytes())
    }

    @Test
    fun `reprise apres un arret entre les deux renommages`() {
        writePlaintext(files.previous, rows = 7)
        files.pending.writeBytes(byteArrayOf(9, 9, 9))

        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        assertEquals(DatabaseFileKind.ENCRYPTED, files.kindOf(files.main))
        assertFalse(files.previous.exists())
        assertFalse(files.pending.exists())
    }

    @Test
    fun `reprise apres un arret avant l effacement de l ancienne base`() {
        writePlaintext(files.main, rows = 4)
        val key = assertIs<PreparedDatabase.Encrypted>(encryptor.prepare()).key.copyOf()
        writePlaintext(files.previous, rows = 4)

        val again = assertIs<PreparedDatabase.Encrypted>(encryptor.prepare()).key
        assertContentEquals(key, again)
        assertFalse(files.previous.exists())
    }

    @Test
    fun `base chiffree illisible avec ancienne base presente on revient a l ancienne`() {
        files.main.writeBytes(Random.nextBytes(64))
        writePlaintext(files.previous, rows = 6)

        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        assertEquals(DatabaseFileKind.ENCRYPTED, files.kindOf(files.main))
        assertEquals(6L, engine.lastEncryptedRows)
        assertTrue(dir.listFiles().orEmpty().any { it.name.startsWith("lunaison.db-illisible-") })
    }

    @Test
    fun `base chiffree sans cle mise de cote jamais effacee`() {
        val foreign = Random.nextBytes(64)
        files.main.writeBytes(foreign)

        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        assertFalse(files.main.exists())
        val kept = dir.listFiles().orEmpty().single { it.name.startsWith("lunaison.db-illisible-") }
        assertContentEquals(foreign, kept.readBytes())
    }

    @Test
    fun `etat anormal les deux bases en clair sont conservees`() {
        writePlaintext(files.main, rows = 2)
        writePlaintext(files.previous, rows = 9)

        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        val kept = dir.listFiles().orEmpty().single { it.name.startsWith("lunaison.db-ancienne-conservee-") }
        assertEquals(DatabaseFileKind.PLAINTEXT, files.kindOf(kept))
    }

    @Test
    fun `installation neuve`() {
        assertIs<PreparedDatabase.Encrypted>(encryptor.prepare())
        assertNotNull(keys.existing())
        assertEquals(0, engine.exports)
    }
}

/** Enveloppe simulée : XOR avec une clé interne et une étiquette de contrôle. */
internal class FakeKeyWrapper : KeyWrapper {
    var present = false
    private var secret = 0

    override fun hasKey() = present

    override fun wrap(plain: ByteArray): ByteArray {
        if (!present) {
            present = true
            secret = Random.nextInt(1, 255)
        }
        val body = ByteArray(plain.size) { (plain[it].toInt() xor secret).toByte() }
        return body + byteArrayOf(plain.sum().toByte())
    }

    override fun unwrap(wrapped: ByteArray): ByteArray? {
        if (!present || wrapped.isEmpty()) return null
        val plain = ByteArray(wrapped.size - 1) { (wrapped[it].toInt() xor secret).toByte() }
        return plain.takeIf { it.sum().toByte() == wrapped.last() }
    }

    override fun deleteKey() {
        present = false
    }

    private fun ByteArray.sum(): Int = fold(0) { acc, b -> acc + b }
}

internal class FakeKeyStore : WrappedKeyStore {
    var value: ByteArray? = null

    override fun read() = value?.copyOf()

    override fun write(value: ByteArray) {
        this.value = value.copyOf()
    }

    override fun clear() {
        value = null
    }
}

/**
 * Moteur simulé. Base « en clair » : en-tête SQLite puis `version;lignes`.
 * Base « chiffrée » : `CHIFFRE`, la clé, puis le même contenu.
 */
internal class FakeEngine : EncryptionEngine {
    var failExport = false
    var dropRows = false
    var exports = 0
    var lastEncryptedRows = -1L

    override fun inspectPlaintext(file: File): DatabaseSnapshot = parse(file.readBytes().drop(HEADER.size))

    override fun exportEncrypted(source: File, target: File, passphrase: ByteArray, userVersion: Int) {
        check(!failExport) { "export simulé en échec" }
        exports++
        val rows = inspectPlaintext(source).rowCounts.getValue("day_logs") - if (dropRows) 1 else 0
        target.writeBytes(MAGIC + passphrase + "$userVersion;$rows".encodeToByteArray())
    }

    override fun inspectEncrypted(file: File, passphrase: ByteArray): DatabaseSnapshot {
        val bytes = file.readBytes()
        val prefix = MAGIC + passphrase
        check(bytes.size >= prefix.size && bytes.copyOf(prefix.size).contentEquals(prefix)) { "clé incorrecte" }
        return parse(bytes.drop(prefix.size)).also { lastEncryptedRows = it.rowCounts.getValue("day_logs") }
    }

    private fun parse(content: List<Byte>): DatabaseSnapshot {
        val (version, rows) = content.toByteArray().decodeToString().split(";")
        return DatabaseSnapshot(version.toInt(), mapOf("day_logs" to rows.toLong()))
    }

    companion object {
        private val HEADER = "SQLite format 3\u0000".encodeToByteArray()
        private val MAGIC = "CHIFFRE".encodeToByteArray()

        fun plaintext(version: Int, rows: Int) = HEADER + "$version;$rows".encodeToByteArray()
    }
}
