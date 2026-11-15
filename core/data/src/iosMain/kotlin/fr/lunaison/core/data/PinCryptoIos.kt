@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package fr.lunaison.core.data

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

internal actual fun randomPinSalt(size: Int): ByteArray {
    val out = ByteArray(size)
    out.usePinned { pinned ->
        val status = SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
        check(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
    }
    return out
}

internal actual fun pbkdf2HmacSha256(
    pin: String,
    salt: ByteArray,
    iterations: Int,
    keyLengthBytes: Int,
): ByteArray {
    val password = pin.encodeToByteArray()
    val blockCount = (keyLengthBytes + 31) / 32
    val out = ByteArray(blockCount * 32)
    for (blockIndex in 1..blockCount) {
        val u1Input = ByteArray(salt.size + 4)
        salt.copyInto(u1Input)
        u1Input[salt.size] = (blockIndex ushr 24).toByte()
        u1Input[salt.size + 1] = (blockIndex ushr 16).toByte()
        u1Input[salt.size + 2] = (blockIndex ushr 8).toByte()
        u1Input[salt.size + 3] = blockIndex.toByte()

        var u = hmacSha256(password, u1Input)
        val t = u.copyOf()
        for (iter in 1 until iterations) {
            u = hmacSha256(password, u)
            for (i in t.indices) {
                t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
            }
        }
        t.copyInto(out, destinationOffset = (blockIndex - 1) * 32)
    }
    return out.copyOf(keyLengthBytes)
}

private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val blockSize = 64
    val normalizedKey = if (key.size > blockSize) Sha256.digest(key) else key
    val kPad = ByteArray(blockSize)
    normalizedKey.copyInto(kPad)

    val iPad = ByteArray(blockSize + data.size)
    val oPad = ByteArray(blockSize + 32)
    for (i in 0 until blockSize) {
        val k = kPad[i].toInt() and 0xFF
        iPad[i] = (k xor 0x36).toByte()
        oPad[i] = (k xor 0x5C).toByte()
    }
    data.copyInto(iPad, destinationOffset = blockSize)
    val inner = Sha256.digest(iPad)
    inner.copyInto(oPad, destinationOffset = blockSize)
    return Sha256.digest(oPad)
}
