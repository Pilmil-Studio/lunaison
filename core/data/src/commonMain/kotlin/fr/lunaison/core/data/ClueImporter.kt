package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.daysTo
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Ce que le parseur a trouvé dans un ou plusieurs fichiers JSON. [skippedCount] : dates illisibles écartées. */
data class ClueParseResult(
    val dayLogs: List<DayLog>,
    val skippedCount: Int,
    /** Nombre de textes qui étaient du JSON valide. */
    val validDocuments: Int,
)

/**
 * Lecteur TOLÉRANT d'un export Clue (JSON de l'archive, ou fichier `.cluedata`).
 *
 * Le schéma réel n'étant pas connu, aucune classe rigide : le JSON est parcouru en entier et on y cherche
 * des jours de saignement, grâce aux hypothèses réunies dans [ClueKeyMapping]. Tout le reste (réglages,
 * symptômes, humeurs, clés inconnues, valeurs de type inattendu, dates invalides) est ignoré sans erreur.
 *
 * Formes reconnues (exemples, combinables) :
 *  - `{"2024-03-05": {"period": "heavy"}}` : date en clé ;
 *  - `[{"date": "2024-03-05", "period": {"value": "medium"}}]` : date dans l'entrée ;
 *  - `[{"date": "2024-03-05", "type": "period", "value": {"option": "light"}}]` : entrée typée ;
 *  - `{"date": "2024-03-05", "tags": ["period_heavy", "cramps"]}` : liste d'étiquettes ;
 *  - `{"period": [{"start": "2024-03-05", "end": "2024-03-09"}]}` : intervalle ;
 *  - `{"period": ["2024-03-05", "2024-03-06"]}` : liste de dates.
 *
 * Plusieurs entrées pour un même jour : la plus forte intensité l'emporte.
 */
object ClueImporter {
    private const val MAX_DEPTH = 40
    private const val MAX_RANGE_DAYS = 31

    private val json = Json

    fun parse(texts: List<String>): ClueParseResult {
        val collector = Collector()
        var valid = 0
        for (text in texts) {
            val element = parseElement(text) ?: continue
            valid++
            collector.walk(element, Context(), 0)
        }
        val logs = collector.days.toSortedMap().map { (date, flow) -> DayLog(date = date, flow = flow) }
        return ClueParseResult(logs, collector.skipped, valid)
    }

    fun parse(text: String): ClueParseResult = parse(listOf(text))

    /**
     * Aperçu d'un export. [UnreadableImportException] si aucun texte n'est du JSON ;
     * [NothingToImportException] s'il n'y a aucun jour de règles.
     */
    fun toPreview(texts: List<String>): ImportPreview {
        val result = parse(texts)
        if (result.validDocuments == 0) throw UnreadableImportException()
        if (result.dayLogs.isEmpty()) throw NothingToImportException()
        return ImportPreview.of(ImportSource.CLUE, result.dayLogs, result.skippedCount)
    }

    private fun parseElement(raw: String): JsonElement? {
        val text = raw.removePrefix("﻿").trim()
        if (text.isEmpty()) return null
        return try {
            json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Ce qu'on sait de l'endroit où l'on se trouve dans le JSON. */
    private data class Context(
        val date: LocalDate? = null,
        /** On est sous une clé de saignement. */
        val inBleeding: Boolean = false,
        /** Intensité annoncée par une clé parente (« heavy_period »). */
        val hint: Flow? = null,
        /** L'élément est dans une liste : une chaîne y est une étiquette potentielle. */
        val inList: Boolean = false,
    )

    private class Collector {
        val days = mutableMapOf<LocalDate, Flow>()
        var skipped = 0

        fun record(date: LocalDate, flow: Flow?) {
            val value = flow ?: Flow.MEDIUM
            val known = days[date]
            if (known == null || value.ordinal > known.ordinal) days[date] = value
        }

        fun walk(element: JsonElement, ctx: Context, depth: Int) {
            if (depth > MAX_DEPTH) return
            when (element) {
                is JsonObject -> walkObject(element, ctx, depth)
                is JsonArray -> element.forEach { walk(it, ctx.copy(inList = true), depth + 1) }
                is JsonPrimitive -> walkPrimitive(element, ctx)
            }
        }

        private fun walkPrimitive(p: JsonPrimitive, ctx: Context) {
            if (p is JsonNull) return
            if (p.isString) {
                val text = p.content
                val asDate = ClueKeyMapping.readDate(text)
                when {
                    ctx.inBleeding && ctx.date == null && asDate.date != null -> record(asDate.date, ctx.hint)
                    ctx.inBleeding && ctx.date == null && asDate.invalid -> skipped++
                    ctx.inBleeding && ctx.date != null -> recordWord(ctx.date, text, ctx.hint)
                    ctx.inList && ctx.date != null && ClueKeyMapping.isBleedingKey(text) ->
                        record(ctx.date, ClueKeyMapping.intensityOf(text) ?: ctx.hint)
                }
                return
            }
            if (!ctx.inBleeding) return
            val bool = p.content.lowercase()
            val number = p.doubleOrNull
            when {
                bool == "true" && ctx.date != null -> record(ctx.date, ctx.hint)
                number != null && ctx.date != null -> ClueKeyMapping.intensityOfNumber(number)?.let { record(ctx.date, it) }
                number != null && ctx.date == null -> p.longOrNull?.let { epoch ->
                    ClueKeyMapping.readEpoch(epoch).date?.let { record(it, ctx.hint) }
                }
            }
        }

        /** Texte sous une clé de saignement, pour un jour connu : intensité nommée ou simple « oui ». */
        private fun recordWord(date: LocalDate, text: String, hint: Flow?) {
            val flow = ClueKeyMapping.intensityOf(text)
            if (flow != null) {
                record(date, flow)
            } else if (ClueKeyMapping.isAffirmative(text)) {
                record(date, hint)
            }
        }

        private fun walkObject(obj: JsonObject, ctx: Context, depth: Int) {
            val own = readOwnDate(obj)
            val date = own.date ?: ctx.date
            val typed = typedBleeding(obj)
            val concerned = ctx.inBleeding || typed != null || obj.keys.any { ClueKeyMapping.isBleedingKey(it) }
            if (own.date == null && own.invalid && concerned) skipped++

            if ((ctx.inBleeding || typed != null) && !isNegative(obj)) {
                val hint = typed ?: ctx.hint
                if (recordRange(obj, hint)) return
                // Une entrée datée rangée sous une clé de saignement, ou typée « saignement », est un jour de règles.
                val day = own.date ?: date.takeIf { typed != null }
                if (day != null) {
                    record(day, ClueKeyMapping.intensityOf(typedText(obj)) ?: findIntensity(obj, 0) ?: ctx.hint)
                }
            }

            for ((key, value) in obj) {
                val keyRead = ClueKeyMapping.readDate(key)
                val keyIsDate = key.any { it == '-' || it == '/' }
                if (keyIsDate && keyRead.invalid && concerned) skipped++
                val child = when {
                    keyIsDate && keyRead.date != null -> ctx.copy(date = keyRead.date, inList = false)
                    ClueKeyMapping.isBleedingKey(key) ->
                        ctx.copy(date = date, inBleeding = true, hint = ClueKeyMapping.intensityOf(key) ?: ctx.hint, inList = false)
                    ClueKeyMapping.notBleedingWords.any { it in key.lowercase() } -> ctx.copy(date = date, inBleeding = false, inList = false)
                    else -> ctx.copy(date = date, inList = false)
                }
                walk(value, child, depth + 1)
            }
        }

        private class OwnDate(val date: LocalDate?, val invalid: Boolean)

        /** Date portée par l'objet lui-même (première clé de [ClueKeyMapping.dateKeys] lisible). */
        private fun readOwnDate(obj: JsonObject): OwnDate {
            var invalid = false
            for (key in ClueKeyMapping.dateKeys) {
                val value = obj[key] as? JsonPrimitive ?: continue
                val read = if (value.isString) ClueKeyMapping.readDate(value.content) else value.longOrNull?.let { ClueKeyMapping.readEpoch(it) }
                if (read?.date != null) return OwnDate(read.date, false)
                if (read?.invalid == true) invalid = true
            }
            return OwnDate(null, invalid)
        }

        /** Une entrée qui dit explicitement « pas de règles » (« none », false, 0) ne compte pas. */
        private fun isNegative(obj: JsonObject): Boolean =
            ClueKeyMapping.valueKeys.any { key ->
                val v = obj[key] as? JsonPrimitive ?: return@any false
                v.content.lowercase() in setOf("none", "no", "false", "0", "null") && v !is JsonNull
            }

        /** Texte du champ de type qui nomme un saignement, ou null. Renvoie l'intensité annoncée (ou MEDIUM sans précision). */
        private fun typedBleeding(obj: JsonObject): Flow? {
            val text = typedText(obj)
            if (text.isEmpty() || !ClueKeyMapping.isBleedingKey(text)) return null
            return ClueKeyMapping.intensityOf(text) ?: Flow.MEDIUM
        }

        private fun typedText(obj: JsonObject): String {
            for (key in ClueKeyMapping.typeKeys) {
                val value = obj[key]
                val text = when (value) {
                    is JsonPrimitive -> if (value.isString) value.content else ""
                    is JsonArray -> value.filterIsInstance<JsonPrimitive>().filter { it.isString }.map { it.content }.firstOrNull { ClueKeyMapping.isBleedingKey(it) }.orEmpty()
                    else -> ""
                }
                if (text.isNotEmpty() && ClueKeyMapping.isBleedingKey(text)) return text
            }
            return ""
        }

        /** Cherche une intensité dans les champs de valeur d'une entrée (chaîne, nombre 1 à 3, ou objet imbriqué). */
        private fun findIntensity(element: JsonElement, depth: Int): Flow? {
            if (depth > 5) return null
            return when (element) {
                is JsonPrimitive ->
                    if (element.isString) ClueKeyMapping.intensityOf(element.content) else element.doubleOrNull?.let { ClueKeyMapping.intensityOfNumber(it) }
                is JsonArray -> element.firstNotNullOfOrNull { findIntensity(it, depth + 1) }
                is JsonObject -> ClueKeyMapping.valueKeys.firstNotNullOfOrNull { key -> element[key]?.let { findIntensity(it, depth + 1) } }
            }
        }

        /** Intervalle `{start, end}` : un jour de saignement par jour couvert. Renvoie true s'il a été reconnu. */
        private fun recordRange(obj: JsonObject, hint: Flow?): Boolean {
            val start = firstDate(obj, ClueKeyMapping.rangeStartKeys) ?: return false
            val end = firstDate(obj, ClueKeyMapping.rangeEndKeys) ?: return false
            if (end < start) {
                skipped++
                return true
            }
            val length = minOf(start.daysTo(end) + 1, MAX_RANGE_DAYS)
            val flow = hint ?: findIntensity(obj, 0)
            repeat(length) { record(start.plusDays(it), flow) }
            return true
        }

        private fun firstDate(obj: JsonObject, keys: List<String>): LocalDate? {
            for (key in keys) {
                val value = obj[key] as? JsonPrimitive ?: continue
                if (!value.isString) continue
                val read = ClueKeyMapping.readDate(value.content)
                if (read.date != null) return read.date
                if (read.invalid) skipped++
            }
            return null
        }
    }
}
