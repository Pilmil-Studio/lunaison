package fr.lunaison.core.data

import fr.lunaison.core.cycle.PeriodDetector
import fr.lunaison.core.model.DayLog
import kotlinx.datetime.LocalDate

/** Source d'un import qui passe par un aperçu et une confirmation avant toute écriture. */
enum class ImportSource {
    HEALTH_CONNECT,
    CLUE,
}

/**
 * Ce qu'un fichier ou une source de données contient, avant toute écriture en base.
 * [skippedCount] compte les éléments lus mais inutilisables (dates invalides par exemple).
 */
data class ImportPreview(
    val source: ImportSource,
    val dayLogs: List<DayLog>,
    val cyclesCount: Int,
    val skippedCount: Int = 0,
) {
    val daysCount: Int get() = dayLogs.size
    val firstDate: LocalDate? get() = dayLogs.minOfOrNull { it.date }
    val lastDate: LocalDate? get() = dayLogs.maxOfOrNull { it.date }

    companion object {
        /** Aperçu à partir de journées de flux : les cycles sont repérés par le même moteur que le reste de l'appli. */
        fun of(source: ImportSource, dayLogs: List<DayLog>, skippedCount: Int = 0): ImportPreview {
            val sorted = dayLogs.sortedBy { it.date }
            return ImportPreview(
                source = source,
                dayLogs = sorted,
                cyclesCount = PeriodDetector.detect(sorted).periods.size,
                skippedCount = skippedCount,
            )
        }
    }
}

/**
 * Résultat de la fusion prévue : [toWrite] est ce qui sera écrit, [keptExisting] le nombre de journées
 * déjà saisies dans Lunaison qui resteront telles quelles.
 */
data class ImportPlan(
    val preview: ImportPreview,
    val toWrite: List<DayLog>,
    val keptExisting: Int,
)

/**
 * Règle de fusion des imports avec aperçu : les saisies de Lunaison gagnent toujours.
 * Une journée importée n'est écrite que si Lunaison n'a rien noté ce jour-là.
 */
object ImportMerger {
    fun plan(preview: ImportPreview, existing: List<DayLog>): ImportPlan {
        val taken = existing.filter { !it.isEmpty }.map { it.date }.toSet()
        val (kept, fresh) = preview.dayLogs.partition { it.date in taken }
        return ImportPlan(preview = preview, toWrite = fresh, keptExisting = kept.size)
    }
}

/** Calcule la fusion sans rien écrire : sert à l'aperçu. */
suspend fun LunaisonRepository.planImport(preview: ImportPreview): ImportPlan =
    ImportMerger.plan(preview, logsOnce())

/**
 * Écrit la fusion confirmée. Le plan est recalculé au moment d'écrire, pour ne jamais écraser
 * une journée saisie entre l'aperçu et la confirmation. Renvoie le nombre de journées écrites.
 */
suspend fun LunaisonRepository.applyImport(plan: ImportPlan): Int {
    val fresh = ImportMerger.plan(plan.preview, logsOnce()).toWrite
    for (log in fresh) save(log)
    return fresh.size
}

/** Le fichier n'est pas un JSON lisible ou n'a pas la forme attendue. */
class UnreadableImportException(message: String = "Fichier illisible") : Exception(message)

/** Le fichier est lisible mais ne contient aucune journée de règles exploitable. */
class NothingToImportException(message: String = "Aucune donnée de règles trouvée") : Exception(message)

/**
 * Archive chiffrée avec une méthode que Lunaison ne gère pas (AES de WinZip).
 * Le texte affiché à la personne est localisé par l'interface, pas par ce message.
 */
class UnsupportedEncryptionException(
    message: String = "Format de chiffrement non pris en charge : décompressez l'archive puis importez le fichier JSON",
) : Exception(message)
