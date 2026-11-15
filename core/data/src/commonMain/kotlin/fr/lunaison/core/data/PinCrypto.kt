package fr.lunaison.core.data

internal expect fun randomPinSalt(size: Int = 16): ByteArray

internal expect fun pbkdf2HmacSha256(
    pin: String,
    salt: ByteArray,
    iterations: Int,
    keyLengthBytes: Int,
): ByteArray
