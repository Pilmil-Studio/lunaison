package fr.lunaison.ui.backup

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupPolicyTest {

    private val encrypted = BackupPolicy.FLAG_CLIENT_SIDE_ENCRYPTION
    private val transfer = BackupPolicy.FLAG_DEVICE_TO_DEVICE

    @Test
    fun `le nuage est interdit par defaut`() {
        assertFalse(BackupPolicy.allows(sdk = 34, transportFlags = encrypted, cloudBackupEnabled = false, passphraseSet = true))
    }

    @Test
    fun `le nuage est permis une fois active et chiffre de bout en bout`() {
        assertTrue(BackupPolicy.allows(sdk = 34, transportFlags = encrypted, cloudBackupEnabled = true, passphraseSet = true))
    }

    @Test
    fun `le nuage est refuse sans phrase secrete meme active et chiffre`() {
        assertFalse(BackupPolicy.allows(sdk = 34, transportFlags = encrypted, cloudBackupEnabled = true, passphraseSet = false))
    }

    @Test
    fun `le transfert direct ne depend pas de la phrase`() {
        assertTrue(BackupPolicy.allows(sdk = 34, transportFlags = transfer, cloudBackupEnabled = false, passphraseSet = false))
    }

    @Test
    fun `le reglage de copie Google demande Android 12, une phrase et un verrouillage d ecran`() {
        assertEquals(CloudCopyBlocker.UNSUPPORTED_ANDROID, BackupPolicy.blocker(sdk = 30, deviceSecure = true, passphraseSet = true))
        assertEquals(CloudCopyBlocker.NO_PASSPHRASE, BackupPolicy.blocker(sdk = 34, deviceSecure = true, passphraseSet = false))
        assertEquals(CloudCopyBlocker.NO_SCREEN_LOCK, BackupPolicy.blocker(sdk = 34, deviceSecure = false, passphraseSet = true))
        assertNull(BackupPolicy.blocker(sdk = 34, deviceSecure = true, passphraseSet = true))
        assertNull(BackupPolicy.blocker(sdk = 31, deviceSecure = true, passphraseSet = true))
    }

    @Test
    fun `le nuage est refuse sans chiffrement meme active`() {
        assertFalse(BackupPolicy.allows(sdk = 34, transportFlags = 0, cloudBackupEnabled = true, passphraseSet = true))
    }

    @Test
    fun `le transfert direct est toujours permis`() {
        assertTrue(BackupPolicy.allows(sdk = 31, transportFlags = transfer, cloudBackupEnabled = false, passphraseSet = true))
        assertTrue(BackupPolicy.allows(sdk = 34, transportFlags = transfer or encrypted, cloudBackupEnabled = false, passphraseSet = true))
    }

    @Test
    fun `avant Android 12 rien ne part`() {
        assertFalse(BackupPolicy.allows(sdk = 30, transportFlags = encrypted or transfer, cloudBackupEnabled = true, passphraseSet = true))
        assertFalse(BackupPolicy.settingAvailable(30))
        assertTrue(BackupPolicy.settingAvailable(31))
    }

    @Test
    fun `le choix est desactive au depart puis se retient`() {
        val dir = Files.createTempDirectory("sommelle").toFile().resolve("no_backup")
        val consent = CloudBackupConsent(dir)
        assertFalse(consent.enabled)
        consent.enabled = true
        assertTrue(CloudBackupConsent(dir).enabled)
        consent.enabled = false
        assertFalse(CloudBackupConsent(dir).enabled)
    }
}
