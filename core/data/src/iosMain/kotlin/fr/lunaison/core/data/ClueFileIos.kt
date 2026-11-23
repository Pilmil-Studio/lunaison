package fr.lunaison.core.data

/** Sur iOS, seuls les fichiers JSON (`.json`, `.cluedata`) sont lus : une archive ZIP est à décompresser d'abord. */
actual fun previewClueFile(bytes: ByteArray, password: String?): ImportPreview {
    val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
    if (isZip) throw UnsupportedEncryptionException()
    return ClueImporter.toPreview(listOf(bytes.decodeToString()))
}
