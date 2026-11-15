package fr.lunaison.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Données fabriquées. Toutes les colonnes de chaque table sont renseignées, pour qu'un oubli se voie. */
class BackupDocumentTest {

    private val logs = listOf(
        DayLogEntity(
            date = "2026-02-03",
            flow = "HEAVY",
            pain = "MODERATE",
            symptoms = "CRAMPS,FATIGUE",
            moods = "CALM,SENSITIVE",
            note = "Essai \"guillemets\", barre \\ et\nsaut de ligne ; accents é ï œ, emoji 🌙, tab\t.",
            medicationTaken = true,
            mucus = "EGG_WHITE",
            basalTemperature = 36.6f,
            pillStatus = "TAKEN",
            medications = "SUPPLEMENT",
            weight = 61.5f,
            intimacy = "PROTECTED",
            libido = "HIGH",
        ),
        DayLogEntity(
            date = "2026-02-04",
            flow = null, pain = null, symptoms = "", moods = "", note = "",
            medicationTaken = false, mucus = null, basalTemperature = null, pillStatus = null,
            medications = "", weight = null, intimacy = "", libido = null,
        ),
    )

    private val settings = SettingsEntity(
        id = 0,
        onboarded = true,
        ageGroup = "ADULT",
        lastPeriodStart = "2026-02-03",
        usualCycleLength = 29,
        usualPeriodLength = 5,
        periodReminder = true,
        periodReminderDaysBefore = 2,
        logReminder = "DAILY",
        logReminderTime = "21:15",
        partnerName = "Alex",
        partnerPhone = "0600000000",
        partnerChannel = "SIGNAL",
        dismissedAlerts = "IRREGULAR@2026-01-10",
        recapSeen = "2026-01-05",
        medicationReminder = true,
        medicationReminderTime = "08:30",
        medicationLabel = "Pilule",
        pausePredictions = true,
        showFertility = true,
        themePalette = "SAGE",
        pinHash = "pbkdf2\$aa\$bb",
        followedPartnerSnapshot = "opaque-snapshot-value",
        myShareName = "Camille",
        trackOvulationSigns = true,
        irregularCyclesMode = true,
        reviewPromptDismissed = true,
        articleLastShown = "spm@2026-02-01,cycle@2026-01-20",
    )

    @Test
    fun `aller-retour retrouve toutes les tables, sans le code PIN`() {
        val json = LunaisonBackup.encode(BackupContent(logs, settings))
        assertTrue(json.contains("\"format\":\"lunaison-backup\""))
        assertTrue(json.contains("\"version\":1"))
        assertFalse(json.contains("pinHash"))
        assertFalse(json.contains("pbkdf2"))

        val back = LunaisonBackup.decode(json)
        assertEquals(logs, back.logs)
        assertEquals(settings.copy(pinHash = null), back.settings)
    }

    @Test
    fun `sauvegarde sans reglages`() {
        val back = LunaisonBackup.decode(LunaisonBackup.encode(BackupContent(emptyList(), null)))
        assertTrue(back.logs.isEmpty())
        assertNull(back.settings)
    }

    @Test
    fun `une version inconnue est refusee`() {
        val json = LunaisonBackup.encode(BackupContent(logs, settings)).replace("\"version\":1", "\"version\":2")
        val e = assertFailsWith<BackupFormatException> { LunaisonBackup.decode(json) }
        assertEquals(BackupFailure.UNSUPPORTED_VERSION, e.failure)
    }

    @Test
    fun `une sauvegarde v7 sans articleLastShown reste importable`() {
        val v8Json = LunaisonBackup.encode(BackupContent(logs, settings))
        val v7Json = v8Json
            .replace("\"schemaVersion\":8", "\"schemaVersion\":7")
            .replace(",\"articleLastShown\":\"spm@2026-02-01,cycle@2026-01-20\"", "")
        val back = LunaisonBackup.decode(v7Json)
        assertEquals("", back.settings?.articleLastShown)
    }

    @Test
    fun `un JSON tronque est refuse a chaque longueur`() {
        val json = LunaisonBackup.encode(BackupContent(logs, settings))
        for (len in listOf(0, 1, 10, json.length / 3, json.length / 2, json.length - 20, json.length - 1)) {
            val e = assertFailsWith<BackupFormatException>("longueur $len") { LunaisonBackup.decode(json.substring(0, len)) }
            assertEquals(BackupFailure.INVALID, e.failure)
        }
    }

    @Test
    fun `un autre format ou des donnees incoherentes sont refuses`() {
        val good = LunaisonBackup.encode(BackupContent(logs, settings))
        val bad = listOf(
            "[]",
            "{}",
            good.replace("lunaison-backup", "autre-chose"),
            good.replace("\"date\":\"2026-02-03\"", "\"date\":\"pas-une-date\""),
            good.replace("\"logReminderTime\":\"21:15\"", "\"logReminderTime\":\"minuit\""),
            good.replace("\"onboarded\":true", "\"onboarded\":\"oui\""),
            "{\"format\":\"lunaison-backup\",\"version\":1,\"tables\":{\"day_logs\":{},\"settings\":[]}}",
            "{\"format\":\"lunaison-backup\",\"version\":1,\"tables\":{\"day_logs\":[],\"settings\":{}}}",
            "{\"format\":\"lunaison-backup\",\"version\":1,\"tables\":{\"day_logs\":[42],\"settings\":[]}}",
            "{\"format\":\"lunaison-backup\",\"version\":1,\"tables\":{\"day_logs\":[{\"date\":\"2026-02-03\",\"flow\":null,\"pain\":null,\"symptoms\":\"\",\"moods\":\"\",\"note\":\"\",\"medicationTaken\":false,\"mucus\":null,\"basalTemperature\":\"chaud\",\"pillStatus\":null,\"medications\":\"\",\"weight\":null,\"intimacy\":\"\",\"libido\":null}],\"settings\":[]}}",
            "[".repeat(40) + "]".repeat(40),
            good + "garbage",
        )
        bad.forEach { json ->
            val e = assertFailsWith<BackupFormatException>(json.take(60)) { LunaisonBackup.decode(json) }
            assertEquals(BackupFailure.INVALID, e.failure)
        }
    }

    @Test
    fun `echappements unicode et valeurs limites sont preserves`() {
        val tricky = DayLogEntity(
            date = "2026-03-01",
            flow = null,
            pain = null,
            symptoms = "",
            moods = "",
            note = "Ctrl:\b\u000C\r\t\"\\ et unicode \u00E9\u20AC",
            medicationTaken = false,
            mucus = null,
            basalTemperature = 37.25f,
            pillStatus = null,
            medications = "",
            weight = 54.0f,
            intimacy = "",
            libido = null,
        )
        val json = LunaisonBackup.encode(BackupContent(listOf(tricky), null))
        val back = LunaisonBackup.decode(json)
        assertEquals(listOf(tricky), back.logs)
    }
}
