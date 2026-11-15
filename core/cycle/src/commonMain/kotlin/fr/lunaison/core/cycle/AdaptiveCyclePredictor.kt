package fr.lunaison.core.cycle

import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Résultat détaillé de la prédiction adaptative locale (bayésienne hiérarchique).
 * Tous les calculs restent en mémoire vive dans le coffre chiffré de l'utilisatrice.
 */
data class AdaptiveCyclePrediction(
    val predictedCycleLength: Int,
    val start: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
    val spreadDays: Int,
    val personalMean: Double,
    val personalStdDev: Double,
    val confidence: Confidence,
    val outlierCyclesCount: Int,
    val usedLutealAnchor: Boolean,
)

/**
 * Modèle bayésien hiérarchique léger pour la prévision locale des cycles.
 *
 * - A priori populational doux (`priorMean`, `priorWeight = 1.5`) : évite les prédictions
 *   aberrantes lorsque seuls 1 ou 2 cycles sont enregistrés.
 * - Décroissance exponentielle (`RECENCY_DECAY = 0.82`) : les cycles récents pèsent davantage.
 * - Détection robuste des cycles atypiques (> 2,5 écarts à la médiane via MAD) : pondérés à
 *   la baisse (`OUTLIER_WEIGHT = 0.2`) pour qu'un cycle exceptionnellement long/court ne fausse
 *   pas les 3 mois suivants.
 * - Ancrage sur la phase lutéale : lorsqu'une date d'ovulation est observée sur le cycle en
 *   cours, la fenêtre probable se resserre autour de `ovulationDate + lutealDays`.
 */
object AdaptiveCyclePredictor {

    private const val PRIOR_WEIGHT = 1.5
    private const val RECENCY_DECAY = 0.82
    private const val OUTLIER_Z_THRESHOLD = 2.5
    private const val OUTLIER_WEIGHT = 0.2
    private const val MIN_ROBUST_SCALE = 2.5

    fun predict(
        lastStart: LocalDate,
        completedCycles: List<Cycle>,
        usualCycleLength: Int? = null,
        observedOvulationInCurrentCycle: LocalDate? = null,
        lutealDays: Int = CycleConfig.LUTEAL_DAYS,
    ): AdaptiveCyclePrediction {
        val plausible = completedCycles
            .filter { it.isPlausible }
            .takeLast(12)
            .map { it.length.toDouble() }

        val priorMean = usualCycleLength
            ?.takeIf { it in CycleConfig.MIN_PLAUSIBLE_CYCLE..CycleConfig.MAX_PLAUSIBLE_CYCLE }
            ?.toDouble()
            ?: CycleConfig.DEFAULT_CYCLE_LENGTH.toDouble()

        if (plausible.isEmpty()) {
            val len = priorMean.roundToInt()
            val spread = CycleConfig.PROVISIONAL_SPREAD
            val anchorStart = observedOvulationInCurrentCycle?.plusDays(lutealDays.toLong())
            val start = anchorStart ?: lastStart.plusDays(len.toLong())
            val effectiveSpread = if (anchorStart != null) CycleConfig.MIN_SPREAD else spread
            return AdaptiveCyclePrediction(
                predictedCycleLength = len,
                start = start,
                earliest = start.plusDays((-effectiveSpread).toLong()),
                latest = start.plusDays(effectiveSpread.toLong()),
                spreadDays = effectiveSpread,
                personalMean = priorMean,
                personalStdDev = effectiveSpread.toDouble(),
                confidence = Confidence.PROVISIONAL,
                outlierCyclesCount = 0,
                usedLutealAnchor = anchorStart != null,
            )
        }

        val med = medianDouble(plausible)
        val mad = medianDouble(plausible.map { abs(it - med) }) * 1.4826
        val robustScale = max(mad, MIN_ROBUST_SCALE)

        var outlierCount = 0
        val n = plausible.size
        val weights = DoubleArray(n) { idx ->
            val age = (n - 1) - idx
            val recencyW = RECENCY_DECAY.pow(age)
            val isOutlier = n >= 4 && abs(plausible[idx] - med) > OUTLIER_Z_THRESHOLD * robustScale
            if (isOutlier) {
                outlierCount++
                recencyW * OUTLIER_WEIGHT
            } else {
                recencyW
            }
        }

        val totalDataWeight = weights.sum()
        val weightedSum = plausible.indices.sumOf { plausible[it] * weights[it] }
        val posteriorMean = (PRIOR_WEIGHT * priorMean + weightedSum) / (PRIOR_WEIGHT + totalDataWeight)

        val variance = if (n >= 2) {
            val num = plausible.indices.sumOf { weights[it] * (plausible[it] - posteriorMean).pow(2) }
            (num + PRIOR_WEIGHT * 2.6 * 2.6) / (totalDataWeight + PRIOR_WEIGHT)
        } else {
            CycleConfig.PROVISIONAL_SPREAD.toDouble().pow(2)
        }
        val stdDev = sqrt(variance)

        val confidence = when {
            n >= 3 -> Confidence.GOOD
            else -> Confidence.LOW
        }

        val predictedLen = posteriorMean.roundToInt()
            .coerceIn(CycleConfig.MIN_PLAUSIBLE_CYCLE, CycleConfig.MAX_PLAUSIBLE_CYCLE)

        val usedLuteal = observedOvulationInCurrentCycle != null &&
            !observedOvulationInCurrentCycle.let { daysBetween(lastStart, it) < 5 }

        val start = if (usedLuteal) {
            observedOvulationInCurrentCycle!!.plusDays(lutealDays.toLong())
        } else {
            lastStart.plusDays(predictedLen.toLong())
        }

        val spread = if (usedLuteal) {
            CycleConfig.MIN_SPREAD
        } else {
            when {
                n >= 3 -> ceil(1.28 * stdDev).toInt().coerceIn(CycleConfig.MIN_SPREAD, CycleConfig.MAX_SPREAD)
                n == 2 -> ceil(1.28 * stdDev).toInt().coerceIn(3, CycleConfig.MAX_SPREAD)
                else -> CycleConfig.PROVISIONAL_SPREAD
            }
        }

        return AdaptiveCyclePrediction(
            predictedCycleLength = predictedLen,
            start = start,
            earliest = start.plusDays((-spread).toLong()),
            latest = start.plusDays(spread.toLong()),
            spreadDays = spread,
            personalMean = posteriorMean,
            personalStdDev = stdDev,
            confidence = confidence,
            outlierCyclesCount = outlierCount,
            usedLutealAnchor = usedLuteal,
        )
    }

    private fun medianDouble(values: List<Double>): Double {
        val sorted = values.sorted()
        val m = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[m] else (sorted[m - 1] + sorted[m]) / 2.0
    }
}
