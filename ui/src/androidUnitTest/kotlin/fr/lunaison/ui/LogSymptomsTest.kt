package fr.lunaison.ui

import fr.lunaison.core.model.Pain
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
