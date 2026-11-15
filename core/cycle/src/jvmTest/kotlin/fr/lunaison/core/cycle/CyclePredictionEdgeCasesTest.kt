package fr.lunaison.core.cycle

import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.daysTo
import fr.lunaison.core.model.of
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CyclePredictionEdgeCasesTest {

    private val d0 = LocalDate.of(2026, 1, 5)

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
    fun `AdaptiveCyclePredictor ignore cycles non plausibles et pondere un seul cycle`() {
        val (last, cycles) = cyclesFromLengths(d0, listOf(10, 32, 120))
        val pred = AdaptiveCyclePredictor.predict(last, cycles, usualCycleLength = 28)

        // Seul le cycle de 32j est plausible (entre 15 et 90)
        assertEquals(Confidence.LOW, pred.confidence)
        assertEquals(CycleConfig.PROVISIONAL_SPREAD, pred.spreadDays)
        assertTrue(pred.predictedCycleLength in 29..31)
    }

    @Test
    fun `AdaptiveCyclePredictor ignore ancrage luteal si ovulation trop precoce`() {
        val (last, cycles) = cyclesFromLengths(d0, listOf(28, 28, 28))
        val tooEarly = last.plusDays(3)
        val pred = AdaptiveCyclePredictor.predict(last, cycles, observedOvulationInCurrentCycle = tooEarly)

        assertFalse(pred.usedLutealAnchor)
        assertEquals(last.plusDays(28), pred.start)
    }

    @Test
    fun `AdaptiveCyclePredictor deux cycles donnent confiance LOW et marge d au moins 3 jours`() {
        val (last, cycles) = cyclesFromLengths(d0, listOf(28, 28))
        val pred = AdaptiveCyclePredictor.predict(last, cycles)

        assertEquals(Confidence.LOW, pred.confidence)
        assertTrue(pred.spreadDays >= 3)
    }

    @Test
    fun `pregnancyOdds couvre tous les niveaux autour de l ovulation et en retard`() {
        val baseline = Baseline(AgeGroup.ADULT, lastPeriodStart = d0, usualCycleLength = 28, usualPeriodLength = 5)
        // Jour 1 : en règles -> VERY_LOW
        val duringPeriod = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, d0.plusDays(1)))
        assertEquals(PregnancyOdds.VERY_LOW, duringPeriod.todayPregnancyOdds)

        // Cycle connu de 28j : nextStart = d0 + 28, ovulation = d0 + 15
        val midCycle = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, d0.plusDays(10)))
        val ovul = midCycle.ovulationDate
        assertEquals(d0.plusDays(15), ovul)

        assertEquals(PregnancyOdds.LOW, midCycle.pregnancyOdds(ovul.plusDays(-6)))
        assertEquals(PregnancyOdds.MEDIUM, midCycle.pregnancyOdds(ovul.plusDays(-4)))
        assertEquals(PregnancyOdds.HIGH, midCycle.pregnancyOdds(ovul.plusDays(-2)))
        assertEquals(PregnancyOdds.PEAK, midCycle.pregnancyOdds(ovul))
        assertEquals(PregnancyOdds.PEAK, midCycle.pregnancyOdds(ovul.plusDays(1)))
        assertEquals(PregnancyOdds.VERY_LOW, midCycle.pregnancyOdds(ovul.plusDays(3)))

        // En retard (au-delà de next.latest = d0 + 28 + 4 = d0 + 32)
        val late = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, d0.plusDays(35)))
        assertTrue(late.daysLate >= 5)
        assertTrue(late.possiblePregnancyHint)
        assertEquals(PregnancyOdds.LOW, late.todayPregnancyOdds)
    }

    @Test
    fun `upcomingDayForecasts et upcoming respectent la fenetre et le plafonnement de marge`() {
        val baseline = Baseline(AgeGroup.ADULT, lastPeriodStart = d0, usualCycleLength = 28, usualPeriodLength = 4)
        val state = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, d0.plusDays(26)))

        val forecasts = state.upcomingDayForecasts(5)
        assertEquals(5, forecasts.size)
        // J+2 = d0 + 28 = début des règles probables
        assertTrue(forecasts[1].inLikelyPeriod)
        assertEquals(Phase.MENSTRUAL, forecasts[1].phase)
        assertEquals(PregnancyOdds.VERY_LOW, forecasts[1].odds)

        val upcoming = state.upcoming(10)
        assertEquals(10, upcoming.size)
        val lastPred = upcoming.last()
        val halfSpan = lastPred.start.daysTo(lastPred.latest)
        assertEquals(CycleConfig.MAX_SPREAD + 4, halfSpan)
    }

    @Test
    fun `phaseInCycle retourne null hors cycle et utilise DEFAULT_PERIOD_LENGTH sans period`() {
        val cycle = Cycle(d0, d0.plusDays(28))
        assertNull(CycleAnalyzer.phaseInCycle(d0.plusDays(-1), cycle, null))
        assertNull(CycleAnalyzer.phaseInCycle(d0.plusDays(28), cycle, null))
        assertEquals(Phase.MENSTRUAL, CycleAnalyzer.phaseInCycle(d0.plusDays(4), cycle, null))
        assertEquals(Phase.FOLLICULAR, CycleAnalyzer.phaseInCycle(d0.plusDays(5), cycle, null))
    }

    @Test
    fun `median et standardDeviation gerent cas pairs et taille inferieure a 2`() {
        assertEquals(29, median(listOf(28, 29))) // (28+29)/2.0 = 28.5 -> 29
        assertEquals(0.0, standardDeviation(emptyList()))
        assertEquals(0.0, standardDeviation(listOf(28)))
        assertTrue(standardDeviation(listOf(26, 30)) > 2.8)
    }
}
