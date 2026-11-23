package fr.lunaison.core.data

import fr.lunaison.core.model.CervicalMucus
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.IntimacyKind
import fr.lunaison.core.model.Libido
import fr.lunaison.core.model.MedicationKind
import fr.lunaison.core.model.Mood
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.PillStatus
import fr.lunaison.core.model.Symptom
import fr.lunaison.core.model.of
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Données fabriquées : l'export CSV doit se relire à l'identique quelle que soit la langue du téléphone. */
class CsvPortabilityTest {

    private val initialLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() = Locale.setDefault(initialLocale)

    private fun setLanguage(code: String) = Locale.setDefault(Locale.forLanguageTag(code))

    private val sample = listOf(
        DayLog(
            date = LocalDate.of(2026, 3, 3),
            flow = Flow.HEAVY,
            pain = Pain.MODERATE,
            symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
            moods = setOf(Mood.CALM, Mood.SENSITIVE),
            note = "Journée d'essai",
            medicationTaken = true,
            pillStatus = PillStatus.TAKEN,
            medications = setOf(MedicationKind.SUPPLEMENT),
            weight = 61.5f,
            intimacy = setOf(IntimacyKind.PROTECTED),
            libido = Libido.HIGH,
            mucus = CervicalMucus.EGG_WHITE,
            basalTemperature = 36.6f,
        ),
        DayLog(date = LocalDate.of(2026, 3, 4), flow = Flow.SPOTTING),
        DayLog(date = LocalDate.of(2026, 3, 9), moods = setOf(Mood.CALM), note = "RAS"),
    )

    private suspend fun repoWith(logs: List<DayLog>) =
        LunaisonRepository(InMemoryLunaisonDao()).also { repo -> logs.forEach { repo.save(it) } }

    private fun roundTrip(from: String, to: String) = runBlocking {
        setLanguage(from)
        val csv = repoWith(sample).exportCsv()
        setLanguage(to)
        val target = LunaisonRepository(InMemoryLunaisonDao())
        val preview = previewLunaisonCsv(csv)
        assertEquals(sample.size, preview.daysCount)
        assertEquals(0, preview.skippedCount)
        assertEquals(0, preview.unknownValues)
        assertEquals(ImportSource.LUNAISON_CSV, preview.source)
        // L'aperçu n'écrit rien : seule la confirmation écrit.
        assertEquals(emptyList(), target.logsOnce())
        assertEquals(sample.size, target.applyImport(target.planImport(preview)))
        assertEquals(sample, target.logsOnce())
    }

    @Test
    fun `aller-retour fr vers en`() = roundTrip("fr", "en")

    @Test
    fun `aller-retour en vers uk`() = roundTrip("en", "uk")

    @Test
    fun `l'export est identique dans toutes les langues et ne contient que des valeurs canoniques`() = runBlocking {
        val exports = listOf("fr", "en", "pl", "uk", "es", "de").map { code ->
            setLanguage(code)
            repoWith(sample).exportCsv()
        }
        assertEquals(1, exports.toSet().size)
        val lines = exports.first().trimEnd().lines()
        assertEquals(LunaisonRepository.CSV_HEADER, lines.first())
        assertEquals(
            "2026-03-03;HEAVY;MODERATE;CRAMPS, FATIGUE;CALM, SENSITIVE;true;EGG_WHITE;36.6;TAKEN;SUPPLEMENT;61.5;PROTECTED;HIGH;Journée d'essai",
            lines[1],
        )
        assertTrue(lines[2].startsWith("2026-03-04;SPOTTING;;;;false;"))
    }

    @Test
    fun `un ancien CSV produit par la version francaise de l'appli est relu en anglais`() = runBlocking {
        // Reconstitue un export tel que l'ancien code l'écrivait : libellés traduits, « oui ».
        setLanguage("fr")
        val log = sample.first()
        val old = listOf(
            log.date.toString(), log.flow!!.label, log.pain!!.label,
            log.symptoms.joinToString(", ") { it.label }, log.moods.joinToString(", ") { it.label }, "oui",
            log.mucus!!.label, "36.6", log.pillStatus!!.label, log.medications.joinToString(", ") { it.label },
            "61.5", log.intimacy.joinToString(", ") { it.label }, log.libido!!.label, log.note,
        ).joinToString(";")
        setLanguage("en")
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val preview = previewLunaisonCsv("date;flux;...\n$old")
        assertEquals(1, preview.daysCount)
        assertEquals(0, preview.unknownValues)
        repo.applyImport(repo.planImport(preview))
        assertEquals(log, repo.logsOnce().single())
    }

    @Test
    fun `les anciens oui et yes sont acceptes`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        repo.confirmImport(previewLunaisonCsv("2026-03-01;LIGHT;;;;oui\n2026-03-02;LIGHT;;;;yes\n2026-03-03;LIGHT;;;;true\n2026-03-04;LIGHT;;;;false"))
        assertEquals(listOf(true, true, true, false), repo.logsOnce().map { it.medicationTaken })
    }

    @Test
    fun `une valeur inconnue n'empeche pas l'import du reste de la ligne`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val csv = LunaisonRepository.CSV_HEADER + "\n" +
            "2026-03-01;HEAVY;PAS_UNE_DOULEUR;CRAMPS, INCONNU;CALM;false;;;;;;;;Bonjour\n" +
            "pas une date;LIGHT;;;;;;;;;;;;\n" +
            "2026-03-02;;;;;;;;;;;;;\n"
        val preview = previewLunaisonCsv(csv)
        assertEquals(1, preview.daysCount)
        assertEquals(2, preview.skippedCount) // ligne illisible + journée vide ; l'en-tête n'est pas comptée
        assertEquals(2, preview.unknownValues)
        repo.confirmImport(preview)
        val log = repo.logsOnce().single()
        assertEquals(Flow.HEAVY, log.flow)
        assertEquals(null, log.pain)
        assertEquals(setOf(Symptom.CRAMPS), log.symptoms)
        assertEquals(setOf(Mood.CALM), log.moods)
        assertEquals("Bonjour", log.note)
        assertFalse(log.medicationTaken)
    }

    @Test
    fun `une note protegee contre les formules revient intacte`() = runBlocking {
        val note = "=1+1"
        val csv = repoWith(listOf(DayLog(LocalDate.of(2026, 3, 1), note = note))).exportCsv()
        val target = LunaisonRepository(InMemoryLunaisonDao())
        target.confirmImport(previewLunaisonCsv(csv))
        assertEquals(note, target.logsOnce().single().note)
    }
}
