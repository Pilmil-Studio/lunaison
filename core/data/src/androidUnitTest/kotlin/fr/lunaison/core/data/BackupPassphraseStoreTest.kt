package fr.lunaison.core.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La phrase secrète n'est jamais stockée en clair et se relit à l'identique, avec un Keystore simulé. */
class BackupPassphraseStoreTest {

    private val wrapper = FakeKeyWrapper()
    private val keyStore = FakeKeyStore()
    private val store = BackupPassphraseStore(wrapper, keyStore)

    @Test
    fun `aucune phrase au depart`() {
        assertEquals(PassphraseState.ABSENT, store.state())
        assertNull(store.read())
    }

    @Test
    fun `phrase enregistree puis relue a l identique, accents compris`() {
        val phrase = "Été à la mer, 2026 !".toCharArray()
        store.save(phrase.copyOf())

        assertEquals(PassphraseState.READABLE, store.state())
        assertContentEquals(phrase, store.read())
    }

    @Test
    fun `la phrase n apparait jamais en clair dans ce qui est stocke`() {
        val phrase = "phrase-secrete-visible"
        store.save(phrase.toCharArray())

        val stored = keyStore.value!!.decodeToString()
        assertTrue(!stored.contains(phrase))
    }

    @Test
    fun `une phrase vide est refusee`() {
        assertFailsWith<IllegalArgumentException> { store.save(CharArray(0)) }
        assertEquals(PassphraseState.ABSENT, store.state())
    }

    @Test
    fun `cle d enveloppe perdue la phrase est declaree illisible`() {
        store.save("une phrase assez longue".toCharArray())
        wrapper.deleteKey()

        assertEquals(PassphraseState.UNREADABLE, store.state())
        assertNull(store.read())
    }

    @Test
    fun `donnees alterees la phrase est declaree illisible`() {
        store.save("une phrase assez longue".toCharArray())
        keyStore.value = keyStore.value!!.also { it[0] = (it[0] + 1).toByte() }

        assertEquals(PassphraseState.UNREADABLE, store.state())
        assertNull(store.read())
    }

    @Test
    fun `effacer oublie la phrase et sa cle`() {
        store.save("une phrase assez longue".toCharArray())
        store.clear()

        assertEquals(PassphraseState.ABSENT, store.state())
        assertTrue(!wrapper.hasKey())
    }

    @Test
    fun `changer de phrase remplace la precedente`() {
        store.save("première phrase secrète".toCharArray())
        store.save("deuxième phrase secrète".toCharArray())

        assertContentEquals("deuxième phrase secrète".toCharArray(), store.read())
    }
}
