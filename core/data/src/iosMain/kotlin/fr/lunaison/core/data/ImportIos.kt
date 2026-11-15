package fr.lunaison.core.data

private val PILMIL_MAGIC = "PILMIL".encodeToByteArray()

actual suspend fun LunaisonRepository.importAuto(bytes: ByteArray, password: String?): ImportSummary {
    // Les sauvegardes chiffrées .pilmil ne sont ni créées ni restaurées sur iOS pour l'instant.
    require(!(bytes.size >= PILMIL_MAGIC.size && PILMIL_MAGIC.indices.all { bytes[it] == PILMIL_MAGIC[it] })) {
        "Les sauvegardes .pilmil ne se restaurent pas encore sur iOS."
    }
    val report = importCsvReport(bytes.decodeToString())
    return ImportSummary(daysCount = report.imported, cyclesCount = 0, skippedCount = report.skipped)
}
