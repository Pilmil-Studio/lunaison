package fr.lunaison.core.data

private val PILMIL_MAGIC = "PILMIL".encodeToByteArray()

actual fun previewAuto(bytes: ByteArray, password: String?): ImportPreview {
    // Les sauvegardes chiffrées .pilmil ne sont ni créées ni restaurées sur iOS pour l'instant.
    require(!(bytes.size >= PILMIL_MAGIC.size && PILMIL_MAGIC.indices.all { bytes[it] == PILMIL_MAGIC[it] })) {
        "Les sauvegardes .pilmil ne se restaurent pas encore sur iOS."
    }
    return previewLunaisonCsv(bytes.decodeToString())
}
