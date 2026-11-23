package fr.lunaison.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import fr.lunaison.core.data.HealthAvailability
import fr.lunaison.core.data.HealthConnectSource
import fr.lunaison.core.data.HealthReadResult
import fr.lunaison.core.data.HealthRecordsMapper
import fr.lunaison.core.data.ImportPlan
import fr.lunaison.core.data.ImportPreview
import fr.lunaison.core.data.ImportSource
import fr.lunaison.core.data.PasswordRequiredException
import fr.lunaison.core.data.UnsupportedEncryptionException
import fr.lunaison.core.data.platformHealthDataSource
import fr.lunaison.core.data.InvalidPasswordException
import fr.lunaison.core.data.PeriodCalendarImporter
import fr.lunaison.core.model.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Import d'un historique (Calendrier des règles, CSV Lunaison) : choix de la source, sélecteur de fichier du système,
 * mot de passe d'une archive chiffrée. Partagé par les réglages et l'accueil du premier lancement.
 * Tout est traité sur le téléphone ; [onResult] reçoit le message à afficher.
 */
@Composable
actual fun ImportDialogs(
    showSource: Boolean,
    onDismiss: () -> Unit,
    actions: Actions,
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingZipBytes by remember { mutableStateOf<ByteArray?>(null) }
    val importResult by rememberUpdatedState(onResult)
    // Tous les imports (Calendrier, CSV Lunaison, Health Connect, Clue) passent par un aperçu : rien n'est écrit avant la confirmation.
    var pendingPlan by remember { mutableStateOf<ImportPlan?>(null) }
    var passwordForClue by rememberSaveable { mutableStateOf(false) }
    val health = remember(context) { platformHealthDataSource(context) }

    suspend fun showPlan(preview: ImportPreview) {
        if (preview.daysCount == 0) importResult(importNothingFoundMessage(preview)) else pendingPlan = actions.planImport(preview)
    }

    suspend fun readHealth() {
        when (val result = health.read()) {
            is HealthReadResult.Data -> {
                val preview = HealthRecordsMapper.toPreview(result.data)
                if (preview.daysCount == 0) importResult(healthMessage(HealthMessage.NO_DATA)) else showPlan(preview)
            }
            HealthReadResult.PermissionDenied -> importResult(healthMessage(HealthMessage.DENIED))
            is HealthReadResult.Unavailable -> importResult(healthAvailabilityMessage(result.availability))
            is HealthReadResult.Failure -> importResult(healthMessage(HealthMessage.FAILURE, result.message))
        }
    }

    /**
     * Aperçu d'un fichier (Clue si [clue], sinon Calendrier ou CSV Lunaison). Rien n'est écrit.
     * Renvoie false si un mot de passe est (re)demandé, pour garder la fenêtre ouverte.
     */
    suspend fun previewBytes(bytes: ByteArray, password: String?, clue: Boolean): Boolean {
        try {
            showPlan(if (clue) actions.previewClue(bytes, password) else actions.previewFile(bytes, password))
            return true
        } catch (e: PasswordRequiredException) {
            pendingZipBytes = bytes
            passwordForClue = clue
            passwordInput = ""
            passwordError = null
            showPasswordDialog = true
            return false
        } catch (e: InvalidPasswordException) {
            passwordError = tr("Mot de passe incorrect.", "Incorrect password.", pl = "Nieprawidłowe hasło.", uk = "Неправильний пароль.", es = "Contraseña incorrecta.", de = "Falsches Passwort.")
            return false
        } catch (e: Exception) {
            val message = importPreviewErrorMessage(e)
                ?: tr("Erreur lors de l'import : ${e.localizedMessage}", "Import error: ${e.localizedMessage}", pl = "Błąd podczas importu: ${e.localizedMessage}", uk = "Помилка під час імпорту: ${e.localizedMessage}", es = "Error al importar: ${e.localizedMessage}", de = "Fehler beim Import: ${e.localizedMessage}")
            importResult(message)
            return true
        }
    }

    val healthPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        scope.launch {
            if (granted.containsAll(HealthConnectSource.PERMISSIONS)) readHealth() else importResult(healthMessage(HealthMessage.DENIED))
        }
    }

    val clueImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                when {
                    bytes == null || bytes.isEmpty() ->
                        importResult(tr("Fichier vide ou inaccessible.", "Empty or inaccessible file.", pl = "Plik jest pusty lub niedostępny.", uk = "Файл порожній або недоступний.", es = "Archivo vacío o inaccesible.", de = "Datei leer oder nicht zugänglich."))
                    PeriodCalendarImporter.isAesEncryptedZip(bytes) ->
                        importResult(importPreviewErrorMessage(UnsupportedEncryptionException()).orEmpty())
                    PeriodCalendarImporter.isEncryptedZip(bytes) -> {
                        pendingZipBytes = bytes
                        passwordForClue = true
                        passwordInput = ""
                        passwordError = null
                        showPasswordDialog = true
                    }
                    else -> previewBytes(bytes, null, clue = true)
                }
            }
        }
    }

    fun startHealthImport() {
        scope.launch {
            val status = health.availability()
            when {
                status != HealthAvailability.AVAILABLE -> importResult(healthAvailabilityMessage(status))
                health.hasPermission() -> readHealth()
                else -> healthPermissions.launch(HealthConnectSource.REQUESTED_PERMISSIONS)
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    if (bytes == null || bytes.isEmpty()) {
                        importResult(tr("Fichier vide ou inaccessible.", "Empty or inaccessible file.", pl = "Plik jest pusty lub niedostępny.", uk = "Файл порожній або недоступний.", es = "Archivo vacío o inaccesible.", de = "Datei leer oder nicht zugänglich."))
                        return@launch
                    }
                    if (isPilmilVault(bytes)) {
                        // Une sauvegarde .pilmil remplace les données : elle a son propre bouton, avec confirmation.
                        importResult(tr(
                            fr = "Ce fichier est une sauvegarde .pilmil : utilisez « Restaurer une sauvegarde ».",
                            en = "This file is a .pilmil backup: use \"Restore a backup\".",
                            pl = "To jest kopia .pilmil: użyj „Przywróć kopię zapasową”.",
                            uk = "Це резервна копія .pilmil: скористайтеся «Відновити резервну копію».",
                            es = "Este archivo es una copia .pilmil: usa «Restaurar una copia».",
                            de = "Diese Datei ist eine .pilmil-Sicherung: nutze „Sicherung wiederherstellen“.",
                        ))
                    } else if (PeriodCalendarImporter.isAesEncryptedZip(bytes)) {
                        importResult(importPreviewErrorMessage(UnsupportedEncryptionException()).orEmpty())
                    } else if (PeriodCalendarImporter.isZip(bytes) && PeriodCalendarImporter.isEncryptedZip(bytes)) {
                        pendingZipBytes = bytes
                        passwordForClue = false
                        passwordInput = ""
                        passwordError = null
                        showPasswordDialog = true
                    } else {
                        previewBytes(bytes, null, clue = false)
                    }
                } catch (e: Exception) {
                    importResult(tr("Erreur lors de l'import : ${e.localizedMessage}", "Import error: ${e.localizedMessage}", pl = "Błąd podczas importu: ${e.localizedMessage}", uk = "Помилка під час імпорту: ${e.localizedMessage}", es = "Error al importar: ${e.localizedMessage}", de = "Fehler beim Import: ${e.localizedMessage}"))
                }
            }
        }
    }

    if (showSource) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(tr("Importer des données", "Import data", pl = "Importuj dane", uk = "Імпорт даних", es = "Importar datos", de = "Daten importieren")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr(
                            fr = "De quelle application souhaitez-vous récupérer votre historique ?",
                            en = "Which app do you want to import your history from?",
                            pl = "Z której aplikacji chcesz zaimportować swoją historię?",
                            uk = "З якого застосунку ви хочете імпортувати свою історію?",
                            es = "¿De qué aplicación quieres recuperar tu historial?",
                            de = "Aus welcher App möchtest du deinen Verlauf importieren?",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Card(
                        onClick = {
                            onDismiss()
                            importer.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*"))
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                tr("Calendrier des règles", "Period Calendar", pl = "Kalendarz miesiączek", uk = "Календар менструацій", es = "Calendario menstrual", de = "Periodenkalender"),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                tr(
                                    fr = "Archive .zip (chiffrée par mot de passe) ou export .json de l'application.",
                                    en = ".zip archive (password-protected) or .json export from the app.",
                                    pl = "Archiwum .zip (zabezpieczone hasłem) lub eksport .json z aplikacji.",
                                    uk = "Архів .zip (захищений паролем) або експорт .json із застосунку.",
                                    es = "Archivo .zip (cifrado con contraseña) o exportación .json de la aplicación.",
                                    de = "Archiv .zip (passwortgeschützt) oder .json-Export der App.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Card(
                        onClick = {
                            onDismiss()
                            startHealthImport()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Health Connect", style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr(
                                    fr = "Lit, en lecture seule, les règles enregistrées par d'autres applis dans Health Connect. Rien ne se lance sans votre demande.",
                                    en = "Reads, read-only, the periods other apps saved in Health Connect. Nothing runs unless you ask.",
                                    pl = "Odczytuje (tylko do odczytu) miesiączki zapisane przez inne aplikacje w Health Connect. Nic nie działa bez Twojej prośby.",
                                    uk = "Лише читає менструації, які інші застосунки зберегли в Health Connect. Нічого не запускається без вашого запиту.",
                                    es = "Lee, solo en lectura, la menstruación que otras aplicaciones guardaron en Health Connect. No se ejecuta nada sin que lo pidas.",
                                    de = "Liest schreibgeschützt die Perioden, die andere Apps in Health Connect gespeichert haben. Ohne deine Aufforderung läuft nichts.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Card(
                        onClick = {
                            onDismiss()
                            clueImporter.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*"))
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(importSourceName(ImportSource.CLUE), style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr(
                                    fr = "Archive .zip protégée par mot de passe (ZipCrypto), fichier .json ou ancienne sauvegarde .cluedata. Format non documenté : vérifiez le résultat.",
                                    en = "Password-protected .zip archive (ZipCrypto), .json file or old .cluedata backup. Undocumented format: check the result.",
                                    pl = "Archiwum .zip chronione hasłem (ZipCrypto), plik .json lub stara kopia .cluedata. Format nieudokumentowany: sprawdź wynik.",
                                    uk = "Архів .zip із паролем (ZipCrypto), файл .json або стара копія .cluedata. Формат не задокументований: перевірте результат.",
                                    es = "Archivo .zip protegido con contraseña (ZipCrypto), archivo .json o copia antigua .cluedata. Formato no documentado: revisa el resultado.",
                                    de = "Passwortgeschütztes .zip-Archiv (ZipCrypto), .json-Datei oder alte .cluedata-Sicherung. Nicht dokumentiertes Format: Ergebnis prüfen.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Card(
                        onClick = {
                            onDismiss()
                            importer.launch(arrayOf("text/*", "text/comma-separated-values", "application/csv", "*/*"))
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                tr("Lunaison (CSV)", "Lunaison (CSV)", pl = "Lunaison (CSV)", uk = "Lunaison (CSV)", es = "Lunaison (CSV)", de = "Lunaison (CSV)"),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                tr(
                                    fr = "Fichier .csv exporté depuis Lunaison, quelle que soit la langue utilisée à l'export.",
                                    en = "A .csv file exported from Lunaison, whatever language was used for the export.",
                                    pl = "Plik .csv wyeksportowany z Lunaison, w dowolnym języku eksportu.",
                                    uk = "Файл .csv, експортований із Lunaison, якою б мовою не робили експорт.",
                                    es = "Archivo .csv exportado desde Lunaison, en cualquier idioma de exportación.",
                                    de = "Aus Lunaison exportierte .csv-Datei, in welcher Sprache auch immer exportiert.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(
                        tr(
                            fr = "D'autres applications (Flo, Drip...) seront ajoutées dans les prochaines mises à jour.",
                            en = "Other applications (Flo, Drip...) will be added in upcoming updates.",
                            pl = "Kolejne aplikacje (Flo, Drip...) zostaną dodane w następnych aktualizacjach.",
                            uk = "Інші застосунки (Flo, Drip...) буде додано в наступних оновленнях.",
                            es = "Se añadirán otras aplicaciones (Flo, Drip...) en próximas actualizaciones.",
                            de = "Weitere Apps (Flo, Drip...) werden in kommenden Updates hinzugefügt.",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen"))
                }
            },
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                pendingZipBytes = null
            },
            title = { Text(tr("Sauvegarde protégée", "Protected backup", pl = "Chroniona kopia zapasowa", uk = "Захищена резервна копія", es = "Copia de seguridad protegida", de = "Geschützte Sicherung")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (passwordForClue) tr(
                            fr = "Cette archive Clue est protégée par un mot de passe. Entrez celui que Clue vous a fourni :",
                            en = "This Clue archive is password-protected. Enter the password Clue gave you:",
                            pl = "To archiwum Clue jest chronione hasłem. Wpisz hasło, które podało Clue:",
                            uk = "Цей архів Clue захищений паролем. Введіть пароль, який надав Clue:",
                            es = "Este archivo de Clue está protegido con contraseña. Introduce la que te dio Clue:",
                            de = "Dieses Clue-Archiv ist passwortgeschützt. Gib das Passwort ein, das Clue dir mitgeteilt hat:",
                        ) else tr(
                            fr = "Cette sauvegarde de l'application « Calendrier » est chiffrée. Entrez le mot de passe fourni lors de l'exportation :",
                            en = "This backup from the \"Calendar\" app is encrypted. Enter the password provided during export:",
                            pl = "Ta kopia zapasowa z aplikacji „Kalendarz” jest zaszyfrowana. Wpisz hasło podane podczas eksportu:",
                            uk = "Ця резервна копія із застосунку «Календар» зашифрована. Введіть пароль, указаний під час експорту:",
                            es = "Esta copia de seguridad de la aplicación «Calendario» está cifrada. Introduce la contraseña indicada durante la exportación:",
                            de = "Diese Sicherung der App „Kalender“ ist verschlüsselt. Gib das beim Export vergebene Passwort ein:",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text(tr("Mot de passe", "Password", pl = "Hasło", uk = "Пароль", es = "Contraseña", de = "Passwort")) },
                        singleLine = true,
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (passwordError != null) {
                        Text(
                            passwordError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bytes = pendingZipBytes
                        if (bytes != null) {
                            scope.launch {
                                if (previewBytes(bytes, passwordInput.trim(), clue = passwordForClue)) {
                                    showPasswordDialog = false
                                    pendingZipBytes = null
                                }
                            }
                        }
                    },
                ) {
                    Text(tr("Déchiffrer et afficher l'aperçu", "Decrypt and preview", pl = "Odszyfruj i pokaż podgląd", uk = "Розшифрувати й переглянути", es = "Descifrar y ver la vista previa", de = "Entschlüsseln und Vorschau anzeigen"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        pendingZipBytes = null
                    },
                ) {
                    Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen"))
                }
            },
        )
    }

    pendingPlan?.let { plan ->
        ImportPlanDialog(
            plan = plan,
            onDismiss = { pendingPlan = null },
            onConfirm = {
                pendingPlan = null
                scope.launch {
                    try {
                        val written = actions.applyImport(plan)
                        importResult(importAppliedMessage(written, plan.keptExisting))
                    } catch (e: Exception) {
                        importResult(tr("Erreur lors de l'import : ${e.localizedMessage}", "Import error: ${e.localizedMessage}", pl = "Błąd podczas importu: ${e.localizedMessage}", uk = "Помилка під час імпорту: ${e.localizedMessage}", es = "Error al importar: ${e.localizedMessage}", de = "Fehler beim Import: ${e.localizedMessage}"))
                    }
                }
            },
        )
    }
}

private val PILMIL_MAGIC = byteArrayOf(0x50, 0x49, 0x4C, 0x4D, 0x49, 0x4C) // "PILMIL"

private fun isPilmilVault(bytes: ByteArray): Boolean =
    bytes.size >= 7 && PILMIL_MAGIC.indices.all { i -> bytes[i] == PILMIL_MAGIC[i] }

private enum class HealthMessage { NO_DATA, DENIED, FAILURE }

private fun healthMessage(kind: HealthMessage, detail: String? = null): String = when (kind) {
    HealthMessage.NO_DATA -> tr(
        fr = "Aucune donnée de règles trouvée dans Health Connect. Vérifiez que l'autre appli y enregistre bien ses règles. Health Connect ne donne accès qu'aux 30 jours qui précèdent l'autorisation.",
        en = "No period data found in Health Connect. Check that the other app does save its periods there. Health Connect only gives access to the 30 days before the permission was granted.",
        pl = "Nie znaleziono w Health Connect danych o miesiączkach. Sprawdź, czy druga aplikacja zapisuje tam swoje dane. Health Connect udostępnia tylko 30 dni sprzed przyznania zgody.",
        uk = "У Health Connect не знайдено даних про менструації. Перевірте, чи інший застосунок записує їх туди. Health Connect дає доступ лише до 30 днів перед наданням дозволу.",
        es = "No se encontraron datos de menstruación en Health Connect. Comprueba que la otra aplicación los guarde allí. Health Connect solo da acceso a los 30 días anteriores a la concesión del permiso.",
        de = "In Health Connect wurden keine Periodendaten gefunden. Prüfe, ob die andere App ihre Daten dort speichert. Health Connect gibt nur Zugriff auf die 30 Tage vor der Berechtigung.",
    )
    HealthMessage.DENIED -> tr(
        fr = "Permission refusée : Lunaison n'a pas accès aux règles de Health Connect. Vous pouvez l'accorder dans les réglages de Health Connect, rubrique des autorisations des applications.",
        en = "Permission denied: Lunaison cannot read periods from Health Connect. You can grant it in Health Connect settings, under app permissions.",
        pl = "Odmowa dostępu: Lunaison nie może odczytać miesiączek z Health Connect. Możesz przyznać zgodę w ustawieniach Health Connect, w uprawnieniach aplikacji.",
        uk = "Дозвіл не надано: Lunaison не може читати менструації з Health Connect. Ви можете надати його в налаштуваннях Health Connect, у розділі дозволів застосунків.",
        es = "Permiso denegado: Lunaison no puede leer la menstruación de Health Connect. Puedes concederlo en los ajustes de Health Connect, en los permisos de las aplicaciones.",
        de = "Berechtigung verweigert: Lunaison kann keine Periodendaten aus Health Connect lesen. Du kannst sie in den Health-Connect-Einstellungen unter den App-Berechtigungen erteilen.",
    )
    HealthMessage.FAILURE -> tr(
        fr = "Lecture de Health Connect impossible : $detail",
        en = "Could not read Health Connect: $detail",
        pl = "Nie można odczytać Health Connect: $detail",
        uk = "Не вдалося прочитати Health Connect: $detail",
        es = "No se pudo leer Health Connect: $detail",
        de = "Health Connect konnte nicht gelesen werden: $detail",
    )
}

private fun healthAvailabilityMessage(status: HealthAvailability): String = when (status) {
    HealthAvailability.NOT_INSTALLED -> tr(
        fr = "Health Connect n'est pas installé sur ce téléphone. Installez-le depuis le Play Store, puis réessayez.",
        en = "Health Connect is not installed on this phone. Install it from the Play Store, then try again.",
        pl = "Health Connect nie jest zainstalowany na tym telefonie. Zainstaluj go ze sklepu Play i spróbuj ponownie.",
        uk = "Health Connect не встановлено на цьому телефоні. Установіть його з Google Play і спробуйте ще раз.",
        es = "Health Connect no está instalado en este teléfono. Instálalo desde Play Store y vuelve a intentarlo.",
        de = "Health Connect ist auf diesem Telefon nicht installiert. Installiere es aus dem Play Store und versuche es erneut.",
    )
    HealthAvailability.UPDATE_REQUIRED -> tr(
        fr = "Health Connect doit être mis à jour. Mettez-le à jour depuis le Play Store, puis réessayez.",
        en = "Health Connect needs an update. Update it from the Play Store, then try again.",
        pl = "Health Connect wymaga aktualizacji. Zaktualizuj go w sklepie Play i spróbuj ponownie.",
        uk = "Health Connect потрібно оновити. Оновіть його в Google Play і спробуйте ще раз.",
        es = "Health Connect necesita una actualización. Actualízalo desde Play Store y vuelve a intentarlo.",
        de = "Health Connect muss aktualisiert werden. Aktualisiere es im Play Store und versuche es erneut.",
    )
    HealthAvailability.NOT_SUPPORTED, HealthAvailability.AVAILABLE -> tr(
        fr = "Health Connect n'est pas disponible sur cet appareil.",
        en = "Health Connect is not available on this device.",
        pl = "Health Connect nie jest dostępny na tym urządzeniu.",
        uk = "Health Connect недоступний на цьому пристрої.",
        es = "Health Connect no está disponible en este dispositivo.",
        de = "Health Connect ist auf diesem Gerät nicht verfügbar.",
    )
}
