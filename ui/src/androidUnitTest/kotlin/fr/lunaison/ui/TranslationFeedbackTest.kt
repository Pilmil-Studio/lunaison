package fr.lunaison.ui

import fr.lunaison.core.model.Language
import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationFeedbackTest {

    @Test
    fun `l'encodage garde les caracteres non reserves et encode le reste en UTF-8`() {
        assertEquals("abc-XYZ_0.9~", TranslationFeedback.percentEncode("abc-XYZ_0.9~"))
        assertEquals("a%20b%26c%3Fd%3De", TranslationFeedback.percentEncode("a b&c?d=e"))
        assertEquals("%C5%81%C3%B3d%C5%BA", TranslationFeedback.percentEncode("Łódź"))
    }

    @Test
    fun `le lien ouvre un e-mail vers le studio, relisible et sans donnees`() {
        val link = TranslationFeedback.mailto("Lunaison", "Règles abondantes", "1.7.4", Language.PL)
        assertTrue(link.startsWith("mailto:pilmil.studio@proton.me?subject="))
        val query = link.substringAfter('?').split('&').associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8") }
        assertEquals("Lunaison 1.7.4, traduction / translation (pl, Polski)", query["subject"])
        val body = query.getValue("body")
        assertTrue("Règles abondantes" in body)
        // Trois rubriques à remplir, quelle que soit la langue : lieu, texte actuel, proposition.
        assertEquals(3, Regex(" :").findAll(body).count())
        assertFalse(link.contains(' '))
    }

    @Test
    fun `sans version, l'objet ne garde que le nom de l'appli`() {
        assertEquals("Lunaison, traduction / translation (uk, Українська)", TranslationFeedback.subject("Lunaison", null, Language.UK))
    }

    @Test
    fun `le bouton n'apparait pas en francais, langue d'origine`() {
        assertEquals(Language.current() != Language.FR, TranslationFeedback.available)
    }
}
