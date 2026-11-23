package fr.lunaison.core.data

/**
 * Lit un export Clue directement en mémoire (le fichier n'est jamais copié) et en fait un aperçu, sans rien écrire.
 * Accepte un fichier `.json`, un fichier `.cluedata` (JSON) ou, sur Android, l'archive ZIP de Clue
 * (protégée par ZipCrypto avec [password] ; le chiffrement AES n'est pas pris en charge).
 *
 * Lève `PasswordRequiredException` (Android) si l'archive est chiffrée sans mot de passe, `InvalidPasswordException` si le mot de passe est faux,
 * [UnsupportedEncryptionException], [UnreadableImportException] ou [NothingToImportException].
 */
expect fun previewClueFile(bytes: ByteArray, password: String? = null): ImportPreview
