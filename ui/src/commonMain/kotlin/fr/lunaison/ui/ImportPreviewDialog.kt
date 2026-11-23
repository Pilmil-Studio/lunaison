package fr.lunaison.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import fr.lunaison.core.data.ImportPlan
import fr.lunaison.core.data.ImportPreview
import fr.lunaison.core.data.ImportSource
import fr.lunaison.core.data.NothingToImportException
import fr.lunaison.core.data.UnreadableImportException
import fr.lunaison.core.data.UnsupportedEncryptionException
import fr.lunaison.core.model.tr
import kotlinx.datetime.LocalDate

private fun fullDate(date: LocalDate): String = "${Format.dayMonth(date)} ${date.year}"

internal fun importSourceName(source: ImportSource): String = when (source) {
    ImportSource.HEALTH_CONNECT -> "Health Connect"
    ImportSource.CLUE -> tr("Clue (expérimental)", "Clue (experimental)", pl = "Clue (eksperymentalne)", uk = "Clue (експериментально)", es = "Clue (experimental)", de = "Clue (experimentell)")
}

/** Lignes de l'aperçu : ce qui a été trouvé et ce qui sera réellement écrit. Texte pur, testable. */
internal fun importPlanLines(plan: ImportPlan): List<String> {
    val preview: ImportPreview = plan.preview
    val lines = mutableListOf<String>()
    val days = preview.daysCount
    val cycles = preview.cyclesCount
    lines += tr("Journées de règles trouvées : $days", "Days with bleeding found: $days", pl = "Znalezione dni z krwawieniem: $days", uk = "Знайдено днів із виділеннями: $days", es = "Días con sangrado encontrados: $days", de = "Gefundene Tage mit Blutung: $days")
    lines += tr("Cycles repérés : $cycles", "Cycles detected: $cycles", pl = "Wykryte cykle: $cycles", uk = "Виявлено циклів: $cycles", es = "Ciclos detectados: $cycles", de = "Erkannte Zyklen: $cycles")
    val first = preview.firstDate
    val last = preview.lastDate
    if (first != null && last != null) {
        val a = fullDate(first)
        val b = fullDate(last)
        lines += tr("Période couverte : du $a au $b", "Covered period: $a to $b", pl = "Okres: od $a do $b", uk = "Період: з $a по $b", es = "Periodo cubierto: del $a al $b", de = "Zeitraum: $a bis $b")
    }
    val kept = plan.keptExisting
    if (kept > 0) {
        lines += tr(
            "Journées déjà notées dans Lunaison, conservées telles quelles : $kept",
            "Days already logged in Lunaison, kept as they are: $kept",
            pl = "Dni już zapisane w Lunaison, pozostaną bez zmian: $kept",
            uk = "Дні, уже записані в Lunaison, залишаться без змін: $kept",
            es = "Días ya anotados en Lunaison, se conservan tal cual: $kept",
            de = "Bereits in Lunaison eingetragene Tage bleiben unverändert: $kept",
        )
    }
    val fresh = plan.toWrite.size
    lines += tr("Journées qui seront ajoutées : $fresh", "Days that will be added: $fresh", pl = "Dni, które zostaną dodane: $fresh", uk = "Дні, які буде додано: $fresh", es = "Días que se añadirán: $fresh", de = "Tage, die hinzugefügt werden: $fresh")
    val skipped = preview.skippedCount
    if (skipped > 0) {
        lines += tr(
            "Éléments ignorés (date illisible) : $skipped",
            "Ignored items (unreadable date): $skipped",
            pl = "Pominięte elementy (nieczytelna data): $skipped",
            uk = "Пропущено елементів (нечитабельна дата): $skipped",
            es = "Elementos omitidos (fecha ilegible): $skipped",
            de = "Übersprungene Einträge (unlesbares Datum): $skipped",
        )
    }
    return lines
}

internal fun importAppliedMessage(written: Int, kept: Int): String = tr(
    fr = "$written journées importées, $kept conservées telles quelles.",
    en = "$written days imported, $kept kept as they were.",
    pl = "Zaimportowano dni: $written, bez zmian: $kept.",
    uk = "Імпортовано днів: $written, без змін: $kept.",
    es = "$written días importados, $kept conservados tal cual.",
    de = "$written Tage importiert, $kept unverändert behalten.",
)

/** Message localisé pour les erreurs d'import avec aperçu, ou null si l'erreur est d'un autre type. */
internal fun importPreviewErrorMessage(e: Throwable): String? = when (e) {
    is UnsupportedEncryptionException -> tr(
        fr = "Format de chiffrement non pris en charge : décompressez l'archive puis importez le fichier JSON.",
        en = "Unsupported encryption format: extract the archive, then import the JSON file.",
        pl = "Nieobsługiwany format szyfrowania: rozpakuj archiwum, a następnie zaimportuj plik JSON.",
        uk = "Непідтримуваний формат шифрування: розпакуйте архів, а потім імпортуйте файл JSON.",
        es = "Formato de cifrado no compatible: descomprime el archivo y luego importa el archivo JSON.",
        de = "Verschlüsselungsformat wird nicht unterstützt: Entpacke das Archiv und importiere dann die JSON-Datei.",
    )
    is UnreadableImportException -> tr(
        fr = "Fichier illisible : ce n'est pas un export JSON valide.",
        en = "Unreadable file: this is not a valid JSON export.",
        pl = "Nie można odczytać pliku: to nie jest poprawny eksport JSON.",
        uk = "Файл не вдалося прочитати: це не дійсний експорт JSON.",
        es = "Archivo ilegible: no es una exportación JSON válida.",
        de = "Datei nicht lesbar: Das ist kein gültiger JSON-Export.",
    )
    is NothingToImportException -> tr(
        fr = "Aucune donnée de règles trouvée dans ce fichier.",
        en = "No period data found in this file.",
        pl = "W tym pliku nie znaleziono danych o miesiączkach.",
        uk = "У цьому файлі не знайдено даних про менструації.",
        es = "No se encontraron datos de menstruación en este archivo.",
        de = "In dieser Datei wurden keine Periodendaten gefunden.",
    )
    else -> null
}

/**
 * Aperçu avant écriture : nombre de journées et de cycles, période couverte, journées conservées.
 * Rien n'est écrit tant que la personne n'a pas touché « Importer ».
 */
@Composable
internal fun ImportPlanDialog(plan: ImportPlan, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val nothingNew = plan.toWrite.isEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Aperçu de l'import", "Import preview", pl = "Podgląd importu", uk = "Попередній перегляд імпорту", es = "Vista previa de la importación", de = "Vorschau des Imports")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("Source : ${importSourceName(plan.preview.source)}", "Source: ${importSourceName(plan.preview.source)}", pl = "Źródło: ${importSourceName(plan.preview.source)}", uk = "Джерело: ${importSourceName(plan.preview.source)}", es = "Origen: ${importSourceName(plan.preview.source)}", de = "Quelle: ${importSourceName(plan.preview.source)}"),
                    style = MaterialTheme.typography.titleSmall,
                )
                importPlanLines(plan).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (nothingNew) {
                    Text(
                        tr(
                            fr = "Rien de nouveau à importer : toutes ces journées sont déjà notées dans Lunaison.",
                            en = "Nothing new to import: all these days are already logged in Lunaison.",
                            pl = "Nic nowego do zaimportowania: wszystkie te dni są już zapisane w Lunaison.",
                            uk = "Нічого нового імпортувати: усі ці дні вже записані в Lunaison.",
                            es = "Nada nuevo que importar: todos estos días ya están anotados en Lunaison.",
                            de = "Nichts Neues zu importieren: Alle diese Tage sind bereits in Lunaison eingetragen.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (plan.preview.source == ImportSource.CLUE) {
                    Text(
                        tr(
                            fr = "Import expérimental : le format de Clue n'est pas documenté, vérifiez le résultat dans le calendrier.",
                            en = "Experimental import: Clue's format is not documented, check the result in the calendar.",
                            pl = "Import eksperymentalny: format Clue nie jest udokumentowany, sprawdź wynik w kalendarzu.",
                            uk = "Експериментальний імпорт: формат Clue не задокументований, перевірте результат у календарі.",
                            es = "Importación experimental: el formato de Clue no está documentado, revisa el resultado en el calendario.",
                            de = "Experimenteller Import: Das Format von Clue ist nicht dokumentiert, prüfe das Ergebnis im Kalender.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    tr(
                        fr = "Tout est traité sur votre téléphone, rien n'est envoyé ailleurs.",
                        en = "Everything is processed on your phone; nothing is sent anywhere.",
                        pl = "Wszystko jest przetwarzane na telefonie, nic nie jest nigdzie wysyłane.",
                        uk = "Усе обробляється на телефоні, нічого нікуди не надсилається.",
                        es = "Todo se procesa en tu teléfono; no se envía nada a ningún sitio.",
                        de = "Alles wird auf deinem Telefon verarbeitet; nichts wird irgendwohin gesendet.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (nothingNew) {
                TextButton(onClick = onDismiss) { Text(tr("Fermer", "Close", pl = "Zamknij", uk = "Закрити", es = "Cerrar", de = "Schließen")) }
            } else {
                TextButton(onClick = onConfirm) { Text(tr("Importer", "Import", pl = "Importuj", uk = "Імпортувати", es = "Importar", de = "Importieren")) }
            }
        },
        dismissButton = {
            if (!nothingNew) {
                TextButton(onClick = onDismiss) { Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen")) }
            }
        },
    )
}
