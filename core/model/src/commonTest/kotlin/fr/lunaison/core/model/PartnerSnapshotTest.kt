package fr.lunaison.core.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnerSnapshotTest {

    private val today = LocalDate.of(2026, 6, 15)

    private fun sampleSnapshot() = PartnerSnapshot(
        partnerName = "Camille",
        sharedAt = today,
        periods = listOf(
            PartnerPeriod(LocalDate.of(2026, 4, 10), 5),
            PartnerPeriod(LocalDate.of(2026, 5, 8), 4),
            PartnerPeriod(LocalDate.of(2026, 6, 5), 5),
        ),
        usualCycleLength = 28,
        showFertility = true,
    )

    @Test
    fun `encode et decode v2 preservent toutes les donnees`() {
        val original = sampleSnapshot()
        val code = original.encode()
        assertTrue(code.startsWith(PartnerSnapshot.PREFIX))

        val decoded = PartnerSnapshot.decode("Coucou voici mon cycle : $code !", today = today)
        assertEquals(original, decoded)
    }

    @Test
    fun `decode accepte un code v1 valide sans checksum`() {
        val p1 = LocalDate.of(2026, 5, 10).toEpochDay()
        val payload = "v1|Alex|${today.toEpochDay()}|29|0|$p1:5"
        val code = PartnerSnapshot.PREFIX + Base64Url.encode(payload.encodeToByteArray())

        val decoded = PartnerSnapshot.decode(code, today = today)
        assertNotNull(decoded)
        assertEquals("Alex", decoded.partnerName)
        assertEquals(29, decoded.usualCycleLength)
        assertFalse(decoded.showFertility)
        assertEquals(listOf(PartnerPeriod(LocalDate.of(2026, 5, 10), 5)), decoded.periods)
    }

    @Test
    fun `decode refuse code v2 altere tronque ou trop long`() {
        val code = sampleSnapshot().encode()
        // Altération d'un caractère dans le Base64
        val tampered = code.dropLast(1) + if (code.last() == 'A') 'B' else 'A'
        assertNull(PartnerSnapshot.decode(tampered, today = today))

        // Troncature
        assertNull(PartnerSnapshot.decode(code.dropLast(4), today = today))

        // Dépassement de MAX_RAW_LENGTH
        val tooLong = "x".repeat(PartnerSnapshot.MAX_RAW_LENGTH + 1) + code
        assertNull(PartnerSnapshot.decode(tooLong, today = today))

        // Absence de préfixe
        assertNull(PartnerSnapshot.decode("bonjour sans code", today = today))
    }

    @Test
    fun `decode nettoie le nom et utilise fallbackName si vide`() {
        val noName = sampleSnapshot().copy(partnerName = "|\u200B  ")
        val code = noName.encode()

        val withFallback = PartnerSnapshot.decode(code, fallbackName = "Sam", today = today)
        assertEquals("Sam", withFallback?.partnerName)

        val withDefault = PartnerSnapshot.decode(code, fallbackName = null, today = today)
        assertNotNull(withDefault)
        assertTrue(withDefault.partnerName.isNotBlank())
    }

    @Test
    fun `decode refuse dates hors bornes et trop de periodes`() {
        val tooFuture = sampleSnapshot().copy(sharedAt = today.plusDays(3))
        assertNull(PartnerSnapshot.decode(tooFuture.encode(), today = today))

        val tooOld = sampleSnapshot().copy(sharedAt = LocalDate.of(2019, 12, 31))
        assertNull(PartnerSnapshot.decode(tooOld.encode(), today = today))

        val sevenPeriods = (0 until 7).map { i ->
            "${today.minusDays((i * 28L + 5)).toEpochDay()}:5"
        }.joinToString(",")
        val body = "v2|Lou|${today.toEpochDay()}|28|0|$sevenPeriods"
        val crc = (Crc32.of(body.encodeToByteArray()) and 0xFFFF).toString(16).padStart(4, '0')
        val code = PartnerSnapshot.PREFIX + Base64Url.encode("$body|$crc".encodeToByteArray())
        assertNull(PartnerSnapshot.decode(code, today = today))
    }

    @Test
    fun `isPlausible verifie les limites du snapshot`() {
        assertTrue(sampleSnapshot().isPlausible(today))
        assertFalse(sampleSnapshot().copy(sharedAt = today.plusDays(5)).isPlausible(today))
        assertFalse(
            sampleSnapshot().copy(
                periods = listOf(PartnerPeriod(today.minusYears(6), 5)),
            ).isPlausible(today),
        )
        assertFalse(
            sampleSnapshot().copy(
                periods = listOf(PartnerPeriod(today, 16)),
            ).isPlausible(today),
        )
    }

    @Test
    fun `mergeWith remplace les regles proches ajoute les nouvelles et garde les 6 dernieres`() {
        val base = PartnerSnapshot(
            partnerName = "Camille",
            sharedAt = LocalDate.of(2026, 5, 1),
            periods = (1..5).map { m -> PartnerPeriod(LocalDate.of(2026, m, 1), 4) },
            usualCycleLength = 28,
        )
        val update = PartnerSnapshot(
            partnerName = "",
            sharedAt = LocalDate.of(2026, 7, 1),
            periods = listOf(
                PartnerPeriod(LocalDate.of(2026, 5, 2), 6), // à 1 jour du 1er mai -> remplace
                PartnerPeriod(LocalDate.of(2026, 6, 1), 5),
                PartnerPeriod(LocalDate.of(2026, 7, 1), 5),
            ),
            usualCycleLength = 29,
            showFertility = true,
        )
        val merged = base.mergeWith(update)
        assertEquals("Camille", merged.partnerName)
        assertEquals(6, merged.periods.size)
        assertEquals(LocalDate.of(2026, 2, 1), merged.periods.first().start)
        assertEquals(6, merged.periods.first { it.start == LocalDate.of(2026, 5, 2) }.length)
        assertEquals(LocalDate.of(2026, 7, 1), merged.periods.last().start)
        assertTrue(merged.showFertility)
    }

    @Test
    fun `toDayLogs et toBaseline reconstruisent les donnees du cycle`() {
        val snap = sampleSnapshot()
        val logs = snap.toDayLogs()
        assertEquals(14, logs.size)
        assertTrue(logs.all { it.flow == Flow.MEDIUM })

        val baseline = snap.toBaseline()
        assertEquals(LocalDate.of(2026, 6, 5), baseline.lastPeriodStart)
        assertEquals(28, baseline.usualCycleLength)
    }
}
