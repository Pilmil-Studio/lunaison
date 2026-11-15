package fr.lunaison.core.cycle

import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveCyclePredictorTest {

    private fun cyclesFromLengths(start: LocalDate, lengths: List<Int>): Pair<LocalDate, List<Cycle>> {
        var cur = start
        val out = mutableListOf<Cycle>()
        for (len in lengths) {
            val next = cur.plusDays(len.toLong())
            out += Cycle(cur, next)
            cur = next
        }
        return cur to out
    }

    @Test
    fun `sans historique utilise l a priori doux`() {
        val last = LocalDate(2026, 5, 1)
        val pred = AdaptiveCyclePredictor.predict(last, emptyList(), usualCycleLength = 30)

        assertEquals(30, pred.predictedCycleLength)
        assertEquals(LocalDate(2026, 5, 31), pred.start)
        assertEquals(Confidence.PROVISIONAL, pred.confidence)
        assertEquals(CycleConfig.PROVISIONAL_SPREAD, pred.spreadDays)
        assertFalse(pred.usedLutealAnchor)
    }

    @Test
    fun `un cycle atypique isole ne fausse pas la prediction sur historique regulier`() {
        val (last, cycles) = cyclesFromLengths(
            LocalDate(2026, 1, 1),
            listOf(28, 29, 28, 45, 28, 29),
        )
        val pred = AdaptiveCyclePredictor.predict(last, cycles)

        assertEquals(1, pred.outlierCyclesCount)
        assertTrue(pred.predictedCycleLength in 28..30, "attendu ~29j malgré le cycle de 45j, obtenu ${pred.predictedCycleLength}")
        assertEquals(Confidence.GOOD, pred.confidence)
    }

    @Test
    fun `ancrage luteal resserre la fenetre autour de l ovulation observee`() {
        val (last, cycles) = cyclesFromLengths(
            LocalDate(2026, 1, 1),
            listOf(30, 34, 28, 35),
        )
        val ovulation = last.plusDays(19)
        val pred = AdaptiveCyclePredictor.predict(
            lastStart = last,
            completedCycles = cycles,
            observedOvulationInCurrentCycle = ovulation,
        )

        assertTrue(pred.usedLutealAnchor)
        assertEquals(ovulation.plusDays(CycleConfig.LUTEAL_DAYS.toLong()), pred.start)
        assertEquals(CycleConfig.MIN_SPREAD, pred.spreadDays)
    }
}
