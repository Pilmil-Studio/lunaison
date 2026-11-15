package fr.lunaison.core.data

import fr.pilmil.core.platform.backup.IosBackupVault

private val PILMIL_MAGIC = "PILMIL".encodeToByteArray()

actual suspend fun LunaisonRepository.importAuto(bytes: ByteArray, password: String?): ImportSummary {
    val isPilmil = bytes.size >= PILMIL_MAGIC.size &&
        PILMIL_MAGIC.indices.all { bytes[it] == PILMIL_MAGIC[it] }

    val csvText = if (isPilmil) {
        val pwd = password?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Mot de passe requis pour déchiffrer la sauvegarde.")
        IosBackupVault().decrypt(bytes, pwd.toCharArray())
    } else {
        bytes.decodeToString()
    }
    val report = importCsvReport(csvText)
    return ImportSummary(daysCount = report.imported, cyclesCount = 0, skippedCount = report.skipped)
}
