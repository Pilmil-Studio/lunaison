package fr.lunaison.ui

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PluralsTest {
    private val initial = Locale.getDefault()

    @AfterTest
    fun restore() = Locale.setDefault(initial)

    private fun days(lang: String, n: Int): String {
        Locale.setDefault(Locale.forLanguageTag(lang))
        return Format.days(n)
    }

    @Test
    fun frenchZeroAndOneAreSingular() {
        assertEquals("0 jour", days("fr", 0))
        assertEquals("1 jour", days("fr", 1))
        assertEquals("2 jours", days("fr", 2))
        assertEquals("1 cycle", Format.cyclesFr(1))
        assertEquals("3 cycles", Format.cyclesFr(3))
    }

    @Test
    fun otherLanguagesPluralizeZero() {
        assertEquals("0 days", days("en", 0))
        assertEquals("1 day", days("en", 1))
        assertEquals("0 Tage", days("de", 0))
        assertEquals("1 Tag", days("de", 1))
        assertEquals("0 días", days("es", 0))
        assertEquals("1 día", days("es", 1))
        assertEquals("0 dni", days("pl", 0))
        assertEquals("1 dzień", days("pl", 1))
        assertEquals("5 dni", days("pl", 5))
    }

    @Test
    fun ukrainianFollowsSlavicRules() {
        assertEquals("1 день", days("uk", 1))
        assertEquals("2 дні", days("uk", 2))
        assertEquals("5 днів", days("uk", 5))
        assertEquals("11 днів", days("uk", 11))
        assertEquals("12 днів", days("uk", 12))
        assertEquals("21 день", days("uk", 21))
        assertEquals("0 днів", days("uk", 0))
    }
}
