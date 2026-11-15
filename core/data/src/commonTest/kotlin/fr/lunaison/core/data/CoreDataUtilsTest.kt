package fr.lunaison.core.data

import fr.lunaison.core.cycle.AlertKind
import fr.lunaison.core.model.of
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreDataUtilsTest {

    private fun ByteArray.toHex(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test
    fun `Sha256 correspond aux vecteurs de test NIST`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.digest(byteArrayOf()).toHex(),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.digest("abc".encodeToByteArray()).toHex(),
        )
        // Frontière de bloc à 56 octets (nécessite 2 blocs de padding)
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).toHex(),
        )
    }

    @Test
    fun `AppSettings hashPin et isDismissed respectent le delai de 60 jours`() {
        val h1 = AppSettings.hashPin("1234")
        val h2 = AppSettings.hashPin("1234")
        val h3 = AppSettings.hashPin("4321")
        assertTrue(h1.startsWith("pbkdf2$"))
        assertTrue(h1 != h2)
        assertTrue(h1 != h3)
        assertTrue(AppSettings.verifyPin("1234", h1))
        assertTrue(AppSettings.verifyPin("1234", h2))
        assertFalse(AppSettings.verifyPin("4321", h1))
        assertTrue(AppSettings.verifyPin("4321", h3))

        val dismissedAt = LocalDate.of(2026, 1, 1)
        val settings = AppSettings(dismissedAlerts = mapOf(AlertKind.IRREGULAR to dismissedAt))
        assertTrue(settings.isDismissed(AlertKind.IRREGULAR, dismissedAt.plusDays(60)))
        assertFalse(settings.isDismissed(AlertKind.IRREGULAR, dismissedAt.plusDays(61)))
        assertFalse(settings.isDismissed(AlertKind.SEVERE_PAIN, dismissedAt))
    }

    @Test
    fun `toOneDecimal formate avec un point quelle que soit la locale`() {
        assertEquals("36.6", 36.6f.toOneDecimal())
        assertEquals("0.0", 0.0f.toOneDecimal())
        assertEquals("-1.5", (-1.5f).toOneDecimal())
        assertEquals("37.0", 36.96f.toOneDecimal())
        assertEquals("NaN", Float.NaN.toOneDecimal())
        assertEquals("Infinity", Float.POSITIVE_INFINITY.toOneDecimal())
    }

    @Test
    fun `PinThrottle normalize ne rebloque pas un blocage deja expire avant redemarrage`() {
        val expired = PinThrottleState(
            failures = 5,
            blockedUntil = 50_000L,
            savedAt = 60_000L,
            lastDelayMs = 30_000L,
        )
        val normalized = PinThrottle.normalize(expired, now = 55_000L)
        assertFalse(PinThrottle.isBlocked(normalized, 55_000L))
        assertEquals(0L, normalized.blockedUntil)
        assertEquals(55_000L, normalized.savedAt)
    }

    @Test
    fun `AppSettings verifyPin refuse les empreintes malformees`() {
        assertFalse(AppSettings.verifyPin("1234", ""))
        assertFalse(AppSettings.verifyPin("1234", "pbkdf2\$not_hex\$abcd"))
        assertFalse(AppSettings.verifyPin("1234", "pbkdf2\$0011"))
        assertFalse(AppSettings.verifyPin("", AppSettings.hashPin("1234")))
    }
}
