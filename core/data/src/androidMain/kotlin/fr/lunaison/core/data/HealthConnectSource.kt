package fr.lunaison.core.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlin.reflect.KClass

actual fun platformHealthDataSource(platformContext: Any?): HealthDataSource =
    (platformContext as? Context)?.let { HealthConnectSource(it) } ?: UnavailableHealthDataSource

/**
 * Lecture seule des règles dans Health Connect (MenstruationPeriodRecord et MenstruationFlowRecord).
 * La seule permission demandée est READ_MENSTRUATION. Rien n'est écrit dans Health Connect ni envoyé ailleurs.
 * La demande de permission passe par l'interface (contrat d'`Activity`), pas par cette classe.
 *
 * Sans la permission d'historique de Health Connect, seules les données des 30 jours précédant
 * le premier accord de la permission sont lisibles : c'est une limite de Health Connect.
 */
class HealthConnectSource(context: Context) : HealthDataSource {
    private val appContext = context.applicationContext

    override suspend fun availability(): HealthAvailability = when (HealthConnectClient.getSdkStatus(appContext)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.UPDATE_REQUIRED
        else -> HealthAvailability.NOT_INSTALLED
    }

    override suspend fun hasPermission(): Boolean = try {
        HealthConnectClient.getOrCreate(appContext).permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    override suspend fun read(): HealthReadResult {
        val status = availability()
        if (status != HealthAvailability.AVAILABLE) return HealthReadResult.Unavailable(status)
        return try {
            val client = HealthConnectClient.getOrCreate(appContext)
            if (!client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)) {
                return HealthReadResult.PermissionDenied
            }
            val periods = readAll(client, MenstruationPeriodRecord::class).map { it.toNeutral() }
            val flows = readAll(client, MenstruationFlowRecord::class).map { it.toNeutral() }
            HealthReadResult.Data(HealthMenstruationData(periods, flows))
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            HealthReadResult.PermissionDenied
        } catch (e: IOException) {
            HealthReadResult.Failure(e.localizedMessage)
        } catch (e: IllegalStateException) {
            HealthReadResult.Failure(e.localizedMessage)
        } catch (e: android.os.RemoteException) {
            HealthReadResult.Failure(e.localizedMessage)
        }
    }

    private suspend fun <T : Record> readAll(client: HealthConnectClient, type: KClass<T>): List<T> {
        val all = mutableListOf<T>()
        var token: String? = null
        var pages = 0
        do {
            val response = client.readRecords(ReadRecordsRequest(type, TimeRangeFilter.after(Instant.EPOCH), pageToken = token))
            all += response.records
            token = response.pageToken
            pages++
        } while (token != null && pages < MAX_PAGES)
        return all
    }

    companion object {
        /** READ_MENSTRUATION : la même permission couvre les périodes et le flux. Aucune permission d'écriture. */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
            HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        )

        private const val MAX_PAGES = 100
    }
}

private fun localTime(instant: Instant, offset: ZoneOffset?): LocalDateTime {
    val zoneOffset = offset ?: ZoneId.systemDefault().rules.getOffset(instant)
    return instant.atOffset(zoneOffset).toLocalDateTime().toKotlinLocalDateTime()
}

private fun MenstruationPeriodRecord.toNeutral() = HealthPeriodRecord(
    start = localTime(startTime, startZoneOffset),
    end = localTime(endTime, endZoneOffset),
)

private fun MenstruationFlowRecord.toNeutral() = HealthFlowRecord(
    time = localTime(time, zoneOffset),
    level = when (flow) {
        MenstruationFlowRecord.FLOW_LIGHT -> HealthFlowLevel.LIGHT
        MenstruationFlowRecord.FLOW_MEDIUM -> HealthFlowLevel.MEDIUM
        MenstruationFlowRecord.FLOW_HEAVY -> HealthFlowLevel.HEAVY
        else -> HealthFlowLevel.UNKNOWN
    },
)
