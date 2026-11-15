package fr.lunaison.ui

import fr.lunaison.core.cycle.Confidence
import fr.lunaison.core.cycle.CycleAnalyzer
import fr.lunaison.core.cycle.CycleState
import fr.lunaison.core.cycle.Phase
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.minusDays
import fr.lunaison.core.model.of
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormatTest {

    private val original = Locale.getDefault()

    @AfterTest
    fun restore() = Locale.setDefault(original)

    // Semaine du lundi 2 au dimanche 8 novembre 2026.
    private val week = (2..8).map { LocalDate.of(2026, 11, it) }

    private fun initials(tag: String): String {
        Locale.setDefault(Locale.forLanguageTag(tag))
        return week.joinToString("") { Format.narrowWeekday(it) }
    }

    @Test
    fun weekdayInitialsFollowTheLanguage() {
        assertEquals("LMMJVSD", initials("fr"))
        assertEquals("MTWTFSS", initials("en"))
        assertEquals("LMXJVSD", initials("es"))
        assertEquals("MDMDFSS", initials("de"))
    }

    @Test
    fun datesAndDurationsFollowLocale() {
        val date = LocalDate.of(2026, 11, 2)
        Locale.setDefault(Locale.forLanguageTag("fr"))
        assertEquals("2 novembre", Format.dayMonth(date))
        assertEquals("Lundi", Format.dayOfWeek(date))
        assertEquals("Lundi 2 novembre", Format.weekday(date))
        assertEquals("Novembre 2026", Format.monthYear(date))
        assertEquals("1 jour", Format.days(1))
        assertEquals("3 jours", Format.days(3))
        assertEquals("Aujourd'hui", Format.relativeDay(date, date))
        assertEquals("Hier", Format.relativeDay(date.minusDays(1), date))
        assertEquals("Samedi 31 octobre", Format.relativeDay(date.minusDays(2), date))

        Locale.setDefault(Locale.forLanguageTag("en"))
        assertEquals("November 2", Format.dayMonth(date))
        assertEquals("Monday, November 2", Format.weekday(date))
        assertEquals("1 day", Format.days(1))
        assertEquals("2 days", Format.days(2))
        assertEquals("Today", Format.relativeDay(date, date))
        assertEquals("Yesterday", Format.relativeDay(date.minusDays(1), date))
    }

    @Test
    fun nextPeriodMessagesCoverAllStates() {
        Locale.setDefault(Locale.forLanguageTag("fr"))
        val start = LocalDate.of(2026, 5, 1)
        val baseline = Baseline(AgeGroup.ADULT, lastPeriodStart = start, usualCycleLength = 28, usualPeriodLength = 5)

        val inPeriod = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, start.plusDays(1)))
        assertEquals("Règles en cours, jour 2.", Format.nextPeriod(inPeriod))

        val tomorrow = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, start.plusDays(27)))
        assertEquals("Prochaines règles probablement demain.", Format.nextPeriod(tomorrow))

        val due = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, start.plusDays(28)))
        assertEquals("Vos règles peuvent arriver d'un jour à l'autre.", Format.nextPeriod(due))

        val late = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, start.plusDays(35)))
        assertTrue(Format.nextPeriod(late).contains("7 jours de retard"))

        val midCycle = assertIs<CycleState.Known>(CycleAnalyzer.analyze(emptyList(), baseline, start.plusDays(10)))
        assertTrue(Format.nextPeriod(midCycle).contains("18 jours"))
        assertTrue(Format.range(midCycle).contains("Plage probable"))
        assertTrue(Format.fertileWindow(midCycle).contains("Fenêtre fertile estimée"))
    }

    @Test
    fun phaseNamesAndConfidenceAreLocalized() {
        Locale.setDefault(Locale.forLanguageTag("en"))
        assertEquals("Period", Format.phaseName(Phase.MENSTRUAL))
        assertEquals("Follicular phase", Format.phaseName(Phase.FOLLICULAR))
        assertEquals("Around ovulation", Format.phaseName(Phase.OVULATION))
        assertEquals("Luteal phase", Format.phaseName(Phase.LUTEAL))
        assertEquals("Period late", Format.phaseName(Phase.LATE))
        assertTrue(Format.confidence(Confidence.PROVISIONAL).isNotBlank())
        assertTrue(Format.confidence(Confidence.LOW).isNotBlank())
        assertTrue(Format.confidence(Confidence.GOOD).isNotBlank())
    }
}
