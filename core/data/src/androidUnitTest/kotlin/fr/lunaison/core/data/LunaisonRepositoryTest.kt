package fr.lunaison.core.data

import fr.lunaison.core.cycle.AlertKind
import fr.lunaison.core.cycle.LogReminderMode
import fr.lunaison.core.cycle.ReminderSettings
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.CervicalMucus
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.IntimacyKind
import fr.lunaison.core.model.Libido
import fr.lunaison.core.model.MedicationKind
import fr.lunaison.core.model.Mood
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.PartnerPeriod
import fr.lunaison.core.model.PartnerSnapshot
import fr.lunaison.core.model.PillStatus
import fr.lunaison.core.model.Symptom
import fr.lunaison.core.model.ThemePalette
import fr.lunaison.core.model.now
import fr.lunaison.core.model.of
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow as CoroutineFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class InMemoryLunaisonDao : LunaisonDao {
    private val logsMap = MutableStateFlow<Map<String, DayLogEntity>>(emptyMap())
    private val settingsFlow = MutableStateFlow<SettingsEntity?>(null)

    override fun logs(): CoroutineFlow<List<DayLogEntity>> =
        logsMap.map { it.values.sortedBy { entity -> entity.date } }

    override suspend fun logsOnce(): List<DayLogEntity> =
        logsMap.value.values.sortedBy { it.date }

    override suspend fun upsertLog(log: DayLogEntity) {
        logsMap.value = logsMap.value + (log.date to log)
    }

    override suspend fun deleteLog(date: String) {
        logsMap.value = logsMap.value - date
    }

    override fun settings(): CoroutineFlow<SettingsEntity?> = settingsFlow

    override suspend fun settingsOnce(): SettingsEntity? = settingsFlow.value

    override suspend fun upsertSettings(settings: SettingsEntity) {
        settingsFlow.value = settings
    }

    override suspend fun deleteLogs() {
        logsMap.value = emptyMap()
    }

    override suspend fun deleteSettings() {
        settingsFlow.value = null
    }
}

class LunaisonRepositoryTest {

    @Test
    fun `save supprime une journee vide et eraseAll reinitialise tout`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val d1 = LocalDate.of(2026, 5, 1)
        val d2 = LocalDate.of(2026, 5, 2)

        repo.save(DayLog(d1, flow = Flow.MEDIUM))
        repo.save(DayLog(d2, flow = Flow.LIGHT))
        assertEquals(2, repo.logsOnce().size)

        // Enregistrer un DayLog vide supprime la journée
        repo.save(DayLog(d1))
        assertEquals(listOf(d2), repo.logsOnce().map { it.date })

        repo.updateSettings { it.copy(onboarded = true, myShareName = "Camille") }
        assertTrue(repo.settingsOnce().onboarded)

        repo.eraseAll()
        assertTrue(repo.logsOnce().isEmpty())
        assertEquals(AppSettings(), repo.settingsOnce())
    }

    @Test
    fun `exportCsv puis importCsv reconstitue fidelement les donnees v2`() = runBlocking {
        val sourceRepo = LunaisonRepository(InMemoryLunaisonDao())
        val original = DayLog(
            date = LocalDate.of(2026, 5, 10),
            flow = Flow.HEAVY,
            pain = Pain.MODERATE,
            symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
            moods = setOf(Mood.CALM, Mood.SENSITIVE),
            note = "Note; avec point-virgule\net saut de ligne",
            medicationTaken = true,
            pillStatus = PillStatus.TAKEN,
            medications = setOf(MedicationKind.SUPPLEMENT),
            weight = 58.4f,
            intimacy = setOf(IntimacyKind.PROTECTED),
            libido = Libido.HIGH,
            mucus = CervicalMucus.EGG_WHITE,
            basalTemperature = 36.7f,
        )
        sourceRepo.save(original)

        val csv = sourceRepo.exportCsv()
        val targetRepo = LunaisonRepository(InMemoryLunaisonDao())
        val importedCount = targetRepo.importCsv(csv)

        assertEquals(1, importedCount)
        val restored = targetRepo.logsOnce().single()
        assertEquals(original.date, restored.date)
        assertEquals(original.flow, restored.flow)
        assertEquals(original.pain, restored.pain)
        assertEquals(original.symptoms, restored.symptoms)
        assertEquals(original.moods, restored.moods)
        assertEquals(original.pillStatus, restored.pillStatus)
        assertTrue(restored.medicationTaken)
        assertEquals(original.medications, restored.medications)
        assertEquals(58.4f, restored.weight)
        assertEquals(original.intimacy, restored.intimacy)
        assertEquals(original.libido, restored.libido)
        assertEquals(original.mucus, restored.mucus)
        assertEquals(36.7f, restored.basalTemperature)
        assertEquals("Note, avec point-virgule et saut de ligne", restored.note)
    }

    @Test
    fun `importCsv accepte un CSV externe a virgules et des colonnes v1`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        val externalCsv = """
            date,flow,pain,symptoms,moods,medication,note
            2026-04-01,heavy,NONE,headache,,yes,Migraine
            2026-04-02,spotting,,,,,
            ligne-invalide,foo,bar
        """.trimIndent()

        val count = repo.importCsv(externalCsv)
        assertEquals(2, count)
        val logs = repo.logsOnce()
        assertEquals(Flow.HEAVY, logs[0].flow)
        assertEquals(Pain.NONE, logs[0].pain)
        assertEquals(setOf(Symptom.HEADACHE), logs[0].symptoms)
        assertTrue(logs[0].medicationTaken)
        assertEquals("Migraine", logs[0].note)
        assertEquals(Flow.SPOTTING, logs[1].flow)
    }

    @Test
    fun `SettingsEntity et AppSettings font un aller-retour complet`() {
        val today = LocalDate.now()
        val snapshot = PartnerSnapshot(
            partnerName = "Alex",
            sharedAt = today,
            periods = listOf(PartnerPeriod(today, 4)),
            usualCycleLength = 29,
            showFertility = true,
        )
        val settings = AppSettings(
            onboarded = true,
            baseline = Baseline(AgeGroup.UNDER_18, LocalDate.of(2026, 4, 1), 30, 5),
            reminders = ReminderSettings(
                periodReminder = false,
                periodReminderDaysBefore = 3,
                logReminder = LogReminderMode.DAILY,
                logReminderTime = LocalTime.of(21, 15),
                medicationReminder = true,
                medicationReminderTime = LocalTime.of(8, 30),
                medicationLabel = "Pilule",
            ),
            partner = Partner("Alex", "0600000000", PartnerChannel.SIGNAL),
            dismissedAlerts = mapOf(AlertKind.IRREGULAR to LocalDate.of(2026, 4, 10)),
            recapSeen = LocalDate.of(2026, 4, 1),
            pausePredictions = true,
            showFertility = true,
            themePalette = ThemePalette.SAGE,
            pinHash = AppSettings.hashPin("2580"),
            followedPartner = snapshot,
            myShareName = "Camille",
            trackOvulationSigns = true,
            irregularCyclesMode = true,
            reviewPromptDismissed = true,
            articleLastShown = mapOf(
                "spm" to LocalDate.of(2026, 4, 12),
                "douleurs" to LocalDate.of(2026, 4, 5),
            ),
            decoyPinHash = AppSettings.hashPin("1357"),
            biometricUnlock = true,
            discreetNotifications = true,
            country = "FR",
        )

        val restored = settings.toEntity().toModel()
        assertEquals(settings, restored)
        assertNotNull(restored.followedPartner)
        assertNull(AppSettings().toEntity().toModel().partner)

        val malformed = settings.toEntity().copy(
            articleLastShown = "spm@2026-04-12,bad,@2026-04-01,cycle@not-a-date",
        ).toModel()
        assertEquals(mapOf("spm" to LocalDate.of(2026, 4, 12)), malformed.articleLastShown)
    }

    @Test
    fun `le leurre reprend l'apparence de la vraie base sans aucune donnee`() = runBlocking {
        val real = AppSettings(
            onboarded = true,
            baseline = Baseline(AgeGroup.ADULT, LocalDate.of(2026, 4, 1), 30, 5),
            themePalette = ThemePalette.SAGE,
            pinHash = AppSettings.hashPin("2580"),
            decoyPinHash = AppSettings.hashPin("1357"),
            biometricUnlock = true,
            myShareName = "Camille",
        )
        val decoy = LunaisonRepository(InMemoryLunaisonDao())
        decoy.prepareAsDecoyOf(real)

        val settings = decoy.settingsOnce()
        assertTrue(settings.onboarded)
        assertEquals(AgeGroup.ADULT, settings.baseline.ageGroup)
        assertNull(settings.baseline.lastPeriodStart)
        assertEquals(ThemePalette.SAGE, settings.themePalette)
        assertEquals(real.pinHash, settings.pinHash)
        assertNull(settings.decoyPinHash)
        assertEquals(false, settings.biometricUnlock)
        assertEquals("", settings.myShareName)
        assertTrue(decoy.logsOnce().isEmpty())

        // Les journées notées dans le leurre restent d'une ouverture à l'autre.
        decoy.save(DayLog(LocalDate.of(2026, 5, 2), flow = Flow.LIGHT))
        decoy.prepareAsDecoyOf(real)
        assertEquals(1, decoy.logsOnce().size)
    }

    @Test
    fun `exportCsv utilise des en-tetes stables et neutralise l'injection de formules`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        repo.save(DayLog(LocalDate.of(2026, 5, 1), note = "=CMD|'/C calc'!A0"))
        repo.save(DayLog(LocalDate.of(2026, 5, 2), note = "+1+1"))
        repo.save(DayLog(LocalDate.of(2026, 5, 3), note = "-2*3"))
        repo.save(DayLog(LocalDate.of(2026, 5, 4), note = "@SUM(A1)"))
        repo.save(DayLog(LocalDate.of(2026, 5, 5), note = "RAS"))

        val lines = repo.exportCsv().trimEnd().lines()
        assertEquals(
            "date;flow;pain;symptoms;moods;medication;mucus;temperature;pill;medications;weight;intimacy;libido;note",
            lines.first(),
        )
        assertTrue(lines[1].endsWith(";'=CMD|'/C calc'!A0"))
        assertTrue(lines[2].endsWith(";'+1+1"))
        assertTrue(lines[3].endsWith(";'-2*3"))
        assertTrue(lines[4].endsWith(";'@SUM(A1)"))
        assertTrue(lines[5].endsWith(";RAS"))
    }

    @Test
    fun `updateSettings serialise les mises a jour concurrentes`() = runBlocking {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        (1..20).map {
            async(Dispatchers.Default) {
                repo.updateSettings { current ->
                    current.copy(baseline = current.baseline.copy(usualCycleLength = (current.baseline.usualCycleLength ?: 0) + 1))
                }
            }
        }.awaitAll()
        assertEquals(20, repo.settingsOnce().baseline.usualCycleLength)
    }
}
