package fr.lunaison.core.model

import java.util.Locale

internal actual fun systemLanguageCode(): String = Locale.getDefault().language

internal actual fun systemRegionCode(): String = Locale.getDefault().country.uppercase()
