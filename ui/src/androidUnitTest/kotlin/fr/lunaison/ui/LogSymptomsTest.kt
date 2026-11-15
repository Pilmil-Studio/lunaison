package fr.lunaison.ui

import fr.lunaison.core.model.Pain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogSymptomsTest {
    @Test
    fun bothersomeOrSeverePainOpensMoreSymptoms() {
        assertTrue(painOpensMoreSymptoms(Pain.MODERATE))
        assertTrue(painOpensMoreSymptoms(Pain.SEVERE))
        assertFalse(painOpensMoreSymptoms(Pain.MILD))
        assertFalse(painOpensMoreSymptoms(Pain.NONE))
        assertFalse(painOpensMoreSymptoms(null))
    }

    @Test
    fun decimalFieldAcceptsCommaAndRejectsOutOfRange() {
        assertEquals(36.6f, parseDecimalInRange("36,6", 35.5f, 38.5f))
        assertEquals(60f, parseDecimalInRange(" 60.0 ", 30f, 250f))
        assertNull(parseDecimalInRange("20", 30f, 250f))
        assertNull(parseDecimalInRange("abc", 30f, 250f))
        assertNull(parseDecimalInRange("", 30f, 250f))
    }
}
