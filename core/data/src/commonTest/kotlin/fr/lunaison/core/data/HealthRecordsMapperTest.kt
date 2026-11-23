package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.of
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Conversion Health Connect -> journées Lunaison, avec des enregistrements factices (pas d'API réelle). */
class HealthRecordsMapperTest {
    private fun at(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0) = LocalDateTime(y, m, d, h, min, 0, 0)

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    private fun map(periods: List<HealthPeriodRecord> = emptyList(), flows: List<HealthFlowRecord> = emptyList()): List<DayLog> =
        HealthRecordsMapper.toDayLogs(HealthMenstruationData(periods, flows))

    @Test
    fun `une periode sans flux donne des jours de flux moyen`() {
        val logs = map(periods = listOf(HealthPeriodRecord(at(2026, 3, 1, 8), at(2026, 3, 4, 20))))
        assertEquals(listOf(day(2026, 3, 1), day(2026, 3, 2), day(2026, 3, 3), day(2026, 3, 4)), logs.map { it.date })
        assertTrue(logs.all { it.flow == Flow.MEDIUM })
    }

    @Test
    fun `un enregistrement de flux precise l'intensite du jour`() {
        val logs = map(
            periods = listOf(HealthPeriodRecord(at(2026, 3, 1), at(2026, 3, 3, 23, 59))),
            flows = listOf(
                HealthFlowRecord(at(2026, 3, 1), HealthFlowLevel.HEAVY),
                HealthFlowRecord(at(2026, 3, 2), HealthFlowLevel.LIGHT),
            ),
        )
        assertEquals(listOf(Flow.HEAVY, Flow.LIGHT, Flow.MEDIUM), logs.map { it.flow })
    }

    @Test
    fun `plusieurs flux le meme jour gardent le plus fort et l'inconnu ne l'emporte pas`() {
        val logs = map(
            flows = listOf(
                HealthFlowRecord(at(2026, 3, 1, 8), HealthFlowLevel.LIGHT),
                HealthFlowRecord(at(2026, 3, 1, 18), HealthFlowLevel.MEDIUM),
                HealthFlowRecord(at(2026, 3, 1, 20), HealthFlowLevel.UNKNOWN),
            ),
        )
        assertEquals(1, logs.size)
        assertEquals(Flow.MEDIUM, logs.single().flow)
    }

    @Test
    fun `un flux hors de toute periode compte comme un jour de flux`() {
        val logs = map(flows = listOf(HealthFlowRecord(at(2026, 4, 9), HealthFlowLevel.UNKNOWN)))
        assertEquals(listOf(day(2026, 4, 9)), logs.map { it.date })
        assertEquals(Flow.MEDIUM, logs.single().flow)
    }

    @Test
    fun `une fin a minuit pile est exclusive`() {
        val logs = map(periods = listOf(HealthPeriodRecord(at(2026, 3, 1, 0), at(2026, 3, 4, 0))))
        assertEquals(listOf(day(2026, 3, 1), day(2026, 3, 2), day(2026, 3, 3)), logs.map { it.date })
    }

    @Test
    fun `une periode d'un seul instant donne un jour, meme si la fin precede le debut`() {
        val same = map(periods = listOf(HealthPeriodRecord(at(2026, 3, 1, 0), at(2026, 3, 1, 0))))
        assertEquals(listOf(day(2026, 3, 1)), same.map { it.date })
        val inverted = map(periods = listOf(HealthPeriodRecord(at(2026, 3, 5), at(2026, 3, 2))))
        assertEquals(listOf(day(2026, 3, 5)), inverted.map { it.date })
    }

    @Test
    fun `une periode aberrante est tronquee`() {
        val logs = map(periods = listOf(HealthPeriodRecord(at(2026, 1, 1), at(2026, 12, 31))))
        assertEquals(HealthRecordsMapper.MAX_PERIOD_DAYS, logs.size)
    }

    @Test
    fun `aucune donnee ne donne aucun jour et l'apercu compte les cycles`() {
        assertTrue(map().isEmpty())
        val data = HealthMenstruationData(
            periods = listOf(
                HealthPeriodRecord(at(2026, 1, 1), at(2026, 1, 5)),
                HealthPeriodRecord(at(2026, 1, 29), at(2026, 2, 2)),
            ),
            flows = emptyList(),
        )
        val preview = HealthRecordsMapper.toPreview(data)
        assertEquals(ImportSource.HEALTH_CONNECT, preview.source)
        assertEquals(10, preview.daysCount)
        assertEquals(2, preview.cyclesCount)
        assertEquals(day(2026, 1, 1), preview.firstDate)
        assertEquals(day(2026, 2, 2), preview.lastDate)
    }
}
