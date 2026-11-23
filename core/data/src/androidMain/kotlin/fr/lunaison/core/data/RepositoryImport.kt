package fr.lunaison.core.data

/*
 * Lecture des sauvegardes d'autres applis (archive Zip, JSON, CSV). Elle s'appuie sur le décompresseur de la JVM :
 * elle reste donc propre à Android, l'application d'origine (Period Calendar) n'existant pas sur iOS.
 *
 * Aucune de ces fonctions n'écrit : elles produisent un [ImportPreview]. L'écriture passe par [planImport] puis
 * [applyImport], qui conservent toujours les journées déjà notées.
 */

private fun PeriodCalendarImportResult.toPreview(): ImportPreview =
    ImportPreview.of(
        source = ImportSource.PERIOD_CALENDAR,
        dayLogs = dayLogs.filter { !it.isEmpty },
        baseline = baseline,
    ).copy(cyclesCount = cyclesCount)

/** Aperçu d'une archive Zip du Calendrier des règles (Period Calendar), avec mot de passe optionnel. */
fun previewPeriodCalendarZip(bytes: ByteArray, password: String? = null): ImportPreview =
    PeriodCalendarImporter.importFromZip(bytes, password).toPreview()

/** Aperçu d'un export JSON brut du Calendrier des règles (Period Calendar). */
fun previewPeriodCalendarJson(jsonContent: String): ImportPreview =
    PeriodCalendarImporter.importFromTexts(dayEntriesJson = jsonContent).toPreview()

/** Reconnaît le format (Zip Calendrier, JSON Calendrier ou CSV Lunaison) et en fait un aperçu, sans rien écrire. */
actual fun previewAuto(bytes: ByteArray, password: String?): ImportPreview {
    if (PeriodCalendarImporter.isZip(bytes)) return previewPeriodCalendarZip(bytes, password)
    val text = String(bytes, Charsets.UTF_8).trim()
    if (text.startsWith("{") || text.startsWith("[")) return previewPeriodCalendarJson(text)
    return previewLunaisonCsv(text)
}
