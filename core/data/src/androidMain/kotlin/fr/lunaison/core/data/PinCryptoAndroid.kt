package fr.lunaison.core.data

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val secureRandom = SecureRandom()

internal actual fun randomPinSalt(size: Int): ByteArray =
    ByteArray(size).also { secureRandom.nextBytes(it) }

internal actual fun pbkdf2HmacSha256(
    pin: String,
    salt: ByteArray,
    iterations: Int,
    keyLengthBytes: Int,
): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, keyLengthBytes * 8)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}
