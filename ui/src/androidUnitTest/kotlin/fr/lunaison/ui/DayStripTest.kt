package fr.lunaison.ui

import fr.lunaison.core.model.of
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DayStripTest {
    private val initial = Locale.getDefault()

    @AfterTest
    fun restore() = Locale.setDefault(initial)

    @Test
    fun descriptionSaysPeriodAndLoggedDay() {
        Locale.setDefault(Locale.forLanguageTag("fr"))
        val date = LocalDate.of(2026, 11, 17)
        assertEquals("Mardi 17 novembre, règles, journée notée", dayDescription(date, bleeding = true, logged = true))
        assertEquals("Mardi 17 novembre, journée notée", dayDescription(date, bleeding = false, logged = true))
        assertEquals("Mardi 17 novembre", dayDescription(date, bleeding = false, logged = false))
    }

    @Test
    fun forecastCardHasOneDescription() {
        Locale.setDefault(Locale.forLanguageTag("fr"))
        val date = LocalDate.of(2026, 11, 17)
        assertEquals("Mardi 17 novembre, jour 12, règles probables", forecastDescription(date, 12, true, "Phase lutéale", ""))
        assertEquals("Mardi 17 novembre, jour 12, Phase lutéale, Faibles", forecastDescription(date, 12, false, "Phase lutéale", "Faibles"))
    }
}
