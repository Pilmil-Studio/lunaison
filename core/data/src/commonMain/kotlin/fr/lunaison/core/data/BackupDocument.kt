package fr.lunaison.core.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/*
 * Document JSON de la sauvegarde complète (contenu du fichier .pilmil avant chiffrement).
 * Il vit dans commonMain : la sérialisation se teste sur la JVM et se réutilisera sur iOS.
 *
 * {
 *   "format": "lunaison-backup",
 *   "version": 1,
 *   "schemaVersion": 7,
 *   "tables": { "day_logs": [ ... ], "settings": [ ... ] }
 * }
 *
 * `tables` reprend chaque table de la base Room, colonne par colonne. Le code PIN (`pinHash`) n'y figure jamais.
 */

/** Pourquoi une sauvegarde a été refusée. */
enum class BackupFailure {
    /** Pas du JSON, JSON tronqué, mauvais `format` ou données incohérentes. */
    INVALID,

    /** Document écrit par une version plus récente du format. */
    UNSUPPORTED_VERSION,
}

class BackupFormatException(val failure: BackupFailure, message: String) : Exception(message)

/** Ce qu'une sauvegarde contient : toutes les lignes de toutes les tables. */
data class BackupContent(val logs: List<DayLogEntity>, val settings: SettingsEntity?)

object LunaisonBackup {
    const val FORMAT = "lunaison-backup"
    const val VERSION = 1

    /** Version du schéma Room au moment de l'écriture (information, pas une condition de lecture). */
    const val SCHEMA_VERSION = 8

    /** Écrit le document ; le hachage du code PIN est retiré des réglages. */
    fun encode(content: BackupContent): String {
        val logs = content.logs.map { log ->
            linkedMapOf<String, Any?>(
                "date" to log.date,
                "flow" to log.flow,
                "pain" to log.pain,
                "symptoms" to log.symptoms,
                "moods" to log.moods,
                "note" to log.note,
                "medicationTaken" to log.medicationTaken,
                "mucus" to log.mucus,
                "basalTemperature" to log.basalTemperature,
                "pillStatus" to log.pillStatus,
                "medications" to log.medications,
                "weight" to log.weight,
                "intimacy" to log.intimacy,
                "libido" to log.libido,
            )
        }
        val settings = listOfNotNull(content.settings).map { s ->
            linkedMapOf<String, Any?>(
                "id" to s.id,
                "onboarded" to s.onboarded,
                "ageGroup" to s.ageGroup,
                "lastPeriodStart" to s.lastPeriodStart,
                "usualCycleLength" to s.usualCycleLength,
                "usualPeriodLength" to s.usualPeriodLength,
                "periodReminder" to s.periodReminder,
                "periodReminderDaysBefore" to s.periodReminderDaysBefore,
                "logReminder" to s.logReminder,
                "logReminderTime" to s.logReminderTime,
                "partnerName" to s.partnerName,
                "partnerPhone" to s.partnerPhone,
                "partnerChannel" to s.partnerChannel,
                "dismissedAlerts" to s.dismissedAlerts,
                "recapSeen" to s.recapSeen,
                "medicationReminder" to s.medicationReminder,
                "medicationReminderTime" to s.medicationReminderTime,
                "medicationLabel" to s.medicationLabel,
                "pausePredictions" to s.pausePredictions,
                "showFertility" to s.showFertility,
                "themePalette" to s.themePalette,
                "followedPartnerSnapshot" to s.followedPartnerSnapshot,
                "myShareName" to s.myShareName,
                "trackOvulationSigns" to s.trackOvulationSigns,
                "irregularCyclesMode" to s.irregularCyclesMode,
                "reviewPromptDismissed" to s.reviewPromptDismissed,
                "articleLastShown" to s.articleLastShown,
            )
        }
        val document = linkedMapOf<String, Any?>(
            "format" to FORMAT,
            "version" to VERSION,
            "schemaVersion" to SCHEMA_VERSION,
            "tables" to linkedMapOf<String, Any?>("day_logs" to logs, "settings" to settings),
        )
        return BackupJson.write(document)
    }

    /**
     * Lit et valide le document en entier avant de rendre quoi que ce soit : l'appelant ne modifie la base
     * qu'avec un contenu déjà vérifié.
     */
    fun decode(json: String): BackupContent {
        val parsed = try {
            BackupJson.parse(json)
        } catch (e: BackupJson.ParseException) {
            throw invalid("JSON illisible ou tronqué : ${e.message}")
        }
        val root = parsed as? Map<*, *> ?: throw invalid("Le document n'est pas un objet JSON")
        if (root["format"] != FORMAT) throw invalid("Ce fichier n'est pas une sauvegarde Lunaison")
        val version = (root["version"] as? BackupJson.Number)?.asInt() ?: throw invalid("Version absente")
        if (version != VERSION) {
            throw BackupFormatException(BackupFailure.UNSUPPORTED_VERSION, "Version de sauvegarde inconnue : $version")
        }
        val tables = root["tables"] as? Map<*, *> ?: throw invalid("Tables absentes")
        val logRows = tables["day_logs"] as? List<*> ?: throw invalid("Table day_logs absente")
        val settingsRows = tables["settings"] as? List<*> ?: throw invalid("Table settings absente")
        if (settingsRows.size > 1) throw invalid("La table settings n'a qu'une ligne")

        val logs = logRows.map { row -> Row(row as? Map<*, *> ?: throw invalid("Ligne day_logs invalide")).toLog() }
        if (logs.map { it.date }.toSet().size != logs.size) throw invalid("Dates en double dans day_logs")
        val settings = settingsRows.firstOrNull()?.let { Row(it as? Map<*, *> ?: throw invalid("Ligne settings invalide")).toSettings() }
        return BackupContent(logs, settings)
    }

    private fun invalid(message: String) = BackupFormatException(BackupFailure.INVALID, message)

    private class Row(private val map: Map<*, *>) {
        fun string(key: String): String = map[key] as? String ?: throw invalid("Champ $key manquant ou invalide")
        fun stringOrNull(key: String): String? = when (val v = map[key]) {
            null -> null
            is String -> v
            else -> throw invalid("Champ $key invalide")
        }
        fun bool(key: String): Boolean = map[key] as? Boolean ?: throw invalid("Champ $key manquant ou invalide")
        fun int(key: String): Int = (map[key] as? BackupJson.Number)?.asInt() ?: throw invalid("Champ $key manquant ou invalide")
        fun intOrNull(key: String): Int? = when (val v = map[key]) {
            null -> null
            is BackupJson.Number -> v.asInt() ?: throw invalid("Champ $key invalide")
            else -> throw invalid("Champ $key invalide")
        }
        fun floatOrNull(key: String): Float? = when (val v = map[key]) {
            null -> null
            is BackupJson.Number -> v.asFloat() ?: throw invalid("Champ $key invalide")
            else -> throw invalid("Champ $key invalide")
        }
        fun dateOrNull(key: String): String? = stringOrNull(key)?.also { checkDate(key, it) }
        fun checkDate(key: String, value: String) {
            runCatching { LocalDate.parse(value) }.getOrNull() ?: throw invalid("Date invalide dans $key")
        }

        fun toLog(): DayLogEntity {
            val date = string("date").also { checkDate("date", it) }
            return DayLogEntity(
                date = date,
                flow = stringOrNull("flow"),
                pain = stringOrNull("pain"),
                symptoms = string("symptoms"),
                moods = string("moods"),
                note = string("note"),
                medicationTaken = bool("medicationTaken"),
                mucus = stringOrNull("mucus"),
                basalTemperature = floatOrNull("basalTemperature"),
                pillStatus = stringOrNull("pillStatus"),
                medications = string("medications"),
                weight = floatOrNull("weight"),
                intimacy = string("intimacy"),
                libido = stringOrNull("libido"),
            )
        }

        fun toSettings(): SettingsEntity {
            val reminderTime = string("logReminderTime").also { checkTime("logReminderTime", it) }
            val medicationTime = string("medicationReminderTime").also { checkTime("medicationReminderTime", it) }
            return SettingsEntity(
                id = 0,
                onboarded = bool("onboarded"),
                ageGroup = string("ageGroup"),
                lastPeriodStart = dateOrNull("lastPeriodStart"),
                usualCycleLength = intOrNull("usualCycleLength"),
                usualPeriodLength = intOrNull("usualPeriodLength"),
                periodReminder = bool("periodReminder"),
                periodReminderDaysBefore = int("periodReminderDaysBefore"),
                logReminder = string("logReminder"),
                logReminderTime = reminderTime,
                partnerName = stringOrNull("partnerName"),
                partnerPhone = stringOrNull("partnerPhone"),
                partnerChannel = stringOrNull("partnerChannel"),
                dismissedAlerts = string("dismissedAlerts"),
                recapSeen = dateOrNull("recapSeen"),
                medicationReminder = bool("medicationReminder"),
                medicationReminderTime = medicationTime,
                medicationLabel = string("medicationLabel"),
                pausePredictions = bool("pausePredictions"),
                showFertility = bool("showFertility"),
                themePalette = string("themePalette"),
                pinHash = null,
                followedPartnerSnapshot = stringOrNull("followedPartnerSnapshot"),
                myShareName = string("myShareName"),
                trackOvulationSigns = bool("trackOvulationSigns"),
                irregularCyclesMode = bool("irregularCyclesMode"),
                reviewPromptDismissed = bool("reviewPromptDismissed"),
                articleLastShown = stringOrNull("articleLastShown") ?: "",
            )
        }

        private fun checkTime(key: String, value: String) {
            runCatching { LocalTime.parse(value) }.getOrNull() ?: throw invalid("Heure invalide dans $key")
        }
    }
}

/**
 * Lecteur et écrivain JSON minimal (pas de dépendance de sérialisation dans le projet).
 * Les objets deviennent des `Map`, les tableaux des `List`, les nombres des [Number] gardant leur texte.
 */
private object BackupJson {
    class ParseException(message: String) : Exception(message)

    /** Nombre JSON : le texte d'origine est conservé pour ne rien arrondir avant de savoir si on veut un `Int` ou un `Float`. */
    class Number(private val text: String) {
        fun asInt(): Int? = text.toIntOrNull()
        fun asFloat(): Float? = text.toFloatOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }

    private const val MAX_DEPTH = 32

    fun write(value: Any?): String = buildString { writeValue(value) }

    private fun StringBuilder.writeValue(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value.toString())
            is Int -> append(value.toString())
            is Float -> append(if (value.isNaN() || value.isInfinite()) "null" else value.toString())
            is String -> writeString(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) append(',')
                    first = false
                    writeString(k as String)
                    append(':')
                    writeValue(v)
                }
                append('}')
            }
            is List<*> -> {
                append('[')
                value.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    writeValue(v)
                }
                append(']')
            }
            else -> error("Type non pris en charge : ${value::class}")
        }
    }

    private fun StringBuilder.writeString(s: String) {
        append('"')
        for (c in s) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> append(c)
            }
        }
        append('"')
    }

    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.readValue(0)
        parser.skipWhitespace()
        if (!parser.atEnd()) throw ParseException("texte après la fin du document")
        return value
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd() = i >= s.length

        fun skipWhitespace() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        private fun peek(): Char {
            if (i >= s.length) throw ParseException("fin de document inattendue")
            return s[i]
        }

        private fun expect(c: Char) {
            if (peek() != c) throw ParseException("« $c » attendu à la position $i")
            i++
        }

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw ParseException("imbrication trop profonde")
            skipWhitespace()
            return when (val c = peek()) {
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c in '0'..'9') readNumber() else throw ParseException("caractère inattendu à la position $i")
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!s.startsWith(word, i)) throw ParseException("valeur inconnue à la position $i")
            i += word.length
            return value
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') { i++; return map }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                map[key] = readValue(depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    '}' -> { i++; return map }
                    else -> throw ParseException("« , » ou « } » attendu à la position $i")
                }
            }
        }

        private fun readArray(depth: Int): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') { i++; return list }
            while (true) {
                list += readValue(depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    ']' -> { i++; return list }
                    else -> throw ParseException("« , » ou « ] » attendu à la position $i")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                val c = peek()
                i++
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        when (val e = peek()) {
                            '"', '\\', '/' -> out.append(e)
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'u' -> {
                                if (i + 5 > s.length) throw ParseException("séquence \\u tronquée")
                                val code = s.substring(i + 1, i + 5).toIntOrNull(16) ?: throw ParseException("séquence \\u invalide")
                                out.append(code.toChar())
                                i += 4
                            }
                            else -> throw ParseException("séquence d'échappement inconnue à la position $i")
                        }
                        i++
                    }
                    else -> {
                        if (c < ' ') throw ParseException("caractère de contrôle dans une chaîne")
                        out.append(c)
                    }
                }
            }
        }

        private fun readNumber(): Number {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i] in '0'..'9' || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++
            val text = s.substring(start, i)
            if (text.toDoubleOrNull() == null) throw ParseException("nombre invalide à la position $start")
            return Number(text)
        }
    }
}
