package fr.lunaison.core.data

import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.of
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Le parseur Clue est tolérant : JSON fabriqués de structures variées, champs inconnus, dates invalides, fichier vide. */
class ClueImporterTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    private fun flows(json: String): Map<LocalDate, Flow?> = ClueImporter.parse(json).dayLogs.associate { it.date to it.flow }

    @Test
    fun `dates en cles d'un objet`() {
        val result = flows("""{"2024-03-05": {"period": "heavy", "mood": ["happy"]}, "2024-03-06": {"period": {"value": "light"}}, "2024-03-07": {"mood": "sad"}}""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.HEAVY, d(2024, 3, 6) to Flow.LIGHT), result)
    }

    @Test
    fun `liste d'entrees datees avec la regle sous une cle`() {
        val result = flows("""{"days": [{"date": "2024-03-05", "period": {"value": "medium"}}, {"date": "2024-03-06", "bleeding": "spotting"}, {"date": "2024-03-07", "period": true}]}""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.MEDIUM, d(2024, 3, 6) to Flow.SPOTTING, d(2024, 3, 7) to Flow.MEDIUM), result)
    }

    @Test
    fun `entrees typees avec valeur imbriquee`() {
        val result = flows("""{"data": [{"day": "2024-03-05", "type": "period", "value": {"option": "heavy"}}, {"day": "2024-03-06", "type": "pain", "value": {"option": "cramps"}}]}""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.HEAVY), result)
    }

    @Test
    fun `etiquettes en liste et horodatages ISO`() {
        val result = flows("""[{"date": "2024-03-05T08:30:00Z", "tags": ["period_heavy", "cramps"]}, {"date": "2024-03-06 21:00:00", "tags": ["period"]}, {"date": "2024-03-07", "tags": ["cramps"]}]""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.HEAVY, d(2024, 3, 6) to Flow.MEDIUM), result)
    }

    @Test
    fun `horodatage Unix en secondes et en millisecondes`() {
        // 2024-03-05 12:00:00 UTC = 1709640000
        val seconds = flows("""[{"timestamp": 1709640000, "period": "light"}]""")
        val millis = flows("""[{"timestamp": 1709640000000, "period": "light"}]""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.LIGHT), seconds)
        assertEquals(seconds, millis)
    }

    @Test
    fun `intervalles et listes de dates`() {
        val range = flows("""{"period": [{"start": "2024-03-05", "end": "2024-03-08"}]}""")
        assertEquals(listOf(d(2024, 3, 5), d(2024, 3, 6), d(2024, 3, 7), d(2024, 3, 8)), range.keys.toList())
        val list = flows("""{"period": ["2024-04-01", "2024-04-02"]}""")
        assertEquals(setOf(d(2024, 4, 1), d(2024, 4, 2)), list.keys)
    }

    @Test
    fun `plusieurs entrees le meme jour gardent l'intensite la plus forte`() {
        val result = flows("""[{"date": "2024-03-05", "period": "light"}, {"date": "2024-03-05", "type": "period", "value": "heavy"}]""")
        assertEquals(mapOf(d(2024, 3, 5) to Flow.HEAVY), result)
    }

    @Test
    fun `champs inconnus, reglages et valeurs negatives sont ignores`() {
        val json = """
            {"settings": {"period_length": 5, "cycle_length": 28, "start": "2024-01-01", "end": "2024-01-09"},
             "weird": [null, 3, true, "texte libre qui parle de period et de heavy", {"a": {"b": {"c": []}}}],
             "entries": [{"date": "2024-03-05", "period": {"value": "none"}, "note": "heavy period ?", "unknown": {"x": 1}},
                         {"date": "2024-03-06", "period": false},
                         {"date": "2024-03-07", "period_pain": 3, "predicted_period": "2024-03-30"}]}
        """.trimIndent()
        val result = ClueImporter.parse(json)
        assertTrue(result.dayLogs.isEmpty(), "rien ne doit etre trouve : ${result.dayLogs}")
        assertEquals(1, result.validDocuments)
    }

    @Test
    fun `dates invalides ecartees et comptees sans planter`() {
        val result = ClueImporter.parse("""[{"date": "2024-02-31", "period": "heavy"}, {"date": "pas une date", "period": "heavy"}, {"date": "2024-03-05", "period": "heavy"}, {"date": "1850-01-01", "period": "light"}]""")
        assertEquals(listOf(d(2024, 3, 5)), result.dayLogs.map { it.date })
        assertEquals(2, result.skippedCount)
    }

    @Test
    fun `JSON invalide, vide ou de forme inattendue ne plante pas`() {
        assertEquals(0, ClueImporter.parse("").validDocuments)
        assertEquals(0, ClueImporter.parse("   \n").validDocuments)
        assertEquals(0, ClueImporter.parse("{pas du json").validDocuments)
        assertEquals(1, ClueImporter.parse("\"juste une chaine\"").validDocuments)
        assertEquals(1, ClueImporter.parse("42").validDocuments)
        assertEquals(1, ClueImporter.parse("null").validDocuments)
        assertTrue(ClueImporter.parse("[]").dayLogs.isEmpty())
        assertTrue(ClueImporter.parse("{}").dayLogs.isEmpty())
    }

    @Test
    fun `un BOM en tete est toleré`() {
        assertEquals(1, ClueImporter.parse("﻿" + """{"2024-03-05": {"period": "light"}}""").dayLogs.size)
    }

    @Test
    fun `une imbrication tres profonde ne fait pas deborder`() {
        val deep = "[".repeat(200) + "]".repeat(200)
        assertTrue(ClueImporter.parse(deep).dayLogs.isEmpty())
    }

    @Test
    fun `apercu avec cycles et periode couverte, erreurs typees`() {
        val json = """{"2024-01-01": {"period": "medium"}, "2024-01-02": {"period": "heavy"}, "2024-01-29": {"period": "light"}, "2024-01-30": {"period": "light"}}"""
        val preview = ClueImporter.toPreview(listOf(json))
        assertEquals(ImportSource.CLUE, preview.source)
        assertEquals(4, preview.daysCount)
        assertEquals(2, preview.cyclesCount)
        assertEquals(d(2024, 1, 1), preview.firstDate)
        assertEquals(d(2024, 1, 30), preview.lastDate)
        assertFailsWith<UnreadableImportException> { ClueImporter.toPreview(listOf("pas du json")) }
        assertFailsWith<NothingToImportException> { ClueImporter.toPreview(listOf("{}")) }
        assertFailsWith<UnreadableImportException> { ClueImporter.toPreview(emptyList()) }
    }

    @Test
    fun `plusieurs fichiers d'une archive sont fusionnes`() {
        val preview = ClueImporter.toPreview(listOf("""{"settings": {}}""", """{"2024-03-05": {"period": "light"}}""", "pas du json"))
        assertEquals(1, preview.daysCount)
    }

    @Test
    fun `les mots d'intensite sont compares par mots entiers`() {
        assertEquals(null, ClueKeyMapping.intensityOf("flow"))
        assertEquals(null, ClueKeyMapping.intensityOf("overflow"))
        assertEquals(Flow.HEAVY, ClueKeyMapping.intensityOf("periodHeavy"))
        assertEquals(Flow.LIGHT, ClueKeyMapping.intensityOf("period-light"))
        assertEquals(Flow.SPOTTING, ClueKeyMapping.intensityOf("Spotting"))
    }
}
