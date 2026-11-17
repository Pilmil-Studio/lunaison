package fr.lunaison.ui.backup

/** Force indicative de la phrase secrète : un repère simple, pas une garantie. */
enum class PassphraseStrength { TOO_SHORT, FAIR, STRONG }

/** Résultat de la vérification des deux saisies de la phrase secrète. */
data class PassphraseCheck(
    val strength: PassphraseStrength,
    /** La confirmation est commencée mais ne correspond pas. */
    val mismatch: Boolean,
    /** La phrase peut être enregistrée : assez longue et saisie deux fois à l'identique. */
    val valid: Boolean,
)

/**
 * Règles de la phrase secrète de la sauvegarde automatique. Elle protège un fichier que la personne emporte
 * hors du téléphone : longueur minimale de 12 caractères, et pas une même lettre répétée. Aucun envoi nulle part.
 */
object PassphraseRules {
    const val MIN_LENGTH = 12
    const val STRONG_LENGTH = 20
    private const val MIN_DISTINCT = 5

    fun strength(phrase: String): PassphraseStrength = when {
        phrase.trim().length < MIN_LENGTH || phrase.toSet().size < MIN_DISTINCT -> PassphraseStrength.TOO_SHORT
        phrase.length >= STRONG_LENGTH -> PassphraseStrength.STRONG
        else -> PassphraseStrength.FAIR
    }

    fun check(phrase: String, confirmation: String): PassphraseCheck {
        val strength = strength(phrase)
        val mismatch = confirmation.isNotEmpty() && phrase != confirmation
        return PassphraseCheck(
            strength = strength,
            mismatch = mismatch,
            valid = strength != PassphraseStrength.TOO_SHORT && phrase == confirmation,
        )
    }
}
