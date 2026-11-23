package fr.lunaison.ui

import fr.lunaison.ui.CyclePhysiology as P
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CyclePhysiologyTest {

    private val days = (0..270).map { 1f + it / 10f }

    @Test
    fun `toutes les valeurs restent entre 0 et 1`() {
        val curves = listOf(P::estradiol, P::progesterone, P::lh, P::endometrium, P::shedding, P::secretory, P::dominantFollicle, P::corpusLuteum, P::fertileMucus, P::cervixOpening)
        days.forEach { d -> curves.forEach { f -> assertTrue(f(d) in 0f..1f, "${f.name}($d) = ${f(d)}") } }
    }

    @Test
    fun `les pics tombent aux bons moments`() {
        assertEquals(14f, days.maxBy { P.lh(it) }, 0.15f)
        assertTrue(days.maxBy { P.estradiol(it) } in 12f..14f)
        assertTrue(days.maxBy { P.progesterone(it) } in 20f..23f)
        assertTrue(P.progesterone(10f) < 0.1f)
    }

    @Test
    fun `l'endometre s'amincit pendant les regles puis s'epaissit`() {
        assertTrue(P.endometrium(1f) > P.endometrium(5f))
        assertTrue(P.endometrium(5f) < P.endometrium(10f))
        assertTrue(P.endometrium(10f) < P.endometrium(14f))
        assertTrue(P.endometrium(20f) >= P.endometrium(14f))
        assertTrue(P.endometrium(5f) < 0.25f)
    }

    @Test
    fun `l'evolution est continue d'un dixieme de jour a l'autre`() {
        // Pas de saut : c'est ce qui permet d'animer le schéma en glissant (l'éclat de l'ovulation excepté).
        listOf(P::estradiol, P::progesterone, P::endometrium, P::secretory, P::fertileMucus).forEach { f ->
            days.zipWithNext().forEach { (a, b) -> assertTrue(kotlin.math.abs(f(b) - f(a)) < 0.12f, "${f.name} saute entre $a et $b") }
        }
    }

    @Test
    fun `follicule puis corps jaune, jamais les deux`() {
        days.forEach { d -> assertTrue(P.dominantFollicle(d) == 0f || P.corpusLuteum(d) == 0f, "jour $d") }
        assertTrue(P.dominantFollicle(13f) > 0.8f)
        assertTrue(P.corpusLuteum(20f) > 0.8f)
        assertTrue(P.corpusLuteum(28f) < P.corpusLuteum(21f))
    }

    @Test
    fun `l'ovule ne voyage que juste apres l'ovulation`() {
        assertNull(P.eggProgress(10f))
        assertNull(P.eggProgress(20f))
        val start = assertNotNull(P.eggProgress(14f))
        val later = assertNotNull(P.eggProgress(16f))
        assertTrue(later > start)
    }

    @Test
    fun `glaire fertile et regles ouvrent le col`() {
        assertTrue(P.fertileMucus(13.5f) > 0.8f)
        assertTrue(P.fertileMucus(20f) < 0.05f)
        assertTrue(P.cervixOpening(2f) > P.cervixOpening(20f))
        assertTrue(P.cervixOpening(13.5f) > P.cervixOpening(20f))
    }
}
