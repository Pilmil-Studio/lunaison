package fr.lunaison.ui.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class BackupFileNamesTest {

    private val today = LocalDate(2026, 11, 17)

    @Test
    fun `le nom est date et neutre`() {
        val name = BackupFileNames.nameFor(today)
        assertEquals("sauvegarde-2026-11-17.pilmil", name)
        assertFalse(name.contains("regles", ignoreCase = true))
        assertFalse(name.contains("cycle", ignoreCase = true))
    }

    @Test
    fun `un nom de l appli se relit, les autres sont ignores`() {
        assertEquals(BackupFileNames.Parsed(today, 1), BackupFileNames.parse("sauvegarde-2026-11-17.pilmil"))
        assertEquals(BackupFileNames.Parsed(today, 2), BackupFileNames.parse("sauvegarde-2026-11-17-2.pilmil"))
        assertNull(BackupFileNames.parse("photo.jpg"))
        assertNull(BackupFileNames.parse("sauvegarde-2026-13-40.pilmil"))
        assertNull(BackupFileNames.parse("sauvegarde-2026-11-17 (1).pilmil"))
        assertNull(BackupFileNames.parse("mes-notes-sauvegarde-2026-11-17.pilmil"))
        assertNull(BackupFileNames.parse("sauvegarde-2026-11-17-0.pilmil"))
    }

    @Test
    fun `une deuxieme sauvegarde le meme jour ne reprend jamais un nom existant`() {
        assertEquals("sauvegarde-2026-11-17.pilmil", BackupFileNames.nextName(today, emptyList()))
        assertEquals("sauvegarde-2026-11-17-2.pilmil", BackupFileNames.nextName(today, listOf("sauvegarde-2026-11-17.pilmil")))
        assertEquals(
            "sauvegarde-2026-11-17-3.pilmil",
            BackupFileNames.nextName(today, listOf("sauvegarde-2026-11-17.pilmil", "sauvegarde-2026-11-17-2.pilmil")),
        )
        assertEquals("sauvegarde-2026-11-17.pilmil", BackupFileNames.nextName(today, listOf("sauvegarde-2026-11-16.pilmil")))
    }

    @Test
    fun `rotation : on garde les 3 plus recentes, nouvelle comprise`() {
        val existing = listOf(
            "sauvegarde-2026-11-10.pilmil",
            "sauvegarde-2026-11-12.pilmil",
            "sauvegarde-2026-11-15.pilmil",
            "sauvegarde-2026-11-16.pilmil",
            "sauvegarde-2026-11-17.pilmil",
        )
        val doomed = BackupFileNames.toDelete(existing, justWritten = "sauvegarde-2026-11-17.pilmil")
        assertEquals(setOf("sauvegarde-2026-11-10.pilmil", "sauvegarde-2026-11-12.pilmil"), doomed.toSet())
    }

    @Test
    fun `rotation : moins de 3 fichiers, rien n est supprime`() {
        val existing = listOf("sauvegarde-2026-11-16.pilmil", "sauvegarde-2026-11-17.pilmil")
        assertTrue(BackupFileNames.toDelete(existing, "sauvegarde-2026-11-17.pilmil").isEmpty())
    }

    @Test
    fun `rotation : les fichiers etrangers ne sont jamais supprimes`() {
        val existing = listOf(
            "releve-banque.pdf",
            "sauvegarde-2026-11-01.pilmil",
            "sauvegarde-2026-11-02.pilmil",
            "sauvegarde-2026-11-03.pilmil",
            "sauvegarde-2026-11-04.pilmil",
        )
        val doomed = BackupFileNames.toDelete(existing, "sauvegarde-2026-11-04.pilmil")
        assertEquals(setOf("sauvegarde-2026-11-01.pilmil"), doomed.toSet())
    }

    @Test
    fun `rotation : le fichier qui vient d etre ecrit n est jamais supprime, meme avec une horloge en retard`() {
        val existing = listOf(
            "sauvegarde-2027-01-01.pilmil",
            "sauvegarde-2027-01-02.pilmil",
            "sauvegarde-2027-01-03.pilmil",
            "sauvegarde-2026-11-17.pilmil",
        )
        val doomed = BackupFileNames.toDelete(existing, "sauvegarde-2026-11-17.pilmil")
        assertFalse("sauvegarde-2026-11-17.pilmil" in doomed)
        assertEquals(listOf("sauvegarde-2027-01-01.pilmil"), doomed)
    }

    @Test
    fun `les numeros du meme jour sont ordonnes`() {
        val existing = listOf("sauvegarde-2026-11-17.pilmil", "sauvegarde-2026-11-17-2.pilmil", "sauvegarde-2026-11-17-3.pilmil", "sauvegarde-2026-11-17-4.pilmil")
        assertEquals(listOf("sauvegarde-2026-11-17.pilmil"), BackupFileNames.toDelete(existing, "sauvegarde-2026-11-17-4.pilmil"))
    }
}

class BackupPlannerTest {

    private val today = LocalDate(2026, 11, 17)
    private val yesterday = LocalDate(2026, 11, 16)

    private fun decide(
        configured: Boolean = true,
        hasData: Boolean = true,
        lastSuccessDate: LocalDate? = yesterday,
        lastFingerprint: String? = "ancienne",
        fingerprint: String = "nouvelle",
        force: Boolean = false,
    ) = BackupPlanner.decide(configured, hasData, today, lastSuccessDate, lastFingerprint, fingerprint, force)

    @Test
    fun `donnees changees depuis hier on sauvegarde`() = assertEquals(BackupDecision.Run, decide())

    @Test
    fun `premiere sauvegarde`() = assertEquals(BackupDecision.Run, decide(lastSuccessDate = null, lastFingerprint = null))

    @Test
    fun `donnees inchangees on ne sauvegarde pas`() =
        assertEquals(BackupDecision.Skip(SkipReason.UNCHANGED), decide(lastFingerprint = "nouvelle"))

    @Test
    fun `deja sauvegarde aujourd hui, meme si les donnees ont change`() =
        assertEquals(BackupDecision.Skip(SkipReason.DONE_TODAY), decide(lastSuccessDate = today))

    @Test
    fun `rien n est fait sans phrase`() =
        assertEquals(BackupDecision.Skip(SkipReason.NOT_CONFIGURED), decide(configured = false, force = true))

    @Test
    fun `une base vide ne remplace jamais de vraies sauvegardes`() {
        assertEquals(BackupDecision.Skip(SkipReason.NOTHING_TO_SAVE), decide(hasData = false))
        assertEquals(BackupDecision.Skip(SkipReason.NOTHING_TO_SAVE), decide(hasData = false, force = true))
    }

    @Test
    fun `le bouton Sauvegarder maintenant passe outre le jour et l absence de changement`() {
        assertEquals(BackupDecision.Run, decide(lastSuccessDate = today, lastFingerprint = "nouvelle", force = true))
    }

    @Test
    fun `l empreinte change quand les donnees changent`() {
        val a = BackupPlanner.fingerprint("""{"jours":1}""")
        assertEquals(a, BackupPlanner.fingerprint("""{"jours":1}"""))
        assertNotEquals(a, BackupPlanner.fingerprint("""{"jours":2}"""))
        assertEquals(64, a.length)
    }
}
