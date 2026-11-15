package fr.lunaison.ui

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.Mood
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.Symptom
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeAndLogLogicTest {

    private val today = LocalDate(2026, 3, 15)
    private val yesterday = LocalDate(2026, 3, 14)

    @Test
    fun `shouldShowReturnWelcomeCard vrai seulement après pause de 10 jours et jour vide`() {
        val emptyToday = DayLog(date = today)
        assertTrue(shouldShowReturnWelcomeCard(paused = false, todayLog = emptyToday, daysSinceLastLog = 10))
        assertTrue(shouldShowReturnWelcomeCard(paused = false, todayLog = emptyToday, daysSinceLastLog = 20))
        assertFalse(shouldShowReturnWelcomeCard(paused = false, todayLog = emptyToday, daysSinceLastLog = 9))
        assertFalse(shouldShowReturnWelcomeCard(paused = false, todayLog = emptyToday, daysSinceLastLog = null))
        assertFalse(shouldShowReturnWelcomeCard(paused = true, todayLog = emptyToday, daysSinceLastLog = 7))
        assertFalse(
            shouldShowReturnWelcomeCard(
                paused = false,
                todayLog = emptyToday.copy(flow = Flow.LIGHT),
                daysSinceLastLog = 7,
            ),
        )
    }

    @Test
    fun `shouldShowDifficultDayCard détecte douleur forte`() {
        assertFalse(shouldShowDifficultDayCard(DayLog(date = today)))
        assertFalse(shouldShowDifficultDayCard(DayLog(date = today, pain = Pain.MODERATE, moods = setOf(Mood.CALM))))
        assertFalse(shouldShowDifficultDayCard(DayLog(date = today, pain = Pain.MILD, moods = setOf(Mood.ANXIOUS))))
        assertTrue(shouldShowDifficultDayCard(DayLog(date = today, pain = Pain.SEVERE)))
    }

    @Test
    fun `canCopyYesterdayLog exige une veille renseignée et un brouillon vide`() {
        val emptyYesterday = DayLog(date = yesterday)
        val filledYesterday = DayLog(date = yesterday, flow = Flow.MEDIUM)
        val emptyDraft = DayLog(date = today)
        val nonEmptyDraft = DayLog(date = today, note = "Déjà commencé")

        assertTrue(canCopyYesterdayLog(yesterdayLog = filledYesterday, draft = emptyDraft))
        assertFalse(canCopyYesterdayLog(yesterdayLog = emptyYesterday, draft = emptyDraft))
        assertFalse(canCopyYesterdayLog(yesterdayLog = filledYesterday, draft = nonEmptyDraft))
    }

    @Test
    fun `copyFromYesterday copie les mesures mais préserve date et note du brouillon`() {
        val yesterdayLog = DayLog(
            date = yesterday,
            flow = Flow.HEAVY,
            pain = Pain.MODERATE,
            symptoms = setOf(Symptom.CRAMPS),
            moods = setOf(Mood.LOW_ENERGY),
            note = "Note d'hier à ne pas copier",
            medicationTaken = true,
            weight = 61.5f,
            basalTemperature = 36.7f,
        )
        val draft = DayLog(date = today, note = "Note du jour")

        val copied = copyFromYesterday(draft = draft, yesterdayLog = yesterdayLog)

        assertEquals(today, copied.date)
        assertEquals("Note du jour", copied.note)
        assertEquals(Flow.HEAVY, copied.flow)
        assertEquals(Pain.MODERATE, copied.pain)
        assertEquals(setOf(Symptom.CRAMPS), copied.symptoms)
        assertEquals(setOf(Mood.LOW_ENERGY), copied.moods)
        assertTrue(copied.medicationTaken)
        assertEquals(61.5f, copied.weight)
        assertEquals(36.7f, copied.basalTemperature)
    }
}
