package fr.lunaison.ui

import fr.lunaison.core.data.ImportPlan
import fr.lunaison.core.data.ImportPreview
import fr.lunaison.core.data.ImportSource
import fr.lunaison.core.data.NothingToImportException
import fr.lunaison.core.data.UnreadableImportException
import fr.lunaison.core.data.UnsupportedEncryptionException
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.of
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportPreviewTextTest {
    private val initial = Locale.getDefault()
    private val languages = listOf("fr", "en", "de", "es", "pl", "uk")

    @AfterTest
    fun restore() = Locale.setDefault(initial)

    private fun plan(): ImportPlan {
        val logs = listOf(DayLog(LocalDate.of(2024, 3, 5), flow = Flow.HEAVY), DayLog(LocalDate.of(2024, 3, 6), flow = Flow.LIGHT))
        val preview = ImportPreview.of(ImportSource.CLUE, logs, skippedCount = 2)
        return ImportPlan(preview, toWrite = logs.take(1), keptExisting = 1)
    }

    private fun csvPlan(): ImportPlan {
        val logs = listOf(DayLog(LocalDate.of(2024, 3, 5), flow = Flow.HEAVY), DayLog(LocalDate.of(2024, 3, 6), note = "RAS"))
        val preview = ImportPreview.of(ImportSource.LUNAISON_CSV, logs, skippedCount = 3, baseline = Baseline(AgeGroup.ADULT, usualCycleLength = 28))
        return ImportPlan(preview, toWrite = logs, keptExisting = 0, fillsBaseline = true)
    }

    @Test
    fun `chaque source d'import a un nom dans chaque langue et l'apercu CSV parle de journees et de lignes`() {
        for (lang in languages) {
            Locale.setDefault(Locale.forLanguageTag(lang))
            assertTrue(ImportSource.entries.all { importSourceName(it).isNotBlank() }, lang)
            val lines = importPlanLines(csvPlan())
            // jours, cycles, période, jours ajoutés, repères de cycle, lignes ignorées
            assertEquals(6, lines.size, "$lang : $lines")
            assertTrue(lines.last().endsWith("3"), "$lang : ${lines.last()}")
            assertTrue(importNothingFoundMessage(csvPlan().preview).isNotBlank(), lang)
        }
    }

    @Test
    fun `l'apercu annonce jours, cycles, periode, journees conservees et elements ignores dans chaque langue`() {
        for (lang in languages) {
            Locale.setDefault(Locale.forLanguageTag(lang))
            val lines = importPlanLines(plan())
            assertEquals(6, lines.size, lang)
            assertTrue(lines.all { it.isNotBlank() }, lang)
            assertTrue(lines[0].endsWith("2"), "$lang : ${lines[0]}")
            assertTrue(lines[3].endsWith("1"), "$lang : ${lines[3]}")
        }
    }

    @Test
    fun `aucun tiret cadratin dans les textes de l'import`() {
        for (lang in languages) {
            Locale.setDefault(Locale.forLanguageTag(lang))
            val texts = importPlanLines(plan()) +
                importAppliedMessage(3, 1) +
                ImportSource.entries.map { importSourceName(it) } +
                importNothingFoundMessage(csvPlan().preview) +
                importPlanLines(csvPlan()) +
                listOf(UnsupportedEncryptionException(), UnreadableImportException(), NothingToImportException()).map { importPreviewErrorMessage(it).orEmpty() }
            assertTrue(texts.none { '\u2014' in it }, lang)
        }
    }

    @Test
    fun `l'erreur de chiffrement est localisee et les autres erreurs ne sont pas captees`() {
        Locale.setDefault(Locale.forLanguageTag("fr"))
        val message = importPreviewErrorMessage(UnsupportedEncryptionException()).orEmpty()
        assertTrue("décompressez l'archive" in message && "JSON" in message, message)
        Locale.setDefault(Locale.forLanguageTag("en"))
        assertTrue("extract the archive" in importPreviewErrorMessage(UnsupportedEncryptionException()).orEmpty())
        assertNull(importPreviewErrorMessage(IllegalStateException("autre")))
    }
}
