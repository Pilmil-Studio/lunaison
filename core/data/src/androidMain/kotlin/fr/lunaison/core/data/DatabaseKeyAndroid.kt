package fr.lunaison.core.data

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Clé d'enveloppe AES-256-GCM rangée dans l'Android Keystore : non exportable, elle ne quitte jamais
 * le téléphone et ne part dans aucune sauvegarde. Elle n'exige pas le déverrouillage de l'écran, pour
 * que les rappels et le widget continuent de lire la base.
 */
internal class AndroidKeystoreKeyWrapper(
    private val alias: String = KEY_ALIAS,
    private val aad: ByteArray = AAD,
) : KeyWrapper {

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun hasKey(): Boolean = keyStore().containsAlias(alias)

    override fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey() ?: generateKey())
        cipher.updateAAD(aad)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "Vecteur d'initialisation inattendu" }
        return byteArrayOf(FORMAT_VERSION) + iv + cipher.doFinal(plain)
    }

    override fun unwrap(wrapped: ByteArray): ByteArray? {
        if (wrapped.size <= 1 + IV_BYTES || wrapped[0] != FORMAT_VERSION) return null
        val key = try {
            secretKey()
        } catch (e: UnrecoverableKeyException) {
            null
        } ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, wrapped, 1, IV_BYTES))
        cipher.updateAAD(aad)
        return try {
            cipher.doFinal(wrapped, 1 + IV_BYTES, wrapped.size - 1 - IV_BYTES)
        } catch (e: AEADBadTagException) {
            null
        }
    }

    override fun deleteKey() {
        val store = keyStore()
        if (store.containsAlias(alias)) store.deleteEntry(alias)
    }

    private fun secretKey(): SecretKey? = keyStore().getKey(alias, null) as? SecretKey

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    internal companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lunaison_cle_enveloppe_base"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        const val FORMAT_VERSION: Byte = 1
        val AAD = "lunaison-cle-base-v1".encodeToByteArray()
    }
}

/**
 * Clé enveloppée, dans un fichier de préférences à part. Ce fichier est exclu de toute sauvegarde et de
 * tout transfert (voir data_extraction_rules.xml) : sur un autre téléphone, il serait de toute façon
 * illisible sans la clé d'enveloppe du Keystore.
 */
internal class PreferencesWrappedKeyStore(
    context: Context,
    prefsName: String = PREFS_NAME,
    private val entry: String = ENTRY,
) : WrappedKeyStore {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun read(): ByteArray? {
        val encoded = prefs.getString(entry, null) ?: return null
        return runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
    }

    // Écriture synchrone : la clé doit être sur le disque avant que la base chiffrée n'existe.
    @SuppressLint("ApplySharedPref")
    override fun write(value: ByteArray) {
        check(prefs.edit().putString(entry, Base64.encodeToString(value, Base64.NO_WRAP)).commit()) {
            "Enregistrement de la clé impossible"
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun clear() {
        prefs.edit().remove(entry).commit()
    }

    companion object {
        /** Nom du fichier de préférences, repris dans data_extraction_rules.xml. */
        const val PREFS_NAME = "lunaison_cle_base"
        const val ENTRY = "cle_enveloppee"
    }
}

private val keyRandom = SecureRandom()

internal fun androidDatabaseKeyManager(context: Context): DatabaseKeyManager = DatabaseKeyManager(
    wrapper = AndroidKeystoreKeyWrapper(),
    store = PreferencesWrappedKeyStore(context),
    randomBytes = { size -> ByteArray(size).also { keyRandom.nextBytes(it) } },
)
