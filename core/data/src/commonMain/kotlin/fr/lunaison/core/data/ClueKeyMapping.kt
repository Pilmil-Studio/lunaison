package fr.lunaison.core.data

import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.ofEpochDay
import kotlinx.datetime.LocalDate

/**
 * TOUT ce que Lunaison suppose sur le format d'export de Clue est ici, et nulle part ailleurs.
 *
 * Clue ne publie pas le schéma de son export et nous n'en avons pas d'échantillon réel : les listes
 * ci-dessous sont des hypothèses larges (noms de clés plausibles, en anglais). Quand un vrai export
 * sera disponible, il suffira de corriger ce fichier :
 *  - [dateKeys] : clés qui portent la date ou l'horodatage d'une entrée ;
 *  - [rangeStartKeys] et [rangeEndKeys] : début et fin d'une période de règles donnée comme intervalle ;
 *  - [typeKeys] : clés qui nomment le type d'une entrée (« period », « bleeding »...) ;
 *  - [valueKeys] : clés qui portent l'intensité d'une entrée typée ;
 *  - [bleedingWords] et [notBleedingWords] : mots qui font reconnaître (ou écarter) une clé de saignement ;
 *  - [intensityOf] et [intensityOfNumber] : conversion d'une intensité en [Flow].
 *
 * Les comparaisons se font en minuscules, sur des sous-chaînes.
 */
internal object ClueKeyMapping {
    /** Par ordre de priorité. */
    val dateKeys = listOf("date", "day", "day_date", "datetime", "timestamp", "time", "created_at", "at", "start_date", "start", "begin")

    val rangeStartKeys = listOf("start_date", "start", "from", "begin", "first_day")

    val rangeEndKeys = listOf("end_date", "end", "to", "until", "last_day")

    val typeKeys = listOf("type", "tag", "tags", "category", "kind", "name", "key", "id", "event", "measurement")

    val valueKeys = listOf("value", "values", "intensity", "level", "amount", "option", "options", "choice", "strength", "flow")

    val bleedingWords = listOf("period", "bleeding", "menstruation", "menstrual", "menses", "spotting", "flow")

    /** Une clé qui contient l'un de ces mots parle d'autre chose que d'un jour de saignement (douleur, durée, réglage...). */
    val notBleedingWords = listOf(
        "pain", "cramp", "symptom", "reminder", "length", "cycle", "predict", "average", "setting",
        "notification", "pms", "ovulat", "fertil", "mood", "sex", "pill", "note", "count", "duration", "ongoing",
    )

    /** Années acceptées : au-delà, la date est considérée comme invalide. */
    val plausibleYears = 1990..2100

    fun isBleedingKey(key: String): Boolean {
        val k = key.lowercase()
        return bleedingWords.any { it in k } && notBleedingWords.none { it in k }
    }

    private val camelBoundary = Regex("([a-z])([A-Z])")
    private val notLetters = Regex("[^a-z]+")

    /** Mots entiers d'un texte, après découpage du camelCase, des tirets et des soulignés. */
    private fun words(text: String): List<String> =
        text.replace(camelBoundary, "$1 $2").lowercase().split(notLetters).filter { it.isNotEmpty() }

    /** Intensité nommée dans un texte (« heavy », « period_light », « spotting »...), ou null. Comparaison par mots entiers. */
    fun intensityOf(text: String): Flow? {
        val w = words(text)
        return when {
            w.any { it in setOf("spotting", "spot", "spots") } -> Flow.SPOTTING
            w.any { it in setOf("heavy", "strong", "abundant", "profuse", "high") } -> Flow.HEAVY
            w.any { it in setOf("medium", "moderate", "normal", "mid") } -> Flow.MEDIUM
            w.any { it in setOf("light", "low", "small", "scant", "scanty") } -> Flow.LIGHT
            else -> null
        }
    }

    /** 1 à 3 : léger, moyen, abondant. Toute autre valeur numérique n'est pas une intensité (durées, compteurs...). */
    fun intensityOfNumber(value: Double): Flow? = when (value) {
        1.0 -> Flow.LIGHT
        2.0 -> Flow.MEDIUM
        3.0 -> Flow.HEAVY
        else -> null
    }

    /** Mots qui signifient « oui, il y a eu des règles ce jour-là » sans préciser l'intensité. */
    fun isAffirmative(text: String): Boolean = text.lowercase() in setOf("true", "yes", "1", "period", "bleeding", "menstruation", "menses")

    private val isoDate = Regex("""^\s*(\d{4})[-/](\d{2})[-/](\d{2})(?:$|[T ].*)""")

    /** Résultat de la lecture d'une date : [date] valide, ou [invalid] si le texte ressemble à une date impossible. */
    class DateRead(val date: LocalDate?, val invalid: Boolean)

    private val none = DateRead(null, false)

    /**
     * Lit « 2024-03-05 », « 2024-03-05T10:00:00Z », « 2024-03-05 10:00 » (la date telle qu'écrite, sans conversion de fuseau)
     * ou un horodatage Unix en secondes ou en millisecondes (jour UTC).
     */
    fun readDate(raw: String): DateRead {
        val text = raw.trim()
        val iso = isoDate.matchEntire(text)
        if (iso != null) {
            val (y, m, d) = iso.destructured
            val date = runCatching { LocalDate(y.toInt(), m.toInt(), d.toInt()) }.getOrNull()
            return if (date != null && date.year in plausibleYears) DateRead(date, false) else DateRead(null, true)
        }
        if (text.length in 9..13 && text.all { it.isDigit() }) return readEpoch(text.toLong())
        return none
    }

    fun readEpoch(value: Long): DateRead {
        val seconds = if (value >= 100_000_000_000L) value / 1000 else value
        val days = seconds / 86_400
        val date = runCatching { LocalDate.ofEpochDay(days) }.getOrNull()
        return if (date != null && date.year in plausibleYears) DateRead(date, false) else none
    }
}
