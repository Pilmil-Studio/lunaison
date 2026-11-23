package fr.lunaison.core.data

import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.daysTo
import fr.lunaison.core.model.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/*
 * Import des règles écrites par d'autres applis dans le magasin de santé du téléphone
 * (Health Connect sur Android ; HealthKit sur iOS, pas encore branché).
 * Lecture seule, uniquement quand la personne le demande depuis l'écran d'import.
 */

/** État de la source de santé du téléphone. */
enum class HealthAvailability {
    AVAILABLE,

    /** Health Connect n'est pas installé (Android 13 et moins). */
    NOT_INSTALLED,

    /** Health Connect est présent mais trop ancien : mise à jour requise. */
    UPDATE_REQUIRED,

    /** Aucune source de santé sur cet appareil (iOS tant que HealthKit n'est pas branché). */
    NOT_SUPPORTED,
}

enum class HealthFlowLevel { UNKNOWN, LIGHT, MEDIUM, HEAVY }

/** Règles lues dans le magasin de santé, en heure locale de l'enregistrement. Valeurs neutres, sans type de SDK. */
data class HealthPeriodRecord(val start: LocalDateTime, val end: LocalDateTime)

data class HealthFlowRecord(val time: LocalDateTime, val level: HealthFlowLevel)

data class HealthMenstruationData(
    val periods: List<HealthPeriodRecord>,
    val flows: List<HealthFlowRecord>,
)

sealed interface HealthReadResult {
    data class Data(val data: HealthMenstruationData) : HealthReadResult

    /** Permission de lecture non accordée (ou retirée entre-temps). */
    data object PermissionDenied : HealthReadResult

    data class Unavailable(val availability: HealthAvailability) : HealthReadResult

    data class Failure(val message: String?) : HealthReadResult
}

/**
 * Source de santé du téléphone. L'interface est commune pour que HealthKit puisse s'y brancher plus tard.
 * Rien ne la lance automatiquement : l'écran d'import l'appelle sur demande de la personne.
 */
interface HealthDataSource {
    suspend fun availability(): HealthAvailability

    suspend fun hasPermission(): Boolean

    /** Lit les règles. Ne demande jamais la permission elle-même (la demande passe par l'interface). */
    suspend fun read(): HealthReadResult
}

/** Source utilisée là où aucun magasin de santé n'est branché. */
object UnavailableHealthDataSource : HealthDataSource {
    override suspend fun availability(): HealthAvailability = HealthAvailability.NOT_SUPPORTED

    override suspend fun hasPermission(): Boolean = false

    override suspend fun read(): HealthReadResult = HealthReadResult.Unavailable(HealthAvailability.NOT_SUPPORTED)
}

/**
 * Source de santé de la plateforme. [platformContext] est le `Context` Android ; il est ignoré ailleurs.
 * iOS renvoie « non disponible » tant que HealthKit n'est pas implémenté.
 */
expect fun platformHealthDataSource(platformContext: Any?): HealthDataSource

/** Conversion des enregistrements de santé en journées Lunaison. Pur, sans dépendance de plateforme. */
object HealthRecordsMapper {
    /** Garde-fou contre un enregistrement aberrant : au-delà, les règles sont tronquées. */
    const val MAX_PERIOD_DAYS = 31

    /**
     * - chaque jour d'une période de règles devient un jour de flux ;
     * - l'intensité vient des enregistrements de flux du même jour (la plus forte), sinon « moyen » par défaut,
     *   comme pour l'import Calendrier des règles ;
     * - un enregistrement de flux hors de toute période compte aussi comme un jour de flux ;
     * - une fin à minuit pile est exclusive (convention des applis qui écrivent le lendemain 00:00).
     */
    fun toDayLogs(data: HealthMenstruationData): List<DayLog> {
        val levelByDay = mutableMapOf<LocalDate, HealthFlowLevel>()
        for (record in data.flows) {
            val day = record.time.date
            val best = levelByDay[day]
            if (record.level.rank() > (best?.rank() ?: -1)) levelByDay[day] = record.level
        }
        val days = sortedSetOf<LocalDate>()
        for (period in data.periods) days += daysOf(period)
        days += levelByDay.keys
        return days.map { DayLog(date = it, flow = levelByDay[it].toFlow()) }
    }

    fun toPreview(data: HealthMenstruationData): ImportPreview =
        ImportPreview.of(ImportSource.HEALTH_CONNECT, toDayLogs(data))

    internal fun daysOf(period: HealthPeriodRecord): List<LocalDate> {
        val first = period.start.date
        var last = period.end.date
        val endsAtMidnight = period.end.time.let { it.hour == 0 && it.minute == 0 && it.second == 0 && it.nanosecond == 0 }
        if (endsAtMidnight && last > first) last = last.plusDays(-1)
        if (last < first) last = first
        val length = minOf(first.daysTo(last) + 1, MAX_PERIOD_DAYS)
        return List(length) { first.plusDays(it) }
    }

    private fun HealthFlowLevel.rank(): Int = when (this) {
        HealthFlowLevel.UNKNOWN -> 0
        HealthFlowLevel.LIGHT -> 1
        HealthFlowLevel.MEDIUM -> 2
        HealthFlowLevel.HEAVY -> 3
    }

    private fun HealthFlowLevel?.toFlow(): Flow = when (this) {
        HealthFlowLevel.LIGHT -> Flow.LIGHT
        HealthFlowLevel.HEAVY -> Flow.HEAVY
        HealthFlowLevel.MEDIUM, HealthFlowLevel.UNKNOWN, null -> Flow.MEDIUM
    }
}
