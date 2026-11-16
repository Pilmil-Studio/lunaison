package fr.lunaison.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import fr.lunaison.core.cycle.HealthReport
import fr.lunaison.core.cycle.Prediction
import fr.lunaison.core.data.AppSettings
import fr.lunaison.core.data.BackupFailure
import fr.lunaison.core.data.BackupFormatException
import fr.lunaison.core.data.BackupRestoreSummary
import fr.lunaison.core.data.PinThrottleState
import fr.lunaison.core.model.DayLog
import androidx.compose.ui.unit.dp
import fr.lunaison.core.designsystem.LunaButton
import fr.lunaison.core.designsystem.LunaButtonStyle
import fr.lunaison.core.model.tr
import fr.lunaison.ui.backup.BackupPolicy
import fr.lunaison.ui.backup.CloudBackupConsent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

actual typealias PlatformContext = Context

@Composable
actual fun rememberPlatformContext(): PlatformContext = LocalContext.current

actual fun openLink(context: PlatformContext, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // Aucun navigateur : rien à faire.
    }
}

private const val PACKAGE_NAME = "fr.lunaison"

actual fun openStore(context: PlatformContext) {
    val marketUri = Uri.parse("market://details?id=$PACKAGE_NAME")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE_NAME")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: ActivityNotFoundException) {
            // Aucun magasin ni navigateur disponible
        }
    }
}

actual val storePageAvailable: Boolean = true

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

actual val platformName: String = "Android"

actual fun showToast(context: PlatformContext, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

/** Les notifications peuvent être envoyées : autorisation accordée (Android 13 et plus) et non coupées dans les réglages d'Android. */
private fun notificationsAllowed(context: Context): Boolean =
    runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(false)

@Composable
actual fun rememberNotificationAccess(onAnswered: (granted: Boolean) -> Unit): NotificationAccess {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val answered by rememberUpdatedState(onAnswered)

    // Au retour des réglages d'Android, l'état des notifications peut avoir changé.
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted || notificationsAllowed(context)
        answered(granted)
    }
    return NotificationAccess(
        allowed = allowed,
        request = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                allowed = notificationsAllowed(context)
                answered(allowed)
            }
        },
        openSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        },
    )
}

@Composable
actual fun rememberFileExporter(mimeType: String, content: suspend () -> String): (fileName: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val produce by rememberUpdatedState(content)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        if (uri != null) {
            scope.launch {
                val text = produce()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }
            }
        }
    }
    return { fileName -> launcher.launch(fileName) }
}

actual val encryptedBackupAvailable: Boolean = true

private fun passwordLabel() = tr("Mot de passe", "Password", pl = "Hasło", uk = "Пароль", es = "Contraseña", de = "Passwort")
private fun cancelLabel() = tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen")

@Composable
actual fun EncryptedBackupButton(
    label: String,
    fileName: String,
    onProducePayload: suspend () -> String,
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val produce by rememberUpdatedState(onProducePayload)
    val notify by rememberUpdatedState(onResult)
    var showDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var passphrase by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var pendingPassphrase by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<CharArray?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pwd = pendingPassphrase
        pendingPassphrase = null
        if (uri != null && pwd != null) {
            scope.launch {
                try {
                    val payload = produce()
                    val encrypted = withContext(Dispatchers.Default) {
                        fr.pilmil.core.platform.backup.AndroidBackupVault().encrypt(payload, pwd)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) }
                    }
                    notify(tr("Sauvegarde chiffrée (.pilmil) enregistrée.", "Encrypted backup (.pilmil) saved.", pl = "Zaszyfrowana kopia (.pilmil) zapisana.", uk = "Зашифровану копію (.pilmil) збережено.", es = "Copia cifrada (.pilmil) guardada.", de = "Verschlüsselte Sicherung (.pilmil) gespeichert."))
                } catch (e: Exception) {
                    notify(tr("Erreur : ${e.localizedMessage}", "Error: ${e.localizedMessage}", pl = "Błąd: ${e.localizedMessage}", uk = "Помилка: ${e.localizedMessage}", es = "Error: ${e.localizedMessage}", de = "Fehler: ${e.localizedMessage}"))
                } finally {
                    pwd.fill('\u0000')
                }
            }
        }
    }

    LunaButton(label, onClick = {
        passphrase = ""
        confirm = ""
        showDialog = true
    }, style = LunaButtonStyle.Outlined)

    if (showDialog) {
        val mismatch = confirm.isNotEmpty() && passphrase != confirm
        val valid = passphrase.isNotEmpty() && passphrase == confirm
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { androidx.compose.material3.Text(tr("Mot de passe de la sauvegarde", "Backup password", pl = "Hasło kopii zapasowej", uk = "Пароль резервної копії", es = "Contraseña de la copia", de = "Passwort der Sicherung")) },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        tr(
                            "Ce mot de passe chiffre votre fichier .pilmil (AES-256-GCM). Il contient tout votre journal et vos réglages. Conservez-le : sans lui, le fichier est illisible. Le code PIN n'est pas sauvegardé.",
                            "This password encrypts your .pilmil file (AES-256-GCM). It holds your whole log and your settings. Keep it: without it the file cannot be read. The PIN code is not backed up.",
                            pl = "To hasło szyfruje plik .pilmil (AES-256-GCM). Zawiera cały dziennik i ustawienia. Zachowaj je: bez niego plik jest nieczytelny. Kod PIN nie jest zapisywany w kopii.",
                            uk = "Цей пароль шифрує ваш файл .pilmil (AES-256-GCM). У ньому весь щоденник і налаштування. Збережіть його: без нього файл не прочитати. PIN-код не зберігається в копії.",
                            es = "Esta contraseña cifra tu archivo .pilmil (AES-256-GCM). Contiene todo tu diario y tus ajustes. Guárdala: sin ella no se puede leer el archivo. El código PIN no se incluye en la copia.",
                            de = "Dieses Passwort verschlüsselt deine .pilmil-Datei (AES-256-GCM). Sie enthält dein ganzes Tagebuch und deine Einstellungen. Bewahre es auf: ohne es ist die Datei nicht lesbar. Der PIN-Code wird nicht gesichert.",
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { androidx.compose.material3.Text(passwordLabel()) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { androidx.compose.material3.Text(tr("Confirmer", "Confirm", pl = "Potwierdź", uk = "Підтвердити", es = "Confirmar", de = "Bestätigen")) },
                        singleLine = true,
                        isError = mismatch,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = valid,
                    onClick = {
                        pendingPassphrase = passphrase.toCharArray()
                        passphrase = ""
                        confirm = ""
                        showDialog = false
                        launcher.launch(fileName)
                    },
                ) { androidx.compose.material3.Text(tr("Exporter", "Export", pl = "Eksportuj", uk = "Експортувати", es = "Exportar", de = "Exportieren")) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    androidx.compose.material3.Text(cancelLabel())
                }
            },
        )
    }
}

private val PILMIL_FILE_MAGIC = "PILMIL".toByteArray(Charsets.US_ASCII)

@Composable
actual fun RestoreBackupButton(
    label: String,
    onRestore: suspend (json: String) -> BackupRestoreSummary,
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val restore by rememberUpdatedState(onRestore)
    val notify by rememberUpdatedState(onResult)
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var passphrase by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    val corrupted = tr(
        "Ce fichier est corrompu ou n'est pas une sauvegarde Lunaison. Rien n'a été modifié.",
        "This file is corrupted or is not a Lunaison backup. Nothing was changed.",
        pl = "Ten plik jest uszkodzony lub nie jest kopią Lunaison. Nic nie zmieniono.",
        uk = "Цей файл пошкоджено або це не резервна копія Lunaison. Нічого не змінено.",
        es = "Este archivo está dañado o no es una copia de Lunaison. No se ha modificado nada.",
        de = "Diese Datei ist beschädigt oder keine Lunaison-Sicherung. Es wurde nichts geändert.",
    )

    fun close() {
        pendingBytes = null
        passphrase = ""
        understood = false
        error = null
        working = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = try {
                    withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                } catch (e: Exception) {
                    null
                }
                when {
                    bytes == null || bytes.isEmpty() -> notify(tr("Fichier vide ou inaccessible.", "Empty or inaccessible file.", pl = "Plik jest pusty lub niedostępny.", uk = "Файл порожній або недоступний.", es = "Archivo vacío o inaccesible.", de = "Datei leer oder nicht zugänglich."))
                    bytes.size < PILMIL_FILE_MAGIC.size || !PILMIL_FILE_MAGIC.indices.all { bytes[it] == PILMIL_FILE_MAGIC[it] } -> notify(corrupted)
                    else -> {
                        close()
                        pendingBytes = bytes
                    }
                }
            }
        }
    }

    LunaButton(label, onClick = { picker.launch(arrayOf("*/*")) }, style = LunaButtonStyle.Outlined)

    val bytes = pendingBytes
    if (bytes != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!working) close() },
            title = { androidx.compose.material3.Text(tr("Restaurer une sauvegarde", "Restore a backup", pl = "Przywróć kopię zapasową", uk = "Відновити резервну копію", es = "Restaurar una copia", de = "Sicherung wiederherstellen")) },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        tr(
                            "Entrez le mot de passe de ce fichier .pilmil. La restauration remplace les données actuelles de ce téléphone (journal et réglages). Le code PIN de ce téléphone est conservé.",
                            "Enter the password of this .pilmil file. Restoring replaces the current data on this phone (log and settings). This phone's PIN code is kept.",
                            pl = "Wpisz hasło do tego pliku .pilmil. Przywrócenie zastępuje obecne dane na tym telefonie (dziennik i ustawienia). Kod PIN tego telefonu zostaje zachowany.",
                            uk = "Введіть пароль до цього файлу .pilmil. Відновлення замінює поточні дані на цьому телефоні (щоденник і налаштування). PIN-код цього телефона зберігається.",
                            es = "Introduce la contraseña de este archivo .pilmil. La restauración reemplaza los datos actuales de este teléfono (diario y ajustes). Se conserva el código PIN de este teléfono.",
                            de = "Gib das Passwort dieser .pilmil-Datei ein. Die Wiederherstellung ersetzt die aktuellen Daten auf diesem Telefon (Tagebuch und Einstellungen). Der PIN-Code dieses Telefons bleibt erhalten.",
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it; error = null },
                        label = { androidx.compose.material3.Text(passwordLabel()) },
                        singleLine = true,
                        isError = error != null,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = understood, onCheckedChange = { understood = it })
                        androidx.compose.material3.Text(
                            tr(
                                "Je comprends que cela remplace les données actuelles",
                                "I understand this replaces the current data",
                                pl = "Rozumiem, że to zastąpi obecne dane",
                                uk = "Розумію, що це замінить поточні дані",
                                es = "Entiendo que esto reemplaza los datos actuales",
                                de = "Ich verstehe, dass dies die aktuellen Daten ersetzt",
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                    error?.let {
                        androidx.compose.material3.Text(
                            it,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = understood && passphrase.isNotEmpty() && !working,
                    onClick = {
                        working = true
                        val pwd = passphrase.toCharArray()
                        scope.launch {
                            try {
                                val json = withContext(Dispatchers.Default) {
                                    fr.pilmil.core.platform.backup.AndroidBackupVault().decrypt(bytes, pwd)
                                }
                                val summary = restore(json)
                                close()
                                notify(
                                    tr(
                                        "Sauvegarde restaurée : ${summary.daysCount} journées.",
                                        "Backup restored: ${summary.daysCount} days.",
                                        pl = "Kopia przywrócona: dni: ${summary.daysCount}.",
                                        uk = "Копію відновлено: днів: ${summary.daysCount}.",
                                        es = "Copia restaurada: ${summary.daysCount} días.",
                                        de = "Sicherung wiederhergestellt: ${summary.daysCount} Tage.",
                                    ),
                                )
                            } catch (e: fr.pilmil.core.platform.backup.InvalidPassphraseException) {
                                working = false
                                error = tr(
                                    "Mot de passe incorrect, ou fichier altéré. Rien n'a été modifié.",
                                    "Wrong password, or the file was altered. Nothing was changed.",
                                    pl = "Nieprawidłowe hasło lub plik został zmieniony. Nic nie zmieniono.",
                                    uk = "Неправильний пароль або файл змінено. Нічого не змінено.",
                                    es = "Contraseña incorrecta o archivo alterado. No se ha modificado nada.",
                                    de = "Falsches Passwort oder Datei verändert. Es wurde nichts geändert.",
                                )
                            } catch (e: BackupFormatException) {
                                working = false
                                error = if (e.failure == BackupFailure.UNSUPPORTED_VERSION) {
                                    tr(
                                        "Cette sauvegarde vient d'une version plus récente de Lunaison. Mettez l'appli à jour. Rien n'a été modifié.",
                                        "This backup comes from a newer version of Lunaison. Update the app. Nothing was changed.",
                                        pl = "Ta kopia pochodzi z nowszej wersji Lunaison. Zaktualizuj aplikację. Nic nie zmieniono.",
                                        uk = "Ця копія з новішої версії Lunaison. Оновіть застосунок. Нічого не змінено.",
                                        es = "Esta copia viene de una versión más reciente de Lunaison. Actualiza la app. No se ha modificado nada.",
                                        de = "Diese Sicherung stammt aus einer neueren Lunaison-Version. Aktualisiere die App. Es wurde nichts geändert.",
                                    )
                                } else {
                                    corrupted
                                }
                            } catch (e: fr.pilmil.core.platform.backup.BackupException) {
                                working = false
                                error = corrupted
                            } catch (e: Exception) {
                                working = false
                                error = tr("Erreur : ${e.localizedMessage}", "Error: ${e.localizedMessage}", pl = "Błąd: ${e.localizedMessage}", uk = "Помилка: ${e.localizedMessage}", es = "Error: ${e.localizedMessage}", de = "Fehler: ${e.localizedMessage}")
                            } finally {
                                pwd.fill('\u0000')
                            }
                        }
                    },
                ) {
                    androidx.compose.material3.Text(tr("Remplacer les données", "Replace data", pl = "Zastąp dane", uk = "Замінити дані", es = "Reemplazar datos", de = "Daten ersetzen"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(enabled = !working, onClick = { close() }) {
                    androidx.compose.material3.Text(cancelLabel())
                }
            },
        )
    }
}

@Composable
actual fun rememberContactPicker(onPicked: (name: String, phone: String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val picked by rememberUpdatedState(onPicked)
    // Le sélecteur de contacts du système ne donne accès qu'au contact choisi : aucune autorisation n'est nécessaire.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                picked(cursor.getString(0).orEmpty().substringBefore(" "), cursor.getString(1).orEmpty())
            }
        }
    }
    return {
        try {
            picker.launch(Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
        } catch (_: ActivityNotFoundException) {
            // Pas de carnet d'adresses : la saisie manuelle reste possible.
        }
    }
}

actual val pdfReportAvailable: Boolean = true

actual suspend fun shareHealthReportPdf(
    context: PlatformContext,
    report: HealthReport,
    medicationLabel: String,
    logs: List<DayLog>,
    sections: Set<ReportSection>,
) {
    val file = withContext(Dispatchers.IO) {
        HealthReportPdf.export(context, buildReportDocument(report, medicationLabel, logs, sections))
    }
    sharePdf(context, file)
}

/** Données du bilan gardées le temps que la personne choisisse où enregistrer le fichier. */
private class PendingReport(
    val report: HealthReport,
    val medicationLabel: String,
    val logs: List<DayLog>,
    val sections: Set<ReportSection>,
)

@Composable
actual fun rememberHealthReportPdfSaver(
    onDone: (success: Boolean) -> Unit,
): ((report: HealthReport, medicationLabel: String, logs: List<DayLog>, sections: Set<ReportSection>) -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    var pending by remember { mutableStateOf<PendingReport?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val request = pending
        pending = null
        if (uri != null && request != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val document = buildReportDocument(request.report, request.medicationLabel, request.logs, request.sections)
                        val file = HealthReportPdf.export(context, document)
                        val output = context.contentResolver.openOutputStream(uri)
                        if (output == null) {
                            false
                        } else {
                            output.use { out -> file.inputStream().use { it.copyTo(out) } }
                            true
                        }
                    } catch (_: Exception) {
                        false
                    }
                }
                done(success)
            }
        }
    }
    return { report, medicationLabel, logs, sections ->
        pending = PendingReport(report, medicationLabel, logs, sections)
        try {
            launcher.launch(HealthReportPdf.FILE_NAME)
        } catch (_: ActivityNotFoundException) {
            pending = null
            done(false)
        }
    }
}

/** Ouvre le sélecteur de partage pour le PDF, lu par les autres applis via le FileProvider de Lunaison. */
private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fichiers", file)
    val subject = tr(
        fr = "Bilan de cycle Lunaison",
        en = "Lunaison cycle health report",
        pl = "Raport o cyklu z Lunaison",
        uk = "Звіт про цикл із Lunaison",
        es = "Informe del ciclo de Lunaison",
        de = "Zyklusbericht von Lunaison",
    )
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(file.name, uri)
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (_: ActivityNotFoundException) {
        val message = tr(
            fr = "Aucune appli pour partager ce PDF.",
            en = "No app available to share this PDF.",
            pl = "Brak aplikacji do udostępnienia tego pliku PDF.",
            uk = "Немає застосунку, щоб поділитися цим PDF.",
            es = "No hay ninguna aplicación para compartir este PDF.",
            de = "Keine App zum Teilen dieses PDFs verfügbar.",
        )
        showToast(context, message)
    }
}

actual val importAvailable: Boolean = true

// Android : copie dans le compte Google désactivée par défaut, activable à partir d'Android 12 (voir LunaisonBackupAgent).
actual val cloudBackupControllable: Boolean = BackupPolicy.settingAvailable(Build.VERSION.SDK_INT)

actual val cloudBackupOptIn: Boolean = true

// Android : WorkManager observe déjà le dépôt dans LunaisonApplication.
actual fun syncReminders(settings: AppSettings, prediction: Prediction?) = Unit

actual fun currentElapsedRealtimeMs(): Long = android.os.SystemClock.elapsedRealtime()

@Composable
actual fun rememberPinThrottleStore(): PinThrottleStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val prefs = context.getSharedPreferences("lunaison_pin_throttle", Context.MODE_PRIVATE)
        object : PinThrottleStore {
            override fun load(): PinThrottleState = PinThrottleState(
                failures = prefs.getInt("failures", 0),
                blockedUntil = prefs.getLong("blocked_until", 0L),
                savedAt = prefs.getLong("saved_at", 0L),
                lastDelayMs = prefs.getLong("last_delay_ms", 0L),
            )

            override fun save(state: PinThrottleState) {
                prefs.edit()
                    .putInt("failures", state.failures)
                    .putLong("blocked_until", state.blockedUntil)
                    .putLong("saved_at", state.savedAt)
                    .putLong("last_delay_ms", state.lastDelayMs)
                    .apply()
            }
        }
    }
}

// Le dossier peut être absent hors d'un vrai téléphone (captures d'écran) : la copie est alors désactivée.
actual fun isCloudBackupEnabled(context: PlatformContext): Boolean =
    context.noBackupFilesDir?.let { CloudBackupConsent(it).enabled } ?: false

// Hors d'un vrai téléphone (captures d'écran), le service du verrouillage peut manquer : le réglage est alors indisponible.
actual fun cloudBackupReady(context: PlatformContext): Boolean = runCatching {
    (context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isDeviceSecure == true
}.getOrDefault(false)

actual fun setCloudBackupEnabled(context: PlatformContext, enabled: Boolean) {
    context.noBackupFilesDir?.let { CloudBackupConsent(it).enabled = enabled }
}
