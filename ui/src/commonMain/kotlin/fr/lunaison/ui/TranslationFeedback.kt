package fr.lunaison.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import fr.lunaison.core.model.Language
import fr.lunaison.core.model.tr

/**
 * « Proposer une meilleure traduction » : ouvre la messagerie de la personne avec un e-mail pré-rempli.
 * Lunaison n'envoie rien elle-même et n'a pas besoin d'Internet ; la personne relit le message avant de l'envoyer.
 * Aucune donnée de l'appli n'y est jointe : seulement la langue, la version et l'endroit du texte.
 *
 * Indépendant de Lunaison à part le nom de l'appli : à déplacer dans pilmil-core pour les autres applis du studio.
 */
object TranslationFeedback {
    const val ADDRESS = "pilmil.studio@proton.me"

    /** Le français est la langue d'origine des textes : le bouton n'apparaît que dans les autres langues. */
    val available: Boolean
        get() = Language.current() != Language.FR

    /** Nom de la langue dans cette langue, pour que l'objet du message se lise sans traduction. */
    fun nativeName(language: Language): String = when (language) {
        Language.FR -> "Français"
        Language.EN -> "English"
        Language.PL -> "Polski"
        Language.UK -> "Українська"
        Language.ES -> "Español"
        Language.DE -> "Deutsch"
    }

    fun subject(appName: String, versionName: String?, language: Language): String {
        val app = if (versionName.isNullOrBlank()) appName else "$appName $versionName"
        return "$app, traduction / translation (${language.code}, ${nativeName(language)})"
    }

    fun body(place: String): String {
        val where = tr("Où apparaît le texte", "Where the text appears", pl = "Gdzie pojawia się tekst", uk = "Де з'являється текст", es = "Dónde aparece el texto", de = "Wo der Text erscheint")
        val current = tr("Texte actuel", "Current text", pl = "Obecny tekst", uk = "Поточний текст", es = "Texto actual", de = "Aktueller Text")
        val proposal = tr("Ma proposition", "My suggestion", pl = "Moja propozycja", uk = "Моя пропозиція", es = "Mi propuesta", de = "Mein Vorschlag")
        val note = tr(
            "Aucune donnée de l'appli n'est jointe à ce message.",
            "No app data is attached to this message.",
            pl = "Do tej wiadomości nie dołączono żadnych danych z aplikacji.",
            uk = "До цього повідомлення не додано жодних даних із застосунку.",
            es = "Este mensaje no incluye ningún dato de la aplicación.",
            de = "Dieser Nachricht sind keine App-Daten beigefügt.",
        )
        return "$where : $place\n\n$current :\n\n\n$proposal :\n\n\n($note)"
    }

    fun mailto(appName: String, place: String, versionName: String? = null, language: Language = Language.current()): String =
        "mailto:$ADDRESS?subject=${percentEncode(subject(appName, versionName, language))}&body=${percentEncode(body(place))}"

    /** Encodage d'URL (RFC 3986) des octets UTF-8 : seuls les caractères non réservés restent tels quels. */
    fun percentEncode(text: String): String = buildString {
        for (byte in text.encodeToByteArray()) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in "-._~") {
                append(ch)
            } else {
                append('%')
                append("0123456789ABCDEF"[c shr 4])
                append("0123456789ABCDEF"[c and 0x0F])
            }
        }
    }
}

/** Lien discret « Proposer une meilleure traduction », absent quand l'appli est en français. */
@Composable
fun TranslationFeedbackButton(place: String, versionName: String? = null, modifier: Modifier = Modifier) {
    if (!TranslationFeedback.available) return
    val context = rememberPlatformContext()
    TextButton(
        onClick = { openLink(context, TranslationFeedback.mailto("Lunaison", place, versionName)) },
        modifier = modifier,
    ) {
        Text(
            tr(
                "Proposer une meilleure traduction",
                "Suggest a better translation",
                pl = "Zaproponuj lepsze tłumaczenie",
                uk = "Запропонувати кращий переклад",
                es = "Proponer una traducción mejor",
                de = "Bessere Übersetzung vorschlagen",
            ),
            style = MaterialTheme.typography.bodySmall,
            textDecoration = TextDecoration.Underline,
        )
    }
}
