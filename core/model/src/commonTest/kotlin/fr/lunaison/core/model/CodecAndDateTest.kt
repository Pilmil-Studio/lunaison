package fr.lunaison.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodecAndDateTest {

    @Test
    fun `Base64Url encode et decode toutes les longueurs de reste`() {
        val samples = listOf(
            byteArrayOf(),
            byteArrayOf(0x00),
            byteArrayOf(0xFF.toByte(), 0x80.toByte()),
            "Lunaison-123!".encodeToByteArray(),
            ByteArray(37) { (it * 7 - 3).toByte() },
        )
        for (raw in samples) {
            val encoded = Base64Url.encode(raw)
            assertFalse('=' in encoded)
            assertEquals(raw.toList(), Base64Url.decode(encoded).toList())
        }
    }

    @Test
    fun `Base64Url refuse longueur impossible et caractere invalide`() {
        assertFailsWith<IllegalArgumentException> { Base64Url.decode("A") }
        assertFailsWith<IllegalArgumentException> { Base64Url.decode("ABCDE") }
        assertFailsWith<IllegalArgumentException> { Base64Url.decode("AB+D") }
        assertFailsWith<IllegalArgumentException> { Base64Url.decode("AB=D") }
    }

    @Test
    fun `Crc32 correspond au polynome IEEE 802_3`() {
        assertEquals(0L, Crc32.of(byteArrayOf()))
        assertEquals(0xCBF43926L, Crc32.of("123456789".encodeToByteArray()))
    }

    @Test
    fun `operations sur points de code ne coupent pas les paires de substitution`() {
        val emoji = "\uD83C\uDF19" // 🌙 (2 UTF-16 code units, 1 code point)
        val text = "A${emoji}B"
        assertEquals(3, text.codePointTotal())
        assertEquals("A$emoji", text.takeCodePoints(2))
        assertEquals("A$emoji", text.dropLastCodePoint())
        assertEquals("A", "A$emoji".dropLastCodePoint())
        assertEquals("", "".dropLastCodePoint())
    }

    @Test
    fun `withoutInvisibleCharacters retire controles format et remplace pipe`() {
        val raw = "Ali\u200Bce|Bob\u0000\n\uD83C\uDF19\uDB40\uDC20"
        assertEquals("Alice Bob\uD83C\uDF19", raw.withoutInvisibleCharacters())
    }

    @Test
    fun `DateCompat epochDay daysTo et LocalTime wrap-around`() {
        val d = LocalDate.of(2026, 3, 15)
        assertEquals(d, LocalDate.ofEpochDay(d.toEpochDay()))
        assertEquals(5, d.daysTo(d.plusDays(5)))
        assertEquals(-3, d.daysTo(d.minusDays(3)))

        assertEquals(LocalTime.of(1, 0), LocalTime.of(23, 0).plusHours(2))
        assertEquals(LocalTime.of(22, 0), LocalTime.of(1, 0).minusHours(3))
    }

    @Test
    fun `DayLog isEmpty detecte chaque champ renseigne`() {
        val date = LocalDate.of(2026, 4, 1)
        assertTrue(DayLog(date).isEmpty)
        assertTrue(DayLog(date, note = "   ").isEmpty)
        assertFalse(DayLog(date, flow = Flow.SPOTTING).isEmpty)
        assertFalse(DayLog(date, pain = Pain.NONE).isEmpty)
        assertFalse(DayLog(date, pillStatus = PillStatus.MISSED).isEmpty)
        assertFalse(DayLog(date, medications = setOf(MedicationKind.SUPPLEMENT)).isEmpty)
        assertFalse(DayLog(date, weight = 60.0f).isEmpty)
        assertFalse(DayLog(date, intimacy = setOf(IntimacyKind.PROTECTED)).isEmpty)
        assertFalse(DayLog(date, libido = Libido.NORMAL).isEmpty)
        assertFalse(DayLog(date, mucus = CervicalMucus.CREAMY).isEmpty)
        assertFalse(DayLog(date, basalTemperature = 36.5f).isEmpty)
    }
}
