package fr.lunaison.core.data

import kotlin.test.Test
import kotlin.test.assertFalse

class AppSettingsDefaultsTest {
    @Test
    fun unknownAgeIsTreatedAsMinorAndFertilityWindowIsOff() {
        val settings = AppSettings()
        assertFalse(settings.isAdult)
        assertFalse(settings.showFertility)
    }
}
