package fr.lunaison.core.data

/** Entrées d'une archive Clue qui peuvent contenir les données : le JSON, ou l'ancien `.cluedata`. */
private val CLUE_ENTRY_SUFFIXES = listOf(".json", ".cluedata")

actual fun previewClueFile(bytes: ByteArray, password: String?): ImportPreview {
    if (bytes.isEmpty()) throw NothingToImportException()
    if (bytes.size.toLong() > PeriodCalendarImporter.MAX_FILE_BYTES) throw ImportTooLargeException()
    val texts = if (PeriodCalendarImporter.isZip(bytes)) {
        // extractZip refuse l'AES (UnsupportedEncryptionException) et réclame le mot de passe d'un ZipCrypto.
        PeriodCalendarImporter.extractZip(bytes, password)
            .filterKeys { name -> CLUE_ENTRY_SUFFIXES.any { name.lowercase().endsWith(it) } }
            .values.toList()
    } else {
        listOf(String(bytes, Charsets.UTF_8))
    }
    return ClueImporter.toPreview(texts)
}
