package fr.lunaison.ui.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class BackupReminderTest {

    private val today = LocalDate(2026, 11, 17)
    private val none = AutoBackupStatus.NotConfigured()

    @Test
    fun `carte affichee avec 14 jours de saisie et aucune sauvegarde`() {
        assertTrue(BackupReminder.shouldShowReminder(none, loggedDays = 14, today = today))
        assertTrue(BackupReminder.shouldShowReminder(none, loggedDays = 200, today = today))
    }

    @Test
    fun `pas de carte avant 14 jours`() {
        assertFalse(BackupReminder.shouldShowReminder(none, loggedDays = 13, today = today))
        assertFalse(BackupReminder.shouldShowReminder(none, loggedDays = 0, today = today))
    }

    @Test
    fun `pas de carte quand une sauvegarde est configuree`() {
        val configured = AutoBackupStatus.Configured(folderName = "Documents", lastSuccessMillis = 1L)
        assertFalse(BackupReminder.shouldShowReminder(configured, loggedDays = 100, today = today))
    }

    @Test
    fun `pas de carte sur une plateforme sans sauvegarde automatique`() {
        assertFalse(BackupReminder.shouldShowReminder(AutoBackupStatus.Unavailable, loggedDays = 100, today = today))
    }

    @Test
    fun `Plus tard masque la carte 30 jours`() {
        val until = BackupReminder.hiddenUntilAfterLater(today)
        assertEquals(LocalDate(2026, 12, 17), until)
        val hidden = AutoBackupStatus.NotConfigured(cardHiddenUntil = until)

        assertFalse(BackupReminder.shouldShowReminder(hidden, 50, today))
        assertFalse(BackupReminder.shouldShowReminder(hidden, 50, LocalDate(2026, 12, 16)))
        assertTrue(BackupReminder.shouldShowReminder(hidden, 50, LocalDate(2026, 12, 17)))
    }

    @Test
    fun `un probleme est signale sur l accueil des qu une sauvegarde configuree echoue`() {
        val failing = AutoBackupStatus.Configured("Documents", 1L, BackupProblem.FOLDER_UNAVAILABLE)
        assertEquals(BackupProblem.FOLDER_UNAVAILABLE, BackupReminder.problemToShow(failing))
        assertNull(BackupReminder.problemToShow(AutoBackupStatus.Configured("Documents", 1L)))
        assertNull(BackupReminder.problemToShow(none))
        assertNull(BackupReminder.problemToShow(AutoBackupStatus.Unavailable))
    }
}

class PassphraseRulesTest {

    @Test
    fun `phrase trop courte refusee`() {
        assertEquals(PassphraseStrength.TOO_SHORT, PassphraseRules.strength("court"))
        assertEquals(PassphraseStrength.TOO_SHORT, PassphraseRules.strength("12345678901"))
        assertFalse(PassphraseRules.check("court", "court").valid)
    }

    @Test
    fun `phrase faite d une seule lettre refusee`() {
        assertEquals(PassphraseStrength.TOO_SHORT, PassphraseRules.strength("aaaaaaaaaaaaaaaaaaaaaaaa"))
    }

    @Test
    fun `espaces seuls refuses`() {
        assertEquals(PassphraseStrength.TOO_SHORT, PassphraseRules.strength("            "))
    }

    @Test
    fun `indicateur simple par longueur`() {
        assertEquals(PassphraseStrength.FAIR, PassphraseRules.strength("mon chat dort"))
        assertEquals(PassphraseStrength.STRONG, PassphraseRules.strength("le chat dort sur le canapé bleu"))
    }

    @Test
    fun `la phrase doit etre saisie deux fois a l identique`() {
        val phrase = "le chat dort sur le canapé"
        assertTrue(PassphraseRules.check(phrase, phrase).valid)
        assertFalse(PassphraseRules.check(phrase, "").valid)
        assertFalse(PassphraseRules.check(phrase, "$phrase.").valid)
        assertTrue(PassphraseRules.check(phrase, "$phrase.").mismatch)
        assertFalse(PassphraseRules.check(phrase, "").mismatch)
    }
}
