package fr.lunaison.core.model

/**
 * Pays dont on montre les droits et les aides (remboursements, accès aux soins). Distinct de la langue :
 * on peut lire en français en Belgique, ou en polonais en France. Rien n'est envoyé : la région vient des
 * réglages du téléphone, sans autorisation ni connexion, et la personne peut la changer.
 */
object Country {
    /** Réglage vide : suivre la région du téléphone. */
    const val AUTO = ""

    /** Réglage « un autre pays » : aucun contenu de droits propre à un pays. */
    const val OTHER = "OTHER"

    /** Pays pour lesquels Lunaison a des fiches de droits. */
    val supported: List<String> = listOf("FR")

    /** Code du pays à utiliser, ou null si aucun pays connu ne correspond. */
    fun resolve(setting: String, systemRegion: String = systemRegionCode()): String? {
        val code = when (setting) {
            AUTO -> systemRegion.uppercase()
            OTHER -> return null
            else -> setting.uppercase()
        }
        return code.takeIf { it.length == 2 && it.all(Char::isLetter) }
    }

    /** Nom du pays dans la langue de l'appli, pour les pays pris en charge. */
    fun displayName(code: String): String = when (code) {
        "FR" -> tr("France", "France", pl = "Francja", uk = "Франція", es = "Francia", de = "Frankreich")
        else -> code
    }
}
