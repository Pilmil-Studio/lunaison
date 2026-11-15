package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.Mood
import fr.lunaison.core.model.ThemePalette
import fr.lunaison.core.model.of
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Données fabriquées. */
class BackupRestoreTest {

    private suspend fun filledRepo(): LunaisonRepository {
        val repo = LunaisonRepository(InMemoryLunaisonDao())
        repo.save(DayLog(LocalDate.of(2026, 4, 1), flow = Flow.MEDIUM, moods = setOf(Mood.CALM), note = "Premier jour"))
        repo.save(DayLog(LocalDate.of(2026, 4, 2), flow = Flow.HEAVY, weight = 60.2f))
        repo.updateSettings {
            it.copy(
                onboarded = true,
                themePalette = ThemePalette.SAGE,
                myShareName = "Camille",
                pinHash = AppSettings.hashPin("2580"),
            )
        }
        return repo
    }

    @Test
    fun `sauvegarde puis restauration sur un autre telephone`() = runBlocking {
        val source = filledRepo()
        val json = source.exportBackupJson()

        val target = LunaisonRepository(InMemoryLunaisonDao())
        target.save(DayLog(LocalDate.of(2025, 1, 1), flow = Flow.LIGHT)) // doit disparaître
        val summary = target.restoreBackupJson(json)

        assertEquals(2, summary.daysCount)
        assertEquals(source.logsOnce(), target.logsOnce())
        val restored = target.settingsOnce()
        assertEquals(source.settingsOnce().copy(pinHash = null), restored)
    }

    @Test
    fun `la sauvegarde ne contient pas le PIN et la restauration garde celui du telephone`() = runBlocking {
        val source = filledRepo()
        val json = source.exportBackupJson()
        assertTrue(!json.contains("pinHash"))

        val target = LunaisonRepository(InMemoryLunaisonDao())
        target.updateSettings { it.copy(pinHash = AppSettings.hashPin("1111")) }
        target.restoreBackupJson(json)
        val pin = target.settingsOnce().pinHash
        assertNotNull(pin)
        assertTrue(AppSettings.verifyPin("1111", pin))
        assertEquals("Camille", target.settingsOnce().myShareName)
    }

    @Test
    fun `une sauvegarde sans reglages ne supprime pas le PIN du telephone`() = runBlocking {
        val json = LunaisonRepository(InMemoryLunaisonDao()).exportBackupJson()

        val target = LunaisonRepository(InMemoryLunaisonDao())
        target.updateSettings { it.copy(pinHash = AppSettings.hashPin("1111")) }
        target.restoreBackupJson(json)
        val pin = target.settingsOnce().pinHash
        assertNotNull(pin)
        assertTrue(AppSettings.verifyPin("1111", pin))
    }

    @Test
    fun `une version inconnue ne modifie rien`() = runBlocking {
        val json = filledRepo().exportBackupJson().replace("\"version\":1", "\"version\":9")
        val target = filledRepo()
        val before = target.logsOnce() to target.settingsOnce()
        val e = assertFailsWith<BackupFormatException> { target.restoreBackupJson(json) }
        assertEquals(BackupFailure.UNSUPPORTED_VERSION, e.failure)
        assertEquals(before, target.logsOnce() to target.settingsOnce())
    }

    @Test
    fun `un JSON tronque ne modifie rien`() = runBlocking {
        val json = filledRepo().exportBackupJson()
        val target = filledRepo()
        target.save(DayLog(LocalDate.of(2026, 4, 3), flow = Flow.LIGHT))
        val before = target.logsOnce() to target.settingsOnce()
        for (cut in listOf(json.length / 4, json.length / 2, json.length - 3)) {
            assertFailsWith<BackupFormatException> { target.restoreBackupJson(json.substring(0, cut)) }
            assertEquals(before, target.logsOnce() to target.settingsOnce())
        }
    }
}
