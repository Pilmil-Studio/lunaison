package fr.lunaison.core.data

/**
 * Lit un fichier d'une autre appli de suivi (archive Zip, JSON ou CSV) et en fait un aperçu, SANS rien écrire.
 * L'écriture passe ensuite par [planImport] puis [applyImport] : les journées déjà notées ne sont jamais modifiées.
 *
 * Android : archive Zip ou JSON du Calendrier des règles, ou CSV Lunaison. iOS : CSV Lunaison seulement
 * (l'appli d'origine du Calendrier n'existe pas sur iOS, où les autres formats échouent).
 * Lève `PasswordRequiredException` ou `InvalidPasswordException` pour une archive chiffrée,
 * [UnsupportedEncryptionException], [UnreadableImportException] ou [NothingToImportException].
 */
expect fun previewAuto(bytes: ByteArray, password: String? = null): ImportPreview
