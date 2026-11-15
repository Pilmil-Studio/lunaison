package fr.lunaison.core.data

import fr.lunaison.core.cycle.AlertKind
import fr.lunaison.core.cycle.LogReminderMode
import fr.lunaison.core.cycle.ReminderSettings
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Baseline
import fr.lunaison.core.model.CervicalMucus
import fr.lunaison.core.model.DayLog
import fr.lunaison.core.model.Flow
import fr.lunaison.core.model.IntimacyKind
import fr.lunaison.core.model.Libido
import fr.lunaison.core.model.MedicationKind
import fr.lunaison.core.model.Mood
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.PartnerSnapshot
import fr.lunaison.core.model.PillStatus
import fr.lunaison.core.model.Symptom
import fr.lunaison.core.model.ThemePalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import fr.lunaison.core.model.of
import fr.lunaison.core.model.tr
import fr.lunaison.core.model.isAfter
import fr.lunaison.core.model.plusDays

/** Messagerie utilisée pour le partage. L'appli ne fait que préparer le message. */
enum class PartnerChannel {
    SMS,
    WHATSAPP,
    SIGNAL,
    OTHER,
    ;

    val label: String get() = when (this) {
        SMS -> "SMS"
        WHATSAPP -> "WhatsApp"
        SIGNAL -> "Signal"
        OTHER -> tr(
            fr = "Autre appli",
            en = "Another app",
            pl = "Inna aplikacja",
            uk = "Інший застосунок",
            es = "Otra aplicación",
            de = "Andere App",
        )
    }
}

data class Partner(val name: String, val phone: String?, val channel: PartnerChannel)

data class AppSettings(
    val onboarded: Boolean = false,
    val baseline: Baseline = Baseline(AgeGroup.ADULT),
    val reminders: ReminderSettings = ReminderSettings(),
    val partner: Partner? = null,
    /** Signaux masqués par la personne, avec la date du masquage : ils reviennent au bout de 60 jours. */
    val dismissedAlerts: Map<AlertKind, LocalDate> = emptyMap(),
    /** Début du cycle dont le bilan a été lu ou masqué. */
    val recapSeen: LocalDate? = null,
    /** Met en pause les prévisions et les signaux (grossesse, allaitement, contraception continue). */
    val pausePredictions: Boolean = false,
    /** Affiche la fenêtre fertile estimée et les chances de concevoir. */
    val showFertility: Boolean = false,
    /** Palette de couleurs choisie. */
    val themePalette: ThemePalette = ThemePalette.PLUM,
    /** Empreinte SHA-256 du code PIN à 4 chiffres, ou null si aucun verrouillage n'est actif. */
    val pinHash: String? = null,
    /** Empreinte du cycle d'un ou d'une partenaire importée par QR Code ou SMS. */
    val followedPartner: PartnerSnapshot? = null,
    /** Prénom affiché dans le QR Code ou le SMS généré pour son ou sa partenaire. */
    val myShareName: String = "",
    /** Active le suivi facultatif des signes d'ovulation (glaire cervicale et température basale). */
    val trackOvulationSigns: Boolean = false,
    /** Mode adapté aux cycles naturellement irréguliers (périménopause, SOPK, post-partum). */
    val irregularCyclesMode: Boolean = false,
    /** Mémorise si la demande d'avis discrète a été traitée (avis laissé ou masqué). */
    val reviewPromptDismissed: Boolean = false,
    /** Date de dernier affichage ou lecture de chaque fiche (identifiant -> date ISO). */
    val articleLastShown: Map<String, LocalDate> = emptyMap(),
) {
    val isAdult: Boolean get() = baseline.ageGroup == AgeGroup.ADULT

    fun isDismissed(alert: AlertKind, today: LocalDate): Boolean =
        dismissedAlerts[alert]?.let { !today.isAfter(it.plusDays(ALERT_SNOOZE_DAYS)) } == true

    companion object {
        const val ALERT_SNOOZE_DAYS = 60L
        const val ARTICLE_COOLDOWN_DAYS = 7L
        private const val PBKDF2_PREFIX = "pbkdf2$"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val PBKDF2_KEY_BYTES = 32
        private const val PBKDF2_SALT_BYTES = 16

        fun hashPin(pin: String): String {
            val salt = randomPinSalt(PBKDF2_SALT_BYTES)
            val derived = pbkdf2HmacSha256(pin, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BYTES)
            return "$PBKDF2_PREFIX${salt.toHex()}$${derived.toHex()}"
        }

        fun verifyPin(pin: String, storedHash: String): Boolean {
            if (storedHash.startsWith(PBKDF2_PREFIX)) {
                val parts = storedHash.split('$')
                if (parts.size != 3) return false
                val salt = parts[1].hexToBytesOrNull() ?: return false
                val expected = parts[2].hexToBytesOrNull() ?: return false
                if (salt.isEmpty() || expected.isEmpty()) return false
                val actual = pbkdf2HmacSha256(pin, salt, PBKDF2_ITERATIONS, expected.size)
                return constantTimeEquals(actual, expected)
            }
            // Compatibilité avec les anciens hash SHA-256 non salés (64 caractères hexadécimaux).
            if (storedHash.length == 64 && storedHash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                val legacyActual = Sha256.digest(pin.encodeToByteArray()).toHex()
                return constantTimeEquals(legacyActual.encodeToByteArray(), storedHash.lowercase().encodeToByteArray())
            }
            return false
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        private fun String.hexToBytesOrNull(): ByteArray? {
            if (length % 2 != 0) return null
            val out = ByteArray(length / 2)
            for (i in out.indices) {
                val byteVal = substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: return null
                out[i] = byteVal.toByte()
            }
            return out
        }

        private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }
    }
}

/** Seule source de données de l'appli. Tout reste sur le téléphone. */
class LunaisonRepository(private val dao: LunaisonDao) {

    private val settingsMutex = Mutex()

    val logs: kotlinx.coroutines.flow.Flow<List<DayLog>> = dao.logs().map { list -> list.map { it.toModel() } }
    val settings: kotlinx.coroutines.flow.Flow<AppSettings> = dao.settings().map { it?.toModel() ?: AppSettings() }

    suspend fun logsOnce(): List<DayLog> = dao.logsOnce().map { it.toModel() }
    suspend fun settingsOnce(): AppSettings = dao.settingsOnce()?.toModel() ?: AppSettings()

    /** Une journée vide est supprimée plutôt qu'enregistrée. */
    suspend fun save(log: DayLog) {
        if (log.isEmpty) dao.deleteLog(log.date.toString()) else dao.upsertLog(log.toEntity())
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsMutex.withLock {
            dao.upsertSettings(transform(settingsOnce()).toEntity())
        }
    }

    /** Efface tout, sans retour possible. */
    suspend fun eraseAll() {
        dao.deleteLogs()
        dao.deleteSettings()
    }

    /**
     * Document JSON de la sauvegarde complète : toutes les tables de la base, sans le code PIN.
     * C'est le contenu qu'on chiffre dans un fichier .pilmil.
     */
    suspend fun exportBackupJson(): String =
        LunaisonBackup.encode(BackupContent(dao.logsOnce(), dao.settingsOnce()))

    /**
     * Remplace toutes les données actuelles par celles d'une sauvegarde. Le document est lu et vérifié en entier
     * avant tout changement ; en cas d'échec ([BackupFormatException]) rien n'est modifié.
     * Le code PIN de ce téléphone est conservé : il n'est pas dans la sauvegarde.
     */
    suspend fun restoreBackupJson(json: String): BackupRestoreSummary {
        val content = LunaisonBackup.decode(json)
        settingsMutex.withLock {
            val currentPin = dao.settingsOnce()?.pinHash
            dao.replaceAll(content.logs, content.settings?.copy(pinHash = currentPin))
        }
        return BackupRestoreSummary(daysCount = content.logs.size, settingsRestored = content.settings != null)
    }

    /**
     * Export lisible par un tableur. Rien ne quitte le téléphone sans que la personne choisisse où l'enregistrer.
     * Le format ne dépend pas de la langue : en-tête anglais fixe, valeurs d'énumération sous leur identifiant
     * (`HEAVY`, `CRAMPS`...), booléens `true`/`false`, dates ISO-8601, nombres avec un point.
     */
    suspend fun exportCsv(): String = buildString {
        appendLine(CSV_HEADER)
        logsOnce().forEach { log ->
            appendLine(
                listOf(
                    log.date.toString(),
                    log.flow?.name.orEmpty(),
                    log.pain?.name.orEmpty(),
                    log.symptoms.joinToString(", ") { it.name },
                    log.moods.joinToString(", ") { it.name },
                    (log.medicationTaken || log.pillStatus == PillStatus.TAKEN).toString(),
                    log.mucus?.name.orEmpty(),
                    log.basalTemperature?.let { it.toOneDecimal() }.orEmpty(),
                    log.pillStatus?.name.orEmpty(),
                    log.medications.joinToString(", ") { it.name },
                    log.weight?.let { it.toOneDecimal() }.orEmpty(),
                    log.intimacy.joinToString(", ") { it.name },
                    log.libido?.name.orEmpty(),
                    sanitizeCsvNote(log.note),
                ).joinToString(";"),
            )
        }
    }

    /**
     * Importe un fichier CSV (format Lunaison `;` ou export externe `,` contenant dates et flux)
     * et renvoie le nombre de journées importées. Voir [importCsvReport] pour le détail.
     */
    suspend fun importCsv(content: String): Int = importCsvReport(content).imported

    /**
     * Comme [importCsv], avec le détail. Les valeurs d'énumération sont reconnues sous leur identifiant ou sous
     * leur libellé dans n'importe quelle des six langues (anciens exports). Une valeur inconnue n'empêche pas
     * l'import : la journée est importée sans ce champ et la valeur est comptée dans [CsvImportReport.unknownValues].
     */
    suspend fun importCsvReport(content: String): CsvImportReport {
        var imported = 0
        var skipped = 0
        var unknown = 0
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        for ((index, line) in lines.withIndex()) {
            val delimiter = if (';' in line) ';' else ','
            val cols = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            val date = cols.firstOrNull()?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (date == null) {
                // La première ligne est l'en-tête : elle n'est pas une journée perdue.
                if (index > 0) skipped += 1
                continue
            }
            fun <T> field(raw: String?, parse: (String) -> T?): T? {
                val text = raw.orEmpty().trim()
                if (text.isEmpty()) return null
                return parse(text).also { if (it == null) unknown += 1 }
            }
            fun <T> fieldSet(raw: String?, parse: (String) -> T?): Set<T> =
                raw.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .mapNotNull { item -> parse(item).also { if (it == null) unknown += 1 } }.toSet()

            val flow = field(cols.getOrNull(1), ::parseFlow)
            val pain = field(cols.getOrNull(2)) { parseLabelled(it, Pain.entries, Pain::allLabels) }
            val symptoms = fieldSet(cols.getOrNull(3)) { parseLabelled(it, Symptom.entries, Symptom::allLabels) }
            val moods = fieldSet(cols.getOrNull(4)) { parseLabelled(it, Mood.entries, Mood::allLabels) }
            val medTaken = cols.getOrNull(5).orEmpty().trim().lowercase() in TRUE_VALUES
            val isV2 = cols.size >= 14
            val isV1Extended = !isV2 && cols.size >= 9
            val mucus = if (isV2 || isV1Extended) field(cols.getOrNull(6)) { parseLabelled(it, CervicalMucus.entries, CervicalMucus::allLabels) } else null
            val temp = if (isV2 || isV1Extended) cols.getOrNull(7)?.replace(',', '.')?.toFloatOrNull() else null
            val pillStatus = if (isV2) field(cols.getOrNull(8)) { parseLabelled(it, PillStatus.entries, PillStatus::allLabels) } else null
            val medications = if (isV2) fieldSet(cols.getOrNull(9)) { parseLabelled(it, MedicationKind.entries, MedicationKind::allLabels) } else emptySet()
            val weight = if (isV2) cols.getOrNull(10)?.replace(',', '.')?.toFloatOrNull() else null
            val intimacy = if (isV2) fieldSet(cols.getOrNull(11)) { parseLabelled(it, IntimacyKind.entries, IntimacyKind::allLabels) } else emptySet()
            val libido = if (isV2) field(cols.getOrNull(12)) { parseLabelled(it, Libido.entries, Libido::allLabels) } else null
            val noteColIdx = when {
                isV2 -> 13
                isV1Extended -> 8
                else -> 6
            }
            val note = unsanitizeCsvNote(cols.getOrNull(noteColIdx).orEmpty()).take(500)
            val log = DayLog(
                date = date,
                flow = flow,
                pain = pain,
                symptoms = symptoms,
                moods = moods,
                note = note,
                medicationTaken = medTaken || pillStatus == PillStatus.TAKEN,
                pillStatus = pillStatus,
                medications = medications,
                weight = weight,
                intimacy = intimacy,
                libido = libido,
                mucus = mucus,
                basalTemperature = temp,
            )
            if (!log.isEmpty) {
                save(log)
                imported += 1
            } else {
                skipped += 1
            }
        }
        return CsvImportReport(imported = imported, skipped = skipped, unknownValues = unknown)
    }

    companion object {
        /** En-tête fixe de l'export CSV, identique dans toutes les langues. */
        const val CSV_HEADER = "date;flow;pain;symptoms;moods;medication;mucus;temperature;pill;medications;weight;intimacy;libido;note"
    }
}

/** Ce qu'une restauration a remis en place. */
data class BackupRestoreSummary(val daysCount: Int, val settingsRestored: Boolean)

/** Bilan d'un import CSV : journées importées, lignes ignorées (illisibles ou vides) et valeurs non reconnues. */
data class CsvImportReport(val imported: Int, val skipped: Int, val unknownValues: Int)

/** Valeurs acceptées pour « vrai » : canonique, anglais et anciens libellés des six langues. */
private val TRUE_VALUES = setOf("true", "yes", "oui", "tak", "так", "sí", "si", "ja", "1")

interface LunaisonRepositoryProvider {
    val repository: LunaisonRepository
}

private fun sanitizeCsvNote(raw: String): String {
    val cleaned = raw.replace(";", ",").replace("\r", " ").replace("\n", " ")
    val trimmed = cleaned.trimStart()
    return if (trimmed.isNotEmpty() && trimmed.first() in charArrayOf('=', '+', '-', '@')) {
        "'$cleaned"
    } else {
        cleaned
    }
}

/** Retire l'apostrophe ajoutée par [sanitizeCsvNote] devant une note qui commence comme une formule. */
private fun unsanitizeCsvNote(raw: String): String =
    if (raw.length >= 2 && raw[0] == '\'' && raw[1] in charArrayOf('=', '+', '-', '@')) raw.substring(1) else raw

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

private inline fun <reified T : Enum<T>> String.toEnumSet(): Set<T> =
    split(",").mapNotNull { it.trim().toEnumOrNull<T>() }.toSet()

internal fun DayLogEntity.toModel() = DayLog(
    date = LocalDate.parse(date),
    flow = flow.toEnumOrNull<Flow>(),
    pain = pain.toEnumOrNull<Pain>(),
    symptoms = symptoms.toEnumSet<Symptom>(),
    moods = moods.toEnumSet<Mood>(),
    note = note,
    medicationTaken = medicationTaken,
    pillStatus = pillStatus.toEnumOrNull<PillStatus>(),
    medications = medications.toEnumSet<MedicationKind>(),
    weight = weight,
    intimacy = intimacy.toEnumSet<IntimacyKind>(),
    libido = libido.toEnumOrNull<Libido>(),
    mucus = mucus.toEnumOrNull<CervicalMucus>(),
    basalTemperature = basalTemperature,
)

internal fun DayLog.toEntity() = DayLogEntity(
    date = date.toString(),
    flow = flow?.name,
    pain = pain?.name,
    symptoms = symptoms.joinToString(",") { it.name },
    moods = moods.joinToString(",") { it.name },
    note = note,
    medicationTaken = medicationTaken || pillStatus == PillStatus.TAKEN,
    pillStatus = pillStatus?.name,
    medications = medications.joinToString(",") { it.name },
    weight = weight,
    intimacy = intimacy.joinToString(",") { it.name },
    libido = libido?.name,
    mucus = mucus?.name,
    basalTemperature = basalTemperature,
)

internal fun SettingsEntity.toModel() = AppSettings(
    onboarded = onboarded,
    baseline = Baseline(
        ageGroup = ageGroup.toEnumOrNull<AgeGroup>() ?: AgeGroup.ADULT,
        lastPeriodStart = lastPeriodStart?.let { LocalDate.parse(it) },
        usualCycleLength = usualCycleLength,
        usualPeriodLength = usualPeriodLength,
    ),
    reminders = ReminderSettings(
        periodReminder = periodReminder,
        periodReminderDaysBefore = periodReminderDaysBefore,
        logReminder = logReminder.toEnumOrNull<LogReminderMode>() ?: LogReminderMode.AROUND_PERIOD,
        logReminderTime = runCatching { LocalTime.parse(logReminderTime) }.getOrDefault(LocalTime.of(20, 0)),
        medicationReminder = medicationReminder,
        medicationReminderTime = runCatching { LocalTime.parse(medicationReminderTime) }.getOrDefault(LocalTime.of(9, 0)),
        medicationLabel = medicationLabel,
    ),
    partner = if (partnerName != null && partnerChannel != null) {
        Partner(partnerName, partnerPhone, partnerChannel.toEnumOrNull<PartnerChannel>() ?: PartnerChannel.OTHER)
    } else {
        null
    },
    dismissedAlerts = dismissedAlerts.split(",").mapNotNull { pair ->
        val parts = pair.split("@")
        val kind = parts.getOrNull(0).toEnumOrNull<AlertKind>()
        val date = parts.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (kind != null && date != null) kind to date else null
    }.toMap(),
    recapSeen = recapSeen?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    pausePredictions = pausePredictions,
    showFertility = showFertility,
    themePalette = themePalette.toEnumOrNull<ThemePalette>() ?: ThemePalette.PLUM,
    pinHash = pinHash,
    followedPartner = followedPartnerSnapshot?.let(PartnerSnapshot::decode),
    myShareName = myShareName,
    trackOvulationSigns = trackOvulationSigns,
    irregularCyclesMode = irregularCyclesMode,
    reviewPromptDismissed = reviewPromptDismissed,
    articleLastShown = articleLastShown.split(",").mapNotNull { pair ->
        val parts = pair.split("@")
        val id = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
        val date = parts.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (id != null && date != null) id to date else null
    }.toMap(),
)

internal fun AppSettings.toEntity() = SettingsEntity(
    onboarded = onboarded,
    ageGroup = baseline.ageGroup.name,
    lastPeriodStart = baseline.lastPeriodStart?.toString(),
    usualCycleLength = baseline.usualCycleLength,
    usualPeriodLength = baseline.usualPeriodLength,
    periodReminder = reminders.periodReminder,
    periodReminderDaysBefore = reminders.periodReminderDaysBefore,
    logReminder = reminders.logReminder.name,
    logReminderTime = reminders.logReminderTime.toString(),
    partnerName = partner?.name,
    partnerPhone = partner?.phone,
    partnerChannel = partner?.channel?.name,
    dismissedAlerts = dismissedAlerts.entries.joinToString(",") { "${it.key.name}@${it.value}" },
    recapSeen = recapSeen?.toString(),
    medicationReminder = reminders.medicationReminder,
    medicationReminderTime = reminders.medicationReminderTime.toString(),
    medicationLabel = reminders.medicationLabel,
    pausePredictions = pausePredictions,
    showFertility = showFertility,
    themePalette = themePalette.name,
    pinHash = pinHash,
    followedPartnerSnapshot = followedPartner?.encode(),
    myShareName = myShareName,
    trackOvulationSigns = trackOvulationSigns,
    irregularCyclesMode = irregularCyclesMode,
    reviewPromptDismissed = reviewPromptDismissed,
    articleLastShown = articleLastShown.entries.joinToString(",") { "${it.key}@${it.value}" },
)

/** Identifiant de l'énumération, sinon libellé dans l'une des six langues (sans tenir compte de la casse). */
private fun <T : Enum<T>> parseLabelled(raw: String, entries: List<T>, labels: (T) -> List<String>): T? {
    val v = raw.trim().lowercase()
    if (v.isEmpty()) return null
    return entries.firstOrNull { it.name.lowercase() == v }
        ?: entries.firstOrNull { entry -> labels(entry).any { it.lowercase() == v } }
}

private fun parseFlow(raw: String): Flow? {
    val v = raw.trim().lowercase()
    if (v.isEmpty()) return null
    return parseLabelled(v, Flow.entries, Flow::allLabels)
        ?: when {
            "spot" in v || "trace" in v -> Flow.SPOTTING
            "light" in v || "léger" in v || "leger" in v -> Flow.LIGHT
            "heavy" in v || "abond" in v -> Flow.HEAVY
            "med" in v || "moyen" in v || "period" in v || "règle" in v -> Flow.MEDIUM
            else -> null
        }
}
