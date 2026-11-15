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
import fr.lunaison.core.data.PinThrottleState
import fr.lunaison.core.model.DayLog
import androidx.compose.ui.unit.dp
import fr.lunaison.core.designsystem.LunaButton
import fr.lunaison.core.designsystem.LunaButtonStyle
import fr.lunaison.core.model.tr
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
                    notify(tr("Sauvegarde chiffrée (.pilmil) enregistrée.", "Encrypted backup (.pilmil) saved."))
                } catch (e: Exception) {
                    notify(tr("Erreur : ${e.localizedMessage}", "Error: ${e.localizedMessage}"))
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
            title = { androidx.compose.material3.Text(tr("Mot de passe de la sauvegarde", "Backup password")) },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        tr(
                            "Ce mot de passe chiffrera votre fichier .pilmil (AES-256-GCM). Conservez-le précieusement : il est impossible de le récupérer sans.",
                            "This password will encrypt your .pilmil file (AES-256-GCM). Keep it safe: it cannot be recovered without it.",
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { androidx.compose.material3.Text(tr("Mot de passe", "Password")) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { androidx.compose.material3.Text(tr("Confirmer", "Confirm")) },
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
                ) { androidx.compose.material3.Text(tr("Exporter", "Export")) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    androidx.compose.material3.Text(tr("Annuler", "Cancel"))
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

// Android : la sauvegarde automatique est désactivée dans le manifeste, rien à régler.
actual val cloudBackupControllable: Boolean = false

actual fun isCloudBackupEnabled(): Boolean = false

actual fun setCloudBackupEnabled(enabled: Boolean) = Unit

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
