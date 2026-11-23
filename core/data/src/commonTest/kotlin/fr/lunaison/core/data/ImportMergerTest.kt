package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.of
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportMergerTest {
    private fun d(day: Int) = LocalDate.of(2026, 3, day)

    private fun preview(vararg days: Int) =
        ImportPreview.of(ImportSource.CLUE, days.map { DayLog(date = d(it), flow = Flow.HEAVY) })

    @Test
    fun `une journee deja saisie est conservee et les autres sont ajoutees`() {
        val existing = listOf(DayLog(date = d(2), flow = Flow.LIGHT), DayLog(date = d(9), pain = Pain.MILD))
        val plan = ImportMerger.plan(preview(1, 2, 3), existing)
        assertEquals(listOf(d(1), d(3)), plan.toWrite.map { it.date })
        assertEquals(1, plan.keptExisting)
    }

    @Test
    fun `une journee vide existante ne bloque pas l'import`() {
        val plan = ImportMerger.plan(preview(1), listOf(DayLog(date = d(1))))
        assertEquals(1, plan.toWrite.size)
        assertEquals(0, plan.keptExisting)
    }

    @Test
    fun `une base vide recoit tout`() {
        val plan = ImportMerger.plan(preview(1, 2, 3), emptyList())
        assertEquals(3, plan.toWrite.size)
    }
}
