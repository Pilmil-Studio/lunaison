package fr.lunaison.core.data

import fr.lunaison.core.cycle.PeriodDetector
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.DayLog
import kotlinx.datetime.LocalDate

/**
 * Source d'un import de journées. Tous les imports passent par un aperçu ([ImportPreview]), une confirmation,
 * puis [applyImport] : aucun ne peut écrire directement ni écraser une journée déjà notée.
 */
enum class ImportSource {
    HEALTH_CONNECT,
    CLUE,
    PERIOD_CALENDAR,
    LUNAISON_CSV,
}

/**
 * Ce qu'un fichier ou une source de données contient, avant toute écriture en base.
 * [skippedCount] compte les éléments lus mais inutilisables (dates invalides, lignes illisibles ou vides).
 * [unknownValues] compte les valeurs non reconnues dans des journées par ailleurs lues (CSV).
 * [baseline] : repères de cycle trouvés dans la source ; ils ne remplissent que les champs encore vides,
 * et seulement à la confirmation ([applyImport]).
 */
data class ImportPreview(
    val source: ImportSource,
    val dayLogs: List<DayLog>,
    val cyclesCount: Int,
    val skippedCount: Int = 0,
    val unknownValues: Int = 0,
    val baseline: Baseline? = null,
) {
    val daysCount: Int get() = dayLogs.size
    val firstDate: LocalDate? get() = dayLogs.minOfOrNull { it.date }
    val lastDate: LocalDate? get() = dayLogs.maxOfOrNull { it.date }

    companion object {
        /** Aperçu à partir de journées de flux : les cycles sont repérés par le même moteur que le reste de l'appli. */
        fun of(
            source: ImportSource,
            dayLogs: List<DayLog>,
            skippedCount: Int = 0,
            unknownValues: Int = 0,
            baseline: Baseline? = null,
        ): ImportPreview {
            val sorted = dayLogs.sortedBy { it.date }
            return ImportPreview(
                source = source,
                dayLogs = sorted,
                cyclesCount = PeriodDetector.detect(sorted).periods.size,
                skippedCount = skippedCount,
                unknownValues = unknownValues,
                baseline = baseline,
            )
        }
    }
}

/**
 * Résultat de la fusion prévue : [toWrite] est ce qui sera écrit, [keptExisting] le nombre de journées
 * déjà saisies dans Lunaison qui resteront telles quelles, [fillsBaseline] indique que la source complète
 * des repères de cycle encore vides.
 */
data class ImportPlan(
    val preview: ImportPreview,
    val toWrite: List<DayLog>,
    val keptExisting: Int,
    val fillsBaseline: Boolean = false,
) {
    /** Vrai si la confirmation n'apporterait rien : aucune journée à ajouter et aucun repère à compléter. */
    val isEmpty: Boolean get() = toWrite.isEmpty() && !fillsBaseline
}

/** Complète [current] avec [imported], sans jamais remplacer un repère déjà renseigné. */
fun mergeBaseline(current: Baseline, imported: Baseline?): Baseline =
    if (imported == null) {
        current
    } else {
        current.copy(
            lastPeriodStart = current.lastPeriodStart ?: imported.lastPeriodStart,
            usualCycleLength = current.usualCycleLength ?: imported.usualCycleLength,
            usualPeriodLength = current.usualPeriodLength ?: imported.usualPeriodLength,
        )
    }

/**
 * Règle de fusion de tous les imports : les saisies de Lunaison gagnent toujours.
 * Une journée importée n'est écrite que si Lunaison n'a rien noté ce jour-là.
 */
object ImportMerger {
    fun plan(preview: ImportPreview, existing: List<DayLog>, currentBaseline: Baseline? = null): ImportPlan {
        val taken = existing.filter { !it.isEmpty }.map { it.date }.toSet()
        // Une journée vide n'apporte rien : l'écrire (donc la supprimer) n'a pas de sens.
        val (kept, fresh) = preview.dayLogs.filter { !it.isEmpty }.partition { it.date in taken }
        val fillsBaseline = currentBaseline != null && mergeBaseline(currentBaseline, preview.baseline) != currentBaseline
        return ImportPlan(preview = preview, toWrite = fresh, keptExisting = kept.size, fillsBaseline = fillsBaseline)
    }
}

/** Calcule la fusion sans rien écrire : sert à l'aperçu. */
suspend fun LunaisonRepository.planImport(preview: ImportPreview): ImportPlan =
    ImportMerger.plan(preview, logsOnce(), settingsOnce().baseline)

/**
 * Écrit la fusion confirmée. Le plan est recalculé au moment d'écrire, pour ne jamais écraser
 * une journée saisie entre l'aperçu et la confirmation. Les repères de cycle de la source ne remplissent
 * que les champs encore vides. Renvoie le nombre de journées écrites.
 */
suspend fun LunaisonRepository.applyImport(plan: ImportPlan): Int {
    val fresh = ImportMerger.plan(plan.preview, logsOnce()).toWrite
    for (log in fresh) save(log)
    val imported = plan.preview.baseline
    if (imported != null) {
        updateSettings { current -> current.copy(baseline = mergeBaseline(current.baseline, imported)) }
    }
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
