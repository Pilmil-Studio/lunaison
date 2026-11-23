package fr.lunaison.ui

import androidx.compose.runtime.staticCompositionLocalOf
import fr.lunaison.core.model.tr

/**
 * Déverrouillage par empreinte ou visage, fourni par la plateforme. Il ne remplace pas le code : il évite
 * seulement de le taper. Le code reste le recours si la reconnaissance échoue ou est annulée.
 */
interface BiometricUnlock {
    /** Un capteur de classe forte est présent et au moins une empreinte ou un visage est enregistré. */
    val available: Boolean

    /** Affiche la demande du système ; [onSuccess] n'est appelé qu'après une reconnaissance réussie. */
    fun authenticate(title: String, cancelLabel: String, onSuccess: () -> Unit)
}

/** Ce que cette plateforme sait faire pour la discrétion. Rien par défaut (captures d'écran, iOS pour l'instant). */
data class DiscretionSupport(
    val biometric: BiometricUnlock? = null,
    /** Une seconde base, ouverte par le code leurre, est disponible. */
    val decoyAvailable: Boolean = false,
)

val LocalDiscretion = staticCompositionLocalOf { DiscretionSupport() }

/** Textes partagés par l'écran de déverrouillage et les réglages. */
internal object DiscretionTexts {
    val biometricPromptTitle: String
        get() = tr6("Déverrouiller Lunaison", "Unlock Lunaison", "Odblokuj Lunaison", "Розблокувати Lunaison", "Desbloquear Lunaison", "Lunaison entsperren")
    val useCode: String
        get() = tr6("Utiliser le code", "Use the code", "Użyj kodu", "Використати код", "Usar el código", "Code verwenden")
    val useBiometric: String
        get() = tr6(
            "Utiliser l'empreinte ou le visage",
            "Use fingerprint or face",
            "Użyj odcisku palca lub twarzy",
            "Використати відбиток або обличчя",
            "Usar la huella o el rostro",
            "Fingerabdruck oder Gesicht verwenden",
        )
}

internal fun tr6(fr: String, en: String, pl: String, uk: String, es: String, de: String): String =
    tr(fr, en, pl = pl, uk = uk, es = es, de = de)
