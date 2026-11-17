package fr.lunaison.core.data

/** Taille de la clé de la base : 256 bits. */
internal const val DATABASE_KEY_BYTES = 32

/**
 * Enveloppe la clé de la base avec une clé qui ne quitte jamais le téléphone (Android Keystore).
 * Derrière une interface pour tester la logique sans Keystore.
 */
internal interface KeyWrapper {
    /** La clé d'enveloppe existe encore dans le Keystore. */
    fun hasKey(): Boolean

    /** Chiffre [plain] ; crée la clé d'enveloppe si elle n'existe pas. */
    fun wrap(plain: ByteArray): ByteArray

    /**
     * Déchiffre une clé enveloppée. Renvoie null si elle est définitivement illisible (clé d'enveloppe
     * absente ou différente, données altérées). Une erreur passagère du Keystore lève une exception.
     */
    fun unwrap(wrapped: ByteArray): ByteArray?

    fun deleteKey()
}

/** Lieu de stockage de la clé enveloppée (préférences privées de l'appli). */
internal interface WrappedKeyStore {
    fun read(): ByteArray?

    fun write(value: ByteArray)

    fun clear()
}

/**
 * Clé aléatoire de la base, jamais écrite en clair : seule sa version enveloppée est stockée.
 *
 * [create] remplace toute clé précédente : l'appelant ne l'utilise que lorsqu'aucune base chiffrée
 * lisible n'existe (première installation, conversion d'une base en clair, base illisible mise de côté).
 */
internal class DatabaseKeyManager(
    private val wrapper: KeyWrapper,
    private val store: WrappedKeyStore,
    private val randomBytes: (Int) -> ByteArray,
) {
    /** La clé actuelle, ou null s'il n'y en a pas ou si elle est définitivement perdue. */
    fun existing(): ByteArray? {
        val wrapped = store.read() ?: return null
        if (!wrapper.hasKey()) return null
        val key = wrapper.unwrap(wrapped) ?: return null
        if (key.size != DATABASE_KEY_BYTES) {
            key.fill(0)
            return null
        }
        return key
    }

    /** Crée, enveloppe et enregistre une nouvelle clé, puis vérifie qu'elle se relit à l'identique. */
    fun create(): ByteArray {
        discard()
        val key = randomBytes(DATABASE_KEY_BYTES)
        check(key.size == DATABASE_KEY_BYTES) { "Taille de clé inattendue" }
        store.write(wrapper.wrap(key))
        val reread = existing()
        val same = reread != null && reread.contentEquals(key)
        reread?.fill(0)
        if (!same) {
            key.fill(0)
            discard()
            error("La clé de la base ne se relit pas après enregistrement")
        }
        return key
    }

    /** Oublie la clé enveloppée et la clé d'enveloppe. */
    fun discard() {
        store.clear()
        wrapper.deleteKey()
    }
}

private val HEX_DIGITS = "0123456789ABCDEF".encodeToByteArray()

/**
 * Clé brute au format attendu par SQLCipher (`x'<64 chiffres hexadécimaux>'`, en octets ASCII) :
 * SQLCipher l'utilise telle quelle, sans dérivation PBKDF2 inutile pour une clé déjà aléatoire.
 * Construite octet par octet pour ne jamais passer par une chaîne de caractères.
 */
internal fun sqlCipherRawKey(key: ByteArray): ByteArray {
    require(key.size == DATABASE_KEY_BYTES) { "Taille de clé inattendue" }
    val out = ByteArray(key.size * 2 + 3)
    out[0] = 'x'.code.toByte()
    out[1] = '\''.code.toByte()
    key.forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xFF
        out[2 + 2 * index] = HEX_DIGITS[value ushr 4]
        out[3 + 2 * index] = HEX_DIGITS[value and 0x0F]
    }
    out[out.size - 1] = '\''.code.toByte()
    return out
}
