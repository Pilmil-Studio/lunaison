package fr.lunaison.core.data

import android.content.Context
import java.nio.ByteBuffer
import java.nio.CharBuffer

/** Ce que l'on sait de la phrase secrète de la sauvegarde automatique. */
enum class PassphraseState {
    /** Aucune phrase n'a été définie. */
    ABSENT,

    /** Une phrase est définie et se relit. */
    READABLE,

    /** Une phrase a été définie mais ne se relit plus (clé du Keystore perdue ou données altérées). */
    UNREADABLE,
}

/**
 * Phrase secrète de la sauvegarde automatique, conservée sur le téléphone uniquement sous forme enveloppée
 * par l'Android Keystore (clé d'enveloppe distincte de celle de la base). Jamais écrite en clair, jamais dans
 * un journal ; le fichier de préférences n'est inclus dans aucune sauvegarde du système (voir
 * data_extraction_rules.xml). Les tableaux de caractères rendus par [read] sont à effacer par l'appelant.
 */
class BackupPassphraseStore internal constructor(
    private val wrapper: KeyWrapper,
    private val store: WrappedKeyStore,
) {
    constructor(context: Context) : this(
        AndroidKeystoreKeyWrapper(alias = KEY_ALIAS, aad = AAD),
        PreferencesWrappedKeyStore(context, PREFS_NAME, ENTRY),
    )

    fun state(): PassphraseState {
        if (store.read() == null) return PassphraseState.ABSENT
        val plain = unwrapOrNull() ?: return PassphraseState.UNREADABLE
        plain.fill(0)
        return PassphraseState.READABLE
    }

    /** Enveloppe et enregistre la phrase, puis vérifie qu'elle se relit à l'identique. Lève une exception sinon. */
    fun save(passphrase: CharArray) {
        require(passphrase.isNotEmpty()) { "Phrase vide" }
        val plain = toUtf8(passphrase)
        try {
            store.write(wrapper.wrap(plain))
            val reread = unwrapOrNull()
            val same = reread != null && reread.contentEquals(plain)
            reread?.fill(0)
            if (!same) {
                clear()
                error("La phrase ne se relit pas après enregistrement")
            }
        } finally {
            plain.fill(0)
        }
    }

    /** La phrase, ou null si elle est absente ou illisible. */
    fun read(): CharArray? {
        val plain = unwrapOrNull() ?: return null
        return try {
            fromUtf8(plain)
        } finally {
            plain.fill(0)
        }
    }

    /** Oublie la phrase et sa clé d'enveloppe. */
    fun clear() {
        store.clear()
        wrapper.deleteKey()
    }

    private fun unwrapOrNull(): ByteArray? {
        val wrapped = store.read() ?: return null
        return runCatching { if (wrapper.hasKey()) wrapper.unwrap(wrapped) else null }.getOrNull()
    }

    private fun toUtf8(chars: CharArray): ByteArray {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars))
        val out = ByteArray(buffer.remaining())
        buffer.get(out)
        if (buffer.hasArray()) buffer.array().fill(0)
        return out
    }

    private fun fromUtf8(bytes: ByteArray): CharArray {
        val buffer = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes))
        val out = CharArray(buffer.remaining())
        buffer.get(out)
        if (buffer.hasArray()) buffer.array().fill('\u0000')
        return out
    }

    private companion object {
        const val KEY_ALIAS = "lunaison_cle_enveloppe_phrase"
        const val PREFS_NAME = "lunaison_phrase_sauvegarde"
        const val ENTRY = "phrase_enveloppee"
        val AAD = "lunaison-phrase-sauvegarde-v1".encodeToByteArray()
    }
}
