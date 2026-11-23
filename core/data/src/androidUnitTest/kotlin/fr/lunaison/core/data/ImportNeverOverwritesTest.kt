package fr.lunaison.core.data

import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.of
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Garde-fou : AUCUN import ne modifie une journée déjà notée, quelle que soit la source.
 * Les tests passent par les fonctions publiques (aperçu, plan, application), comme l'interface.
 * Si une source écrit un jour directement, ou si la fusion cesse de protéger les saisies, ils échouent.
 */
class ImportNeverOverwritesTest {
    private val password = "mdp-de-test"

    private val calendarDays = """
        {"entryList": [
          {"entrydatekey": 20250106, "flow": 3, "note": "venue du calendrier"},
          {"entrydatekey": 20250107, "flow": 2},
          {"entrydatekey": 20250108, "flow": 1, "weight": 60.5},
          {"entrydatekey": 20250120, "flow": 0, "symptoms": "bloating"}
        ]}
    """.trimIndent()
    private val calendarCycles = """{"cycleList": [{"cycleStartDate": 20250106, "cycleEndDate": 20250109}]}"""
    private val calendarSettings = """{"averageCLength": 30, "averagePLength": 6, "defaultCLength": 30}"""

    private val csv = LunaisonRepository.CSV_HEADER + "\n" +
        "2025-01-06;HEAVY;;CRAMPS;;false;;;;;;;;venue du csv\n" +
        "2025-01-07;LIGHT;;;;false;;;;;;;;\n" +
        "2025-01-09;MEDIUM;;;;false;;;;;;;;\n"

    private val clueJson = """{"2025-01-06": {"period": "heavy"}, "2025-01-07": {"period": "light"}, "2025-01-10": {"period": "medium"}}"""

    private fun at(d: Int) = LocalDateTime(2025, 1, d, 12, 0, 0, 0)

    private val healthData = HealthMenstruationData(
        periods = listOf(HealthPeriodRecord(at(6), at(9))),
        flows = emptyList(),
    )

    private fun zip(password: String?): ByteArray = PeriodCalendarImporterTest().zipEntries(
        listOf(
            "dayEntries_device.json" to calendarDays,
            "cyclesData_device.json" to calendarCycles,
            "settings_device.json" to calendarSettings,
        ),
        password,
    )

    /** Toutes les sources d'import, chacune lue par sa fonction publique d'aperçu. */
    private fun allPreviews(): Map<String, ImportPreview> = mapOf(
        "Calendrier ZIP" to previewAuto(zip(null)),
        "Calendrier ZIP chiffré" to previewAuto(zip(password), password),
        "Calendrier ZIP direct" to previewPeriodCalendarZip(zip(null)),
        "Calendrier JSON" to previewAuto(calendarDays.toByteArray()),
        "Calendrier JSON direct" to previewPeriodCalendarJson(calendarDays),
        "CSV Lunaison" to previewAuto(csv.toByteArray()),
        "CSV Lunaison direct" to previewLunaisonCsv(csv),
        "Clue" to previewClueFile(clueJson.toByteArray()),
        "Health Connect" to HealthRecordsMapper.toPreview(healthData),
    )

    private fun d(day: Int) = LocalDate.of(2025, 1, day)

    private fun saisie(day: Int) = DayLog(
        date = d(day),
        flow = Flow.SPOTTING,
        pain = Pain.SEVERE,
        note = "ma saisie du $day",
    )

    @Test
    fun `les sources couvrent bien tous les formats d'import`() {
        val sources = allPreviews().values.map { it.source }.toSet()
        assertEquals(ImportSource.entries.toSet(), sources, "une source d'import n'est pas couverte par le garde-fou")
        assertTrue(allPreviews().values.all { it.daysCount > 0 })
    }

    @Test
    fun `aucune source d'import ne modifie une journee deja notee`() = runBlocking {
        for ((name, preview) in allPreviews()) {
            val repo = LunaisonRepository(InMemoryLunaisonDao())
            // Une saisie sur chacun des jours que la source contient.
            val mine = preview.dayLogs.map { saisie(it.date.dayOfMonth) }
            mine.forEach { repo.save(it) }
            val before = repo.logsOnce()

            val plan = repo.planImport(preview)
            assertTrue(plan.toWrite.isEmpty(), "$name : le plan veut écrire sur une journée notée")
            assertEquals(mine.size, plan.keptExisting, name)
            assertEquals(before, repo.logsOnce(), "$name : l'aperçu a écrit")

            assertEquals(0, repo.applyImport(plan), name)
            assertEquals(before, repo.logsOnce(), "$name : une journée notée a été modifiée")
        }
    }

    @Test
    fun `les journees notees restent intactes et seules les journees libres sont ajoutees`() = runBlocking {
        for ((name, preview) in allPreviews()) {
            val repo = LunaisonRepository(InMemoryLunaisonDao())
            val noted = preview.dayLogs.first().date
            val mine = saisie(noted.dayOfMonth)
            repo.save(mine)

            val written = repo.applyImport(repo.planImport(preview))
            val after = repo.logsOnce().associateBy { it.date }
            assertEquals(mine, after.getValue(noted), "$name : la saisie a changé")
            assertEquals(preview.daysCount - 1, written, name)
            assertEquals(preview.dayLogs.map { it.date }.toSet(), after.keys, name)
        }
    }

    @Test
    fun `une journee saisie entre l'apercu et la confirmation n'est jamais ecrasee, quelle que soit la source`() = runBlocking {
        for ((name, preview) in allPreviews()) {
            val repo = LunaisonRepository(InMemoryLunaisonDao())
            val plan = repo.planImport(preview)
            val late = saisie(preview.dayLogs.first().date.dayOfMonth)
            repo.save(late)
            repo.applyImport(plan)
            assertEquals(late, repo.logsOnce().first { it.date == late.date }, name)
        }
    }

    @Test
    fun `la situation de depart du Calendrier ne se renseigne qu'a la confirmation et ne remplit que les champs vides`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val mine = Baseline(AgeGroup.ADULT, lastPeriodStart = LocalDate.of(2024, 12, 1), usualCycleLength = null, usualPeriodLength = null)
        repo.updateSettings { it.copy(baseline = mine) }

        val preview = previewAuto(zip(null))
        val plan = repo.planImport(preview)
        assertTrue(plan.fillsBaseline)
        assertEquals(mine, repo.settingsOnce().baseline, "l'aperçu a modifié les réglages")

        repo.applyImport(plan)
        val after = repo.settingsOnce().baseline
        assertEquals(LocalDate.of(2024, 12, 1), after.lastPeriodStart, "un repère déjà renseigné a été remplacé")
        assertEquals(30, after.usualCycleLength)
        assertEquals(6, after.usualPeriodLength)
        assertEquals(AgeGroup.ADULT, after.ageGroup)
    }

    @Test
    fun `un import qui n'apporte rien n'ecrit rien`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val preview = previewLunaisonCsv(csv)
        repo.applyImport(repo.planImport(preview))
        val once = repo.logsOnce()
        val again = repo.planImport(preview)
        assertTrue(again.isEmpty)
        assertEquals(0, repo.applyImport(again))
        assertEquals(once, repo.logsOnce())
    }

    @Test
    fun `le comptage des lignes illisibles du CSV est conserve dans l'apercu`() {
        val preview = previewAuto((csv + "pas une date;LIGHT\n2025-01-11;;;;;;;;;;;;;\n").toByteArray())
        assertEquals(ImportSource.LUNAISON_CSV, preview.source)
        assertEquals(2, preview.skippedCount)
        assertEquals(3, preview.daysCount)
    }
}
