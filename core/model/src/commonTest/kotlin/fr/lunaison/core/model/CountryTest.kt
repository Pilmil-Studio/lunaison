package fr.lunaison.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CountryTest {
    @Test
    fun suit_la_region_du_telephone_par_defaut() {
        assertEquals("FR", Country.resolve(Country.AUTO, systemRegion = "fr"))
        assertEquals("BE", Country.resolve(Country.AUTO, systemRegion = "BE"))
    }

    @Test
    fun region_inconnue_ou_autre_pays_donne_null() {
        assertNull(Country.resolve(Country.AUTO, systemRegion = ""))
        assertNull(Country.resolve(Country.AUTO, systemRegion = "419"))
        assertNull(Country.resolve(Country.OTHER, systemRegion = "FR"))
    }

    @Test
    fun un_choix_explicite_l_emporte_sur_le_telephone() {
        assertEquals("FR", Country.resolve("FR", systemRegion = "DE"))
    }
}
