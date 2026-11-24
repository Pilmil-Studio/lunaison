package fr.lunaison.core.content

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RightsSheetsTest {

    @Test
    fun `chaque fiche de droits a des sources officielles en https`() {
        RightsSheets.all.forEach { sheet ->
            assertTrue(sheet.article.sources.isNotEmpty(), sheet.article.id)
            sheet.article.sources.forEach { assertTrue(it.url.startsWith("https://"), it.url) }
        }
    }

    @Test
    fun `aucune fiche de droits n'a plus d'un an sans verification`() {
        // Ce test échoue volontairement quand une fiche n'a pas été revérifiée : les droits changent.
        val today = LocalDate.now()
        RightsSheets.all.forEach { sheet ->
            val verified = LocalDate.of(sheet.verifiedOn.year, sheet.verifiedOn.monthNumber, sheet.verifiedOn.dayOfMonth)
            val age = ChronoUnit.DAYS.between(verified, today)
            assertTrue(age <= RightsSheets.REVIEW_MAX_DAYS, "${sheet.article.id} vérifiée il y a $age jours : à revérifier")
        }
    }

    @Test
    fun `les fiches sont rattachees a un pays et distinctes des fiches Comprendre`() {
        assertTrue(RightsSheets.forCountry("FR").size >= 2)
        assertTrue(RightsSheets.forCountry("DE").isEmpty())
        assertTrue(RightsSheets.forCountry(null).isEmpty())
        RightsSheets.all.forEach { sheet ->
            assertTrue(sheet.article.id.startsWith(RightsSheets.ID_PREFIX))
            assertTrue(Articles.forLanguage(fr.lunaison.core.model.Language.FR).none { it.id == sheet.article.id })
            assertNotNull(RightsSheets.byId(sheet.article.id))
        }
    }

    @Test
    fun `les fiches existent dans les six langues`() {
        val saved = Locale.getDefault()
        try {
            val titles = listOf("fr", "en", "pl", "uk", "es", "de").map { lang ->
                Locale.setDefault(Locale(lang))
                RightsSheets.all.map { it.article.title + it.article.sections.joinToString { s -> s.paragraphs.joinToString() } }
            }
            assertEquals(6, titles.distinct().size, "chaque langue doit avoir ses propres textes")
        } finally {
            Locale.setDefault(saved)
        }
    }
}
