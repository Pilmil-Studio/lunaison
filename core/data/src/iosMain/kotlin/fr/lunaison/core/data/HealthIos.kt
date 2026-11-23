package fr.lunaison.core.data

/** HealthKit n'est pas encore branché : l'import depuis la santé du téléphone est indisponible sur iOS. */
actual fun platformHealthDataSource(platformContext: Any?): HealthDataSource = UnavailableHealthDataSource
