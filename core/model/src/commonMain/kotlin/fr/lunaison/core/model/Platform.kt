package fr.lunaison.core.model

/**
 * Code de langue de l'appareil sur deux lettres minuscules (par exemple `fr`), lu à chaque appel :
 * la langue suit le réglage du téléphone sans redémarrer l'appli.
 */
internal expect fun systemLanguageCode(): String

/** Région du téléphone (code ISO à deux lettres, en majuscules), ou chaîne vide si elle est inconnue. */
internal expect fun systemRegionCode(): String
