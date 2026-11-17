package fr.lunaison.ui.backup

import java.security.MessageDigest
import kotlinx.datetime.LocalDate

/**
 * Noms des fichiers de la sauvegarde automatique : `sauvegarde-AAAA-MM-JJ.pilmil`, neutres (ni « règles » ni
 * « cycle »). Si une sauvegarde du même jour existe déjà, le suivant porte un numéro (`...-JJ-2.pilmil`) :
 * un fichier existant n'est jamais écrasé avant que le nouveau soit vérifié.
 */
object BackupFileNames {
    const val PREFIX = "sauvegarde-"
    const val EXTENSION = ".pilmil"

    /** Nombre de sauvegardes conservées dans le dossier. */
    const val KEEP = 3

    private val PATTERN = Regex("""^sauvegarde-(\d{4})-(\d{2})-(\d{2})(?:-(\d{1,3}))?\.pilmil$""")

    /** Date et numéro d'ordre dans la journée d'un nom reconnu. */
    data class Parsed(val date: LocalDate, val counter: Int) : Comparable<Parsed> {
        override fun compareTo(other: Parsed): Int =
            compareValuesBy(this, other, { it.date }, { it.counter })
    }

    fun nameFor(date: LocalDate, counter: Int = 1): String {
        val suffix = if (counter <= 1) "" else "-$counter"
        return "$PREFIX$date$suffix$EXTENSION"
    }

    /** Null pour tout nom qui n'est pas un fichier de l'appli : ces fichiers ne sont jamais touchés. */
    fun parse(name: String): Parsed? {
        val match = PATTERN.matchEntire(name) ?: return null
        val groups = match.groupValues
        val date = runCatching { LocalDate(groups[1].toInt(), groups[2].toInt(), groups[3].toInt()) }.getOrNull() ?: return null
        val number = groups[4].takeIf { it.isNotEmpty() }?.toInt() ?: 1
        return if (number < 1) null else Parsed(date, number)
    }

    /** Nom à utiliser aujourd'hui, jamais identique à un fichier déjà présent. */
    fun nextName(today: LocalDate, existing: Collection<String>): String {
        val usedToday = existing.mapNotNull { parse(it) }.filter { it.date == today }.map { it.counter }
        val counter = if (usedToday.isEmpty()) 1 else usedToday.max() + 1
        return nameFor(today, counter)
    }

    /**
     * Fichiers à supprimer une fois [justWritten] écrit et vérifié : on garde [keep] sauvegardes, les plus
     * récentes, dont toujours celle qui vient d'être écrite. Un nom qui n'est pas de l'appli n'est jamais supprimé.
     */
    fun toDelete(existing: Collection<String>, justWritten: String, keep: Int = KEEP): List<String> {
        val others = existing
            .filter { it != justWritten }
            .mapNotNull { name -> parse(name)?.let { name to it } }
            .sortedByDescending { it.second }
        return others.drop((keep - 1).coerceAtLeast(0)).map { it.first }
    }
}

/** Pourquoi une exécution de la sauvegarde n'a rien écrit. */
enum class SkipReason { NOT_CONFIGURED, NOTHING_TO_SAVE, DONE_TODAY, UNCHANGED }

sealed interface BackupDecision {
    data object Run : BackupDecision

    data class Skip(val reason: SkipReason) : BackupDecision
}

/** Décide si une sauvegarde doit être écrite maintenant. Logique pure. */
object BackupPlanner {

    /**
     * - rien n'est fait tant qu'aucune phrase n'est définie, ni quand il n'y a aucune donnée (une base vide ne
     *   doit jamais remplacer de vraies sauvegardes) ;
     * - [force] (bouton « Sauvegarder maintenant », fin de la configuration) passe outre le jour et l'absence de changement ;
     * - sinon : au plus une fois par jour, et seulement si les données ont changé depuis la dernière réussite.
     */
    fun decide(
        configured: Boolean,
        hasData: Boolean,
        today: LocalDate,
        lastSuccessDate: LocalDate?,
        lastFingerprint: String?,
        fingerprint: String,
        force: Boolean,
    ): BackupDecision = when {
        !configured -> BackupDecision.Skip(SkipReason.NOT_CONFIGURED)
        !hasData -> BackupDecision.Skip(SkipReason.NOTHING_TO_SAVE)
        force -> BackupDecision.Run
        lastSuccessDate == today -> BackupDecision.Skip(SkipReason.DONE_TODAY)
        lastFingerprint == fingerprint -> BackupDecision.Skip(SkipReason.UNCHANGED)
        else -> BackupDecision.Run
    }

    /** Empreinte du contenu sauvegardé : sert seulement à savoir si les données ont changé. */
    fun fingerprint(payload: String): String =
        MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
