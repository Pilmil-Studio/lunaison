package fr.lunaison.ui

import fr.lunaison.core.cycle.Phase
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.minusDays
import fr.lunaison.core.model.of
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DiscoverGuideTest {

    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun contextualPickPrioritizesActiveSignal() {
        val pick = pickDiscoverGuide(
            today = today,
            recentStrongPain = true,
            irregularCyclesMode = true,
            todayPain = Pain.MODERATE,
            knownPhase = Phase.LUTEAL,
            articleLastShown = emptyMap(),
        )
        assertEquals("endometriose", pick.id)
        assertTrue(pick.isContextual)
    }

    @Test
    fun sameDayShownStaysStable() {
        val pick = pickDiscoverGuide(
            today = today,
            recentStrongPain = false,
            irregularCyclesMode = false,
            todayPain = Pain.NONE,
            knownPhase = Phase.LUTEAL,
            articleLastShown = mapOf("spm" to today),
        )
        assertEquals("spm", pick.id)
        assertTrue(pick.isContextual)
    }

    @Test
    fun recentlyShownContextualFicheIsSkippedDuringCooldown() {
        val pick = pickDiscoverGuide(
            today = today,
            recentStrongPain = false,
            irregularCyclesMode = false,
            todayPain = Pain.NONE,
            knownPhase = Phase.LUTEAL,
            articleLastShown = mapOf("spm" to today.minusDays(2)),
        )
        assertNotEquals("spm", pick.id)
        assertFalse(pick.isContextual)
    }

    @Test
    fun contextualFicheReturnsOnceCooldownExpires() {
        val lastShown = today.minusDays(7)
        val pick = pickDiscoverGuide(
            today = today,
            recentStrongPain = false,
            irregularCyclesMode = true,
            todayPain = Pain.NONE,
            knownPhase = null,
            articleLastShown = mapOf("perimenopause" to lastShown),
        )
        assertEquals("perimenopause", pick.id)
        assertTrue(pick.isContextual)
    }

    @Test
    fun allRecentlyShownFallsBackToOldest() {
        val history = mapOf(
            "cycle" to today.minusDays(1),
            "protections" to today.minusDays(2),
            "douleurs" to today.minusDays(3),
            "spm" to today.minusDays(4),
            "alimentation" to today.minusDays(6),
            "entourage" to today.minusDays(5),
        )
        val pick = pickDiscoverGuide(
            today = today,
            recentStrongPain = false,
            irregularCyclesMode = false,
            todayPain = Pain.NONE,
            knownPhase = null,
            articleLastShown = history,
        )
        assertEquals("alimentation", pick.id)
    }
}
