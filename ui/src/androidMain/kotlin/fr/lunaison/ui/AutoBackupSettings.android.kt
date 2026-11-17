package fr.lunaison.ui

import android.app.KeyguardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import fr.lunaison.core.designsystem.LunaButton
import fr.lunaison.core.designsystem.LunaButtonStyle
import fr.lunaison.core.model.tr
import fr.lunaison.ui.backup.AutoBackup
import fr.lunaison.ui.backup.AutoBackupStatus
import fr.lunaison.ui.backup.BackupPolicy
import fr.lunaison.ui.backup.CloudCopyBlocker
import fr.lunaison.ui.backup.PassphraseRules
import fr.lunaison.ui.backup.PassphraseStrength
import fr.lunaison.ui.backup.RunResult
import fr.lunaison.ui.backup.SafFolder
import fr.lunaison.ui.backup.SkipReason
import fr.lunaison.ui.backup.backupProblemMessage
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private enum class BackupDialog { SETUP, CHANGE_PHRASE, DISABLE }

private enum class FolderPurpose { SETUP, CHANGE }

private fun formatMoment(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

private fun describe(result: RunResult, justConfigured: Boolean): String = when (result) {
    RunResult.Saved -> tr(
        "Sauvegarde réussie.",
        "Backup saved.",
        pl = "Kopia zapisana.",
        uk = "Копію збережено.",
        es = "Copia guardada.",
        de = "Sicherung gespeichert.",
    )
    is RunResult.Skipped -> if (justConfigured || result.reason == SkipReason.NOTHING_TO_SAVE) {
        tr(
            "La sauvegarde automatique est activée. Il n'y a encore rien à sauvegarder : la première copie sera écrite dès que vous aurez noté des journées.",
            "Automatic backup is on. There is nothing to back up yet: the first copy will be written once you have logged some days.",
            pl = "Automatyczna kopia jest włączona. Nie ma jeszcze czego kopiować: pierwsza kopia powstanie, gdy zanotujesz jakieś dni.",
            uk = "Автоматичну копію ввімкнено. Поки що нічого копіювати: першу копію буде записано, щойно ви додасте записи.",
            es = "La copia automática está activada. Todavía no hay nada que copiar: la primera copia se escribirá en cuanto anotes algunos días.",
            de = "Die automatische Sicherung ist aktiv. Es gibt noch nichts zu sichern: Die erste Kopie wird geschrieben, sobald du Tage eingetragen hast.",
        )
    } else {
        tr(
            "Rien à faire : la sauvegarde est déjà à jour.",
            "Nothing to do: the backup is already up to date.",
            pl = "Nic do zrobienia: kopia jest aktualna.",
            uk = "Нічого робити: копія актуальна.",
            es = "Nada que hacer: la copia ya está al día.",
            de = "Nichts zu tun: Die Sicherung ist schon aktuell.",
        )
    }
    is RunResult.Failed -> backupProblemMessage(result.problem)
}

@Composable
actual fun AutoBackupSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val status by produceState<AutoBackupStatus>(AutoBackupStatus.Unavailable, refresh) { value = AutoBackup.status(context) }
    var dialog by remember { mutableStateOf<BackupDialog?>(null) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingFolder by remember { mutableStateOf<Uri?>(null) }
    var pendingFolderName by remember { mutableStateOf<String?>(null) }
    var purpose by remember { mutableStateOf(FolderPurpose.SETUP) }
    var phrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }

    fun finish(result: RunResult, justConfigured: Boolean = false) {
        working = false
        message = describe(result, justConfigured)
        refresh++
    }

    fun closeDialog() {
        dialog = null
        phrase = ""
        confirmation = ""
        understood = false
        pendingFolder = null
        pendingFolderName = null
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            when (purpose) {
                FolderPurpose.SETUP -> {
                    pendingFolder = uri
                    pendingFolderName = runCatching { SafFolder(context, uri).displayName() }.getOrNull()
                }
                FolderPurpose.CHANGE -> {
                    working = true
                    message = null
                    scope.launch { finish(AutoBackup.changeFolder(context, uri)) }
                }
            }
        }
    }

    when (val current = status) {
        AutoBackupStatus.Unavailable -> Unit
        is AutoBackupStatus.NotConfigured -> {
            Text(
                tr(
                    "Lunaison peut enregistrer chaque jour une copie chiffrée de vos données dans un dossier de votre choix. Elle est protégée par une phrase secrète que vous définissez : cette phrase reste sur ce téléphone et n'est envoyée nulle part.",
                    "Lunaison can save an encrypted copy of your data every day in a folder you choose. It is protected by a secret phrase that you define. The phrase stays on this phone and is sent nowhere.",
                    pl = "Lunaison może codziennie zapisywać zaszyfrowaną kopię Twoich danych w wybranym folderze. Chroni ją tajna fraza, którą ustalasz Ty. Fraza zostaje na tym telefonie i nie jest nigdzie wysyłana.",
                    uk = "Lunaison може щодня зберігати зашифровану копію ваших даних у вибраній вами теці. Її захищає таємна фраза, яку ви задаєте. Фраза залишається на цьому телефоні й нікуди не надсилається.",
                    es = "Lunaison puede guardar cada día una copia cifrada de tus datos en una carpeta que elijas. La protege una frase secreta que defines tú. La frase se queda en este teléfono y no se envía a ningún sitio.",
                    de = "Lunaison kann jeden Tag eine verschlüsselte Kopie deiner Daten in einem Ordner deiner Wahl speichern. Sie ist durch eine geheime Phrase geschützt, die du festlegst. Die Phrase bleibt auf diesem Handy und wird nirgendwohin gesendet.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LunaButton(
                tr(
                    "Configurer la sauvegarde automatique",
                    "Set up automatic backup",
                    pl = "Skonfiguruj automatyczną kopię",
                    uk = "Налаштувати автоматичну копію",
                    es = "Configurar la copia automática",
                    de = "Automatische Sicherung einrichten",
                ),
                onClick = {
                    message = null
                    dialog = BackupDialog.SETUP
                },
                style = LunaButtonStyle.Outlined,
            )
            CloudCopySetting(passphraseSet = false, refresh = refresh)
        }
        is AutoBackupStatus.Configured -> {
            Text(
                current.lastSuccessMillis?.let {
                    tr(
                        "Dernière sauvegarde réussie : ${formatMoment(it)}",
                        "Last successful backup: ${formatMoment(it)}",
                        pl = "Ostatnia udana kopia: ${formatMoment(it)}",
                        uk = "Остання успішна копія: ${formatMoment(it)}",
                        es = "Última copia correcta: ${formatMoment(it)}",
                        de = "Letzte erfolgreiche Sicherung: ${formatMoment(it)}",
                    )
                } ?: tr(
                    "Aucune sauvegarde réussie pour l'instant.",
                    "No successful backup yet.",
                    pl = "Na razie brak udanej kopii.",
                    uk = "Успішних копій поки немає.",
                    es = "Todavía no hay ninguna copia correcta.",
                    de = "Noch keine erfolgreiche Sicherung.",
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            current.folderName?.let { name ->
                Text(
                    tr("Dossier : $name", "Folder: $name", pl = "Folder: $name", uk = "Тека: $name", es = "Carpeta: $name", de = "Ordner: $name"),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            current.problem?.let { problem ->
                Text(backupProblemMessage(problem), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Muted(
                tr(
                    "Une copie est écrite au plus une fois par jour, seulement si vos données ont changé. Les 3 dernières sont conservées.",
                    "A copy is written at most once a day, only if your data changed. The last 3 are kept.",
                    pl = "Kopia jest zapisywana najwyżej raz dziennie i tylko wtedy, gdy dane się zmieniły. Zachowywane są 3 ostatnie.",
                    uk = "Копію записують щонайбільше раз на день і лише якщо дані змінилися. Зберігаються 3 останні.",
                    es = "Se escribe una copia como máximo una vez al día, solo si tus datos han cambiado. Se conservan las 3 últimas.",
                    de = "Höchstens einmal am Tag wird eine Kopie geschrieben, nur wenn sich deine Daten geändert haben. Die letzten 3 bleiben erhalten.",
                ),
            )
            if (working) {
                Muted(tr("Sauvegarde en cours…", "Backing up…", pl = "Trwa zapisywanie kopii…", uk = "Триває копіювання…", es = "Copiando…", de = "Sicherung läuft …"))
            }
            LunaButton(
                tr("Sauvegarder maintenant", "Back up now", pl = "Zapisz kopię teraz", uk = "Зберегти копію зараз", es = "Copiar ahora", de = "Jetzt sichern"),
                onClick = {
                    working = true
                    message = null
                    scope.launch { finish(AutoBackup.execute(context, force = true)) }
                },
                style = LunaButtonStyle.Outlined,
                enabled = !working,
            )
            LunaButton(
                tr("Changer de phrase", "Change phrase", pl = "Zmień frazę", uk = "Змінити фразу", es = "Cambiar la frase", de = "Phrase ändern"),
                onClick = {
                    message = null
                    dialog = BackupDialog.CHANGE_PHRASE
                },
                style = LunaButtonStyle.Outlined,
                enabled = !working,
            )
            LunaButton(
                tr("Changer de dossier", "Change folder", pl = "Zmień folder", uk = "Змінити теку", es = "Cambiar la carpeta", de = "Ordner ändern"),
                onClick = {
                    purpose = FolderPurpose.CHANGE
                    folderPicker.launch(null)
                },
                style = LunaButtonStyle.Outlined,
                enabled = !working,
            )
            LunaButton(
                tr("Désactiver", "Turn off", pl = "Wyłącz", uk = "Вимкнути", es = "Desactivar", de = "Deaktivieren"),
                onClick = {
                    message = null
                    dialog = BackupDialog.DISABLE
                },
                style = LunaButtonStyle.Quiet,
                enabled = !working,
            )
            CloudCopySetting(passphraseSet = true, refresh = refresh)
        }
    }
    message?.let { Muted(it) }

    val check = PassphraseRules.check(phrase, confirmation)
    when (dialog) {
        BackupDialog.SETUP, BackupDialog.CHANGE_PHRASE -> {
            val setup = dialog == BackupDialog.SETUP
            AlertDialog(
                onDismissRequest = { if (!working) closeDialog() },
                title = {
                    Text(
                        if (setup) {
                            tr("Sauvegarde automatique", "Automatic backup", pl = "Automatyczna kopia zapasowa", uk = "Автоматична резервна копія", es = "Copia de seguridad automática", de = "Automatische Sicherung")
                        } else {
                            tr("Changer de phrase", "Change phrase", pl = "Zmień frazę", uk = "Змінити фразу", es = "Cambiar la frase", de = "Phrase ändern")
                        },
                    )
                },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (setup) {
                            LunaButton(
                                tr("Choisir un dossier", "Choose a folder", pl = "Wybierz folder", uk = "Вибрати теку", es = "Elegir una carpeta", de = "Ordner wählen"),
                                onClick = {
                                    purpose = FolderPurpose.SETUP
                                    folderPicker.launch(null)
                                },
                                style = LunaButtonStyle.Outlined,
                            )
                            val folderLabel = pendingFolderName
                            Text(
                                when {
                                    pendingFolder == null -> tr("Aucun dossier choisi.", "No folder chosen.", pl = "Nie wybrano folderu.", uk = "Теку не вибрано.", es = "Ninguna carpeta elegida.", de = "Kein Ordner gewählt.")
                                    folderLabel != null -> tr("Dossier : $folderLabel", "Folder: $folderLabel", pl = "Folder: $folderLabel", uk = "Тека: $folderLabel", es = "Carpeta: $folderLabel", de = "Ordner: $folderLabel")
                                    else -> tr("Dossier choisi.", "Folder chosen.", pl = "Folder wybrany.", uk = "Теку вибрано.", es = "Carpeta elegida.", de = "Ordner gewählt.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            tr(
                                "Si vous oubliez cette phrase, personne ne pourra rouvrir la sauvegarde : ni vous, ni Pilmil Studio, ni personne d'autre. Il n'existe aucun moyen de la récupérer. Pensez à la noter sur papier, dans un endroit sûr.",
                                "If you forget this phrase, nobody can open the backup again: not you, not Pilmil Studio, not anyone else. There is no way to recover it. Consider writing it down on paper and keeping it somewhere safe.",
                                pl = "Jeśli zapomnisz tej frazy, nikt nie otworzy kopii: ani Ty, ani Pilmil Studio, ani nikt inny. Nie da się jej odzyskać. Rozważ zapisanie jej na papierze i schowanie w bezpiecznym miejscu.",
                                uk = "Якщо ви забудете цю фразу, ніхто не зможе відкрити копію: ні ви, ні Pilmil Studio, ні хтось інший. Відновити її неможливо. Подумайте про те, щоб записати її на папері й зберігати в безпечному місці.",
                                es = "Si olvidas esta frase, nadie podrá volver a abrir la copia: ni tú, ni Pilmil Studio, ni nadie más. No hay forma de recuperarla. Piensa en anotarla en papel y guardarla en un lugar seguro.",
                                de = "Wenn du diese Phrase vergisst, kann niemand die Sicherung wieder öffnen: weder du noch Pilmil Studio noch sonst jemand. Sie lässt sich nicht wiederherstellen. Schreib sie am besten auf Papier auf und bewahre sie an einem sicheren Ort auf.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!setup) {
                            Text(
                                tr(
                                    "Les sauvegardes déjà écrites restent protégées par l'ancienne phrase.",
                                    "Backups already written stay protected by the old phrase.",
                                    pl = "Już zapisane kopie pozostają chronione starą frazą.",
                                    uk = "Уже записані копії залишаються захищеними старою фразою.",
                                    es = "Las copias ya escritas siguen protegidas por la frase anterior.",
                                    de = "Bereits geschriebene Sicherungen bleiben durch die alte Phrase geschützt.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedTextField(
                            value = phrase,
                            onValueChange = { phrase = it },
                            label = { Text(tr("Phrase secrète", "Secret phrase", pl = "Tajna fraza", uk = "Таємна фраза", es = "Frase secreta", de = "Geheime Phrase")) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        if (phrase.isNotEmpty()) {
                            Text(
                                when (check.strength) {
                                    PassphraseStrength.TOO_SHORT -> tr(
                                        "Trop courte (12 caractères minimum)",
                                        "Too short (12 characters minimum)",
                                        pl = "Za krótka (minimum 12 znaków)",
                                        uk = "Закоротка (щонайменше 12 символів)",
                                        es = "Demasiado corta (mínimo 12 caracteres)",
                                        de = "Zu kurz (mindestens 12 Zeichen)",
                                    )
                                    PassphraseStrength.FAIR -> tr("Force : correcte", "Strength: fair", pl = "Siła: wystarczająca", uk = "Міцність: достатня", es = "Fuerza: aceptable", de = "Stärke: ausreichend")
                                    PassphraseStrength.STRONG -> tr("Force : solide", "Strength: strong", pl = "Siła: mocna", uk = "Міцність: міцна", es = "Fuerza: sólida", de = "Stärke: stark")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (check.strength == PassphraseStrength.TOO_SHORT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = { confirmation = it },
                            label = { Text(tr("Confirmer la phrase", "Confirm the phrase", pl = "Potwierdź frazę", uk = "Підтвердьте фразу", es = "Confirmar la frase", de = "Phrase bestätigen")) },
                            singleLine = true,
                            isError = check.mismatch,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        if (check.mismatch) {
                            Text(
                                tr(
                                    "Les deux saisies ne correspondent pas.",
                                    "The two entries do not match.",
                                    pl = "Oba wpisy nie są takie same.",
                                    uk = "Дві введені фрази не збігаються.",
                                    es = "Las dos entradas no coinciden.",
                                    de = "Die beiden Eingaben stimmen nicht überein.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = understood, onCheckedChange = { understood = it })
                            Text(
                                tr(
                                    "J'ai compris : sans cette phrase, la sauvegarde est inutilisable",
                                    "I understand: without this phrase, the backup cannot be opened",
                                    pl = "Rozumiem: bez tej frazy kopii nie da się otworzyć",
                                    uk = "Розумію: без цієї фрази копію не відкрити",
                                    es = "Entiendo: sin esta frase, la copia no se puede abrir",
                                    de = "Verstanden: ohne diese Phrase lässt sich die Sicherung nicht öffnen",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = check.valid && understood && !working && (!setup || pendingFolder != null),
                        onClick = {
                            val secret = phrase.toCharArray()
                            val folder = pendingFolder
                            working = true
                            message = null
                            scope.launch {
                                val result = try {
                                    if (setup && folder != null) {
                                        AutoBackup.configure(context, folder, secret)
                                    } else {
                                        AutoBackup.changePassphrase(context, secret)
                                    }
                                } finally {
                                    secret.fill('\u0000')
                                }
                                closeDialog()
                                finish(result, justConfigured = setup)
                            }
                        },
                    ) {
                        Text(
                            if (setup) {
                                tr("Activer", "Turn on", pl = "Włącz", uk = "Увімкнути", es = "Activar", de = "Aktivieren")
                            } else {
                                tr("Enregistrer", "Save", pl = "Zapisz", uk = "Зберегти", es = "Guardar", de = "Speichern")
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(enabled = !working, onClick = { closeDialog() }) {
                        Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen"))
                    }
                },
            )
        }
        BackupDialog.DISABLE -> AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(tr("Désactiver", "Turn off", pl = "Wyłącz", uk = "Вимкнути", es = "Desactivar", de = "Deaktivieren")) },
            text = {
                Text(
                    tr(
                        "La sauvegarde automatique s'arrête : l'appli oublie la phrase et supprime sa copie interne. Les fichiers déjà écrits dans votre dossier y restent : supprimez-les vous-même si vous le souhaitez.",
                        "Automatic backup stops: the app forgets the phrase and deletes its internal copy. Files already written in your folder stay there: delete them yourself if you wish.",
                        pl = "Automatyczna kopia przestaje działać: aplikacja zapomina frazę i usuwa swoją wewnętrzną kopię. Pliki już zapisane w folderze pozostają: usuń je samodzielnie, jeśli chcesz.",
                        uk = "Автоматична копія зупиняється: застосунок забуває фразу й видаляє свою внутрішню копію. Файли, уже записані у вашій теці, залишаються: за бажанням видаліть їх самі.",
                        es = "La copia automática se detiene: la app olvida la frase y borra su copia interna. Los archivos ya escritos en tu carpeta se quedan ahí: bórralos tú si quieres.",
                        de = "Die automatische Sicherung stoppt: Die App vergisst die Phrase und löscht ihre interne Kopie. Bereits in deinen Ordner geschriebene Dateien bleiben dort: lösche sie bei Bedarf selbst.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    closeDialog()
                    scope.launch {
                        AutoBackup.disable(context)
                        message = tr("Sauvegarde automatique désactivée.", "Automatic backup turned off.", pl = "Automatyczna kopia wyłączona.", uk = "Автоматичну копію вимкнено.", es = "Copia automática desactivada.", de = "Automatische Sicherung deaktiviert.")
                        refresh++
                    }
                }) { Text(tr("Désactiver", "Turn off", pl = "Wyłącz", uk = "Вимкнути", es = "Desactivar", de = "Deaktivieren")) }
            },
            dismissButton = {
                TextButton(onClick = { closeDialog() }) { Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen")) }
            },
        )
        null -> Unit
    }
}

private fun deviceHasScreenLock(context: Context): Boolean = runCatching {
    (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true
}.getOrDefault(false)

/**
 * Copie dans le compte Google, sur demande : seul le dernier fichier .pilmil chiffré par la phrase secrète est
 * concerné (jamais la base ni les clés). Activable seulement si une phrase est définie et si le téléphone a un
 * verrouillage d'écran ; masquée avant Android 12.
 */
@Composable
private fun CloudCopySetting(passphraseSet: Boolean, refresh: Int) {
    val context = LocalContext.current
    if (!BackupPolicy.settingAvailable(Build.VERSION.SDK_INT)) return
    var enabled by remember(refresh) { mutableStateOf(isCloudBackupEnabled(context)) }
    val blocker = BackupPolicy.blocker(Build.VERSION.SDK_INT, deviceHasScreenLock(context), passphraseSet)
    if (blocker == CloudCopyBlocker.NO_PASSPHRASE) {
        Muted(
            tr(
                "La copie dans votre compte Google est disponible une fois la sauvegarde automatique configurée : elle emporte le fichier chiffré par votre phrase secrète.",
                "Copying to your Google account is available once automatic backup is set up: the copy carries the file encrypted with your secret phrase.",
                pl = "Kopia na koncie Google jest dostępna po skonfigurowaniu automatycznej kopii: obejmuje plik zaszyfrowany Twoją tajną frazą.",
                uk = "Копіювання в акаунт Google доступне після налаштування автоматичної копії: копіюється файл, зашифрований вашою таємною фразою.",
                es = "La copia en tu cuenta de Google está disponible cuando la copia automática esté configurada: lleva el archivo cifrado con tu frase secreta.",
                de = "Die Kopie in dein Google-Konto ist verfügbar, sobald die automatische Sicherung eingerichtet ist: Sie enthält die mit deiner geheimen Phrase verschlüsselte Datei.",
            ),
        )
        return
    }
    Text(
        tr(
            "Copie dans votre compte Google : désactivée par défaut. Si vous l'activez, Android copie dans votre compte Google uniquement le dernier fichier de sauvegarde, déjà chiffré par votre phrase secrète : jamais la base ni les clés. Android chiffre en plus cette copie avec le code de verrouillage de votre téléphone. Sur un nouveau téléphone, Lunaison retrouve ce fichier, mais il faudra votre phrase secrète pour le rouvrir. Désactiver le réglage n'efface pas une copie déjà envoyée.",
            "Copy to your Google account: off by default. If you turn it on, Android copies to your Google account only the latest backup file, already encrypted with your secret phrase: never the database or the keys. Android also encrypts this copy with your phone's lock code. On a new phone, Lunaison finds this file, but your secret phrase is needed to open it. Turning the setting off does not delete a copy already sent.",
            pl = "Kopia na koncie Google: domyślnie wyłączona. Po włączeniu Android kopiuje na Twoje konto Google tylko ostatni plik kopii, już zaszyfrowany Twoją tajną frazą: nigdy bazy ani kluczy. Android dodatkowo szyfruje tę kopię kodem blokady telefonu. Na nowym telefonie Lunaison znajdzie ten plik, ale do jego otwarcia potrzebna będzie Twoja tajna fraza. Wyłączenie ustawienia nie usuwa kopii, która została już wysłana.",
            uk = "Копія в акаунті Google: вимкнено за замовчуванням. Якщо ввімкнути, Android копіює у ваш акаунт Google лише останній файл копії, уже зашифрований вашою таємною фразою: ніколи не базу й не ключі. Android додатково шифрує цю копію кодом блокування телефону. На новому телефоні Lunaison знайде цей файл, але щоб його відкрити, знадобиться ваша таємна фраза. Вимкнення налаштування не видаляє копію, яку вже надіслано.",
            es = "Copia en tu cuenta de Google: desactivada por defecto. Si la activas, Android copia en tu cuenta de Google solo el último archivo de copia, ya cifrado con tu frase secreta: nunca la base de datos ni las claves. Android cifra además esta copia con el código de bloqueo de tu teléfono. En un teléfono nuevo, Lunaison encuentra este archivo, pero hará falta tu frase secreta para abrirlo. Desactivar el ajuste no borra una copia ya enviada.",
            de = "Kopie in dein Google-Konto: standardmäßig aus. Wenn du sie einschaltest, kopiert Android nur die letzte Sicherungsdatei in dein Google-Konto, bereits mit deiner geheimen Phrase verschlüsselt: nie die Datenbank und nie die Schlüssel. Android verschlüsselt diese Kopie zusätzlich mit dem Sperrcode deines Handys. Auf einem neuen Handy findet Lunaison diese Datei, aber zum Öffnen braucht es deine geheime Phrase. Das Ausschalten löscht keine bereits gesendete Kopie.",
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (blocker == CloudCopyBlocker.NO_SCREEN_LOCK) {
        Muted(
            tr(
                "Indisponible tant que votre téléphone n'a pas de verrouillage d'écran : sans lui, Android ne chiffre pas la copie.",
                "Unavailable until your phone has a screen lock: without it, Android does not encrypt the copy.",
                pl = "Niedostępne, dopóki telefon nie ma blokady ekranu: bez niej Android nie szyfruje kopii.",
                uk = "Недоступно, доки на телефоні немає блокування екрана: без нього Android не шифрує копію.",
                es = "No disponible hasta que tu teléfono tenga bloqueo de pantalla: sin él, Android no cifra la copia.",
                de = "Nicht verfügbar, solange dein Handy keine Displaysperre hat: Ohne sie verschlüsselt Android die Kopie nicht.",
            ),
        )
    }
    val usable = blocker == null
    val label = tr(
        "Copier dans mon compte Google",
        "Copy to my Google account",
        pl = "Kopiuj na moje konto Google",
        uk = "Копіювати в мій акаунт Google",
        es = "Copiar en mi cuenta de Google",
        de = "In mein Google-Konto kopieren",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = enabled && usable,
                enabled = usable,
                role = Role.Switch,
                onValueChange = {
                    enabled = it
                    setCloudBackupEnabled(context, it)
                },
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = enabled && usable, onCheckedChange = null, enabled = usable)
    }
}
