package fr.lunaison.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class UnlockTest {
    private val real = AppSettings.hashPin("2580")
    private val decoy = AppSettings.hashPin("1357")

    @Test
    fun le_vrai_code_ouvre_la_vraie_base() {
        assertEquals(UnlockResult.REAL, AppSettings.checkUnlock("2580", real, decoy))
        assertEquals(UnlockResult.REAL, AppSettings.checkUnlock("2580", real, null))
    }

    @Test
    fun le_code_leurre_ouvre_le_leurre() {
        assertEquals(UnlockResult.DECOY, AppSettings.checkUnlock("1357", real, decoy))
    }

    @Test
    fun sans_leurre_defini_le_code_leurre_est_un_mauvais_code() {
        assertEquals(UnlockResult.WRONG, AppSettings.checkUnlock("1357", real, null))
        assertEquals(UnlockResult.WRONG, AppSettings.checkUnlock("0000", real, decoy))
    }

    @Test
    fun le_vrai_code_l_emporte_si_les_deux_empreintes_viennent_du_meme_code() {
        // Ne devrait pas arriver (le réglage refuse un leurre identique), mais la vraie base doit gagner.
        assertEquals(UnlockResult.REAL, AppSettings.checkUnlock("2580", real, AppSettings.hashPin("2580")))
    }
}
