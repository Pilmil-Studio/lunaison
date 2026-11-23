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
    ImportSource.PERIOD_CALENDAR -> tr("Calendrier des règles", "Period Calendar", pl = "Kalendarz miesiączek", uk = "Календар менструацій", es = "Calendario menstrual", de = "Periodenkalender")
    ImportSource.LUNAISON_CSV -> "Lunaison (CSV)"
}

/** Message affiché quand le fichier a été lu mais ne contient aucune journée exploitable. */
internal fun importNothingFoundMessage(preview: ImportPreview): String {
    val base = tr(
        fr = "Aucune journée exploitable dans ce fichier. Rien n'a été modifié.",
        en = "No usable day in this file. Nothing was changed.",
        pl = "W tym pliku nie ma użytecznych dni. Nic nie zmieniono.",
        uk = "У цьому файлі немає придатних до імпорту днів. Нічого не змінено.",
        es = "No hay ningún día utilizable en este archivo. No se ha modificado nada.",
        de = "Keine verwertbaren Tage in dieser Datei. Es wurde nichts geändert.",
    )
    val n = preview.skippedCount
    if (n <= 0) return base
    return base + " " + tr(
        fr = "$n ligne(s) ignorée(s).",
        en = "$n line(s) skipped.",
        pl = "Pominięte wiersze: $n.",
        uk = "Пропущено рядків: $n.",
        es = "$n línea(s) omitida(s).",
        de = "$n Zeile(n) übersprungen.",
    )
}

/** Les sources qui ne contiennent que des jours de saignement (et pas un journal complet). */
private fun ImportSource.bleedingOnly(): Boolean = this == ImportSource.HEALTH_CONNECT || this == ImportSource.CLUE

/** Lignes de l'aperçu : ce qui a été trouvé et ce qui sera réellement écrit. Texte pur, testable. */
internal fun importPlanLines(plan: ImportPlan): List<String> {
    val preview: ImportPreview = plan.preview
    val lines = mutableListOf<String>()
    val days = preview.daysCount
    val cycles = preview.cyclesCount
    lines += if (preview.source.bleedingOnly()) {
        tr("Journées de règles trouvées : $days", "Days with bleeding found: $days", pl = "Znalezione dni z krwawieniem: $days", uk = "Знайдено днів із виділеннями: $days", es = "Días con sangrado encontrados: $days", de = "Gefundene Tage mit Blutung: $days")
    } else {
        tr("Journées trouvées : $days", "Days found: $days", pl = "Znalezione dni: $days", uk = "Знайдено днів: $days", es = "Días encontrados: $days", de = "Gefundene Tage: $days")
    }
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
    if (plan.fillsBaseline) {
        lines += tr(
            "Repères de cycle de la source : seuls les champs encore vides seront complétés",
            "Cycle settings from the source: only fields that are still empty will be filled in",
            pl = "Ustawienia cyklu ze źródła: uzupełnione zostaną tylko pola, które są jeszcze puste",
            uk = "Параметри циклу з джерела: буде заповнено лише ті поля, що ще порожні",
            es = "Datos del ciclo de la fuente: solo se completarán los campos que sigan vacíos",
            de = "Zyklusangaben aus der Quelle: Es werden nur noch leere Felder ergänzt",
        )
    }
    val skipped = preview.skippedCount
    if (skipped > 0 && preview.source == ImportSource.LUNAISON_CSV) {
        lines += tr(
            "Lignes ignorées (illisibles ou vides) : $skipped",
            "Skipped lines (unreadable or empty): $skipped",
            pl = "Pominięte wiersze (nieczytelne lub puste): $skipped",
            uk = "Пропущено рядків (нечитабельні або порожні): $skipped",
            es = "Líneas omitidas (ilegibles o vacías): $skipped",
            de = "Übersprungene Zeilen (unlesbar oder leer): $skipped",
        )
    } else if (skipped > 0) {
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
    val nothingNew = plan.isEmpty
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
                        fr = "Vos journées déjà notées ne sont jamais modifiées.",
                        en = "Days you already logged are never changed.",
                        pl = "Dni już zapisane w Lunaison nigdy nie są zmieniane.",
                        uk = "Дні, які ви вже записали, ніколи не змінюються.",
                        es = "Los días que ya anotaste nunca se modifican.",
                        de = "Bereits eingetragene Tage werden nie verändert.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
