package fr.lunaison.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.lunaison.core.data.AppSettings
import fr.lunaison.core.data.PinThrottle
import fr.lunaison.core.model.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Écran de déverrouillage par code PIN à 4 chiffres au lancement de l'application,
 * lorsque la personne a activé le verrouillage dans les réglages.
 *
 * [decoyHash] : code leurre facultatif. Saisi à la place du code, il appelle [onDecoy] sans rien laisser
 * paraître (même animation, même délai). [biometric] : proposé d'emblée s'il est activé ; il ouvre
 * toujours la vraie base.
 */
@Composable
fun PinUnlockScreen(
    expectedHash: String,
    onUnlocked: () -> Unit,
    decoyHash: String? = null,
    onDecoy: () -> Unit = {},
    biometric: BiometricUnlock? = null,
) {
    val throttleStore = rememberPinThrottleStore()
    val scope = rememberCoroutineScope()
    var throttleState by remember {
        mutableStateOf(PinThrottle.normalize(throttleStore.load(), currentElapsedRealtimeMs()))
    }
    var nowMs by remember { mutableStateOf(currentElapsedRealtimeMs()) }
    var entered by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = throttleStore.load()
        val normalized = PinThrottle.normalize(loaded, currentElapsedRealtimeMs())
        throttleState = normalized
        if (normalized != loaded) throttleStore.save(normalized)
    }

    val remainingMs = PinThrottle.remainingMs(throttleState, nowMs)
    val isBlocked = remainingMs > 0L

    fun unlockedByBiometric() {
        val reset = PinThrottle.onSuccess()
        throttleState = reset
        throttleStore.save(reset)
        onUnlocked()
    }

    fun askBiometric() {
        biometric?.authenticate(DiscretionTexts.biometricPromptTitle, DiscretionTexts.useCode) { unlockedByBiometric() }
    }

    // La demande du système s'affiche d'elle-même à chaque ouverture ; « Utiliser le code » la referme.
    LaunchedEffect(biometric) { if (biometric != null) askBiometric() }

    LaunchedEffect(throttleState, isBlocked) {
        if (!isBlocked) return@LaunchedEffect
        while (PinThrottle.isBlocked(throttleState, currentElapsedRealtimeMs())) {
            delay(500L)
            nowMs = currentElapsedRealtimeMs()
        }
        nowMs = currentElapsedRealtimeMs()
    }

    fun appendDigit(digit: Char) {
        val currentNow = currentElapsedRealtimeMs()
        if (verifying || PinThrottle.isBlocked(throttleState, currentNow) || entered.length >= 4) return
        val next = entered + digit
        error = false
        if (next.length == 4) {
            entered = next
            verifying = true
            scope.launch {
                // Le code leurre est toujours vérifié aussi, pour que les deux chemins prennent le même temps.
                val (ok, decoy) = withContext(Dispatchers.Default) {
                    val real = AppSettings.verifyPin(next, expectedHash)
                    val leurre = decoyHash?.let { AppSettings.verifyPin(next, it) } ?: false
                    real to (!real && leurre)
                }
                verifying = false
                if (ok || decoy) {
                    val reset = PinThrottle.onSuccess()
                    throttleState = reset
                    throttleStore.save(reset)
                    if (ok) onUnlocked() else onDecoy()
                } else {
                    val failNow = currentElapsedRealtimeMs()
                    val updated = PinThrottle.onFailure(throttleState, failNow)
                    throttleState = updated
                    throttleStore.save(updated)
                    nowMs = failNow
                    entered = ""
                    error = true
                }
            }
        } else {
            entered = next
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Lunaison", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            val statusMessage = when {
                isBlocked -> {
                    val remainingSec = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
                    tr(
                        "Trop d'essais. Réessayez dans ${remainingSec} s",
                        "Too many attempts. Try again in ${remainingSec}s",
                        pl = "Zbyt wiele prób. Spróbuj ponownie za ${remainingSec} s",
                        uk = "Забагато спроб. Спробуйте знову через ${remainingSec} с",
                        es = "Demasiados intentos. Inténtalo de nuevo en ${remainingSec} s",
                        de = "Zu viele Versuche. Erneut versuchen in ${remainingSec} s",
                    )
                }
                error -> tr("Code incorrect, réessayez", "Incorrect code, please try again", pl = "Nieprawidłowy kod, spróbuj ponownie", uk = "Неправильний код, спробуйте ще раз", es = "Código incorrecto, inténtalo de nuevo", de = "Falscher Code, bitte erneut versuchen")
                else -> tr("Entrez votre code à 4 chiffres", "Enter your 4-digit code", pl = "Wpisz swój 4-cyfrowy kod", uk = "Введіть свій 4-значний код", es = "Introduce tu código de 4 dígitos", de = "Gib deinen 4-stelligen Code ein")
            }
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                // Zone live : « Code incorrect » est annoncé sans avoir à explorer l'écran.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = if (isBlocked || error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.semantics {
                    contentDescription = tr("${entered.length} chiffres saisis sur 4", "${entered.length} of 4 digits entered", pl = "Wpisane cyfry: ${entered.length} z 4", uk = "Введено цифр: ${entered.length} з 4", es = "${entered.length} de 4 dígitos introducidos", de = "${entered.length} von 4 Ziffern eingegeben")
                },
            ) {
                repeat(4) { index ->
                    val filled = index < entered.length
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
            )
            val inputEnabled = !isBlocked && !verifying
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { digit ->
                            PinKey(label = digit.toString(), enabled = inputEnabled, onClick = { appendDigit(digit) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(72.dp))
                    PinKey(label = "0", enabled = inputEnabled, onClick = { appendDigit('0') })
                    TextButton(
                        onClick = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                        enabled = inputEnabled,
                        modifier = Modifier.size(72.dp),
                    ) {
                        val eraseLabel = tr("Effacer le dernier chiffre", "Delete the last digit", pl = "Usuń ostatnią cyfrę", uk = "Стерти останню цифру", es = "Borrar el último dígito", de = "Letzte Ziffer löschen")
                        Text("⌫", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = eraseLabel })
                    }
                }
            }
            if (biometric != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { askBiometric() }, enabled = !isBlocked) {
                    Text(DiscretionTexts.useBiometric)
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(72.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun PinSetupDialog(
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Choisir un code à 4 chiffres", "Choose a 4-digit PIN", pl = "Wybierz 4-cyfrowy kod", uk = "Оберіть 4-значний код", es = "Elige un código de 4 dígitos", de = "4-stelligen Code wählen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tr("Ce code sera demandé à l'ouverture de Lunaison et masquera l'aperçu dans les applications récentes. Attention : comme tout reste sur votre téléphone, il n'existe pas de récupération à distance.", "This code will be requested when opening Lunaison and will hide the preview in recent apps. Note: since everything stays on your phone, there is no remote recovery.", pl = "Ten kod będzie wymagany przy otwieraniu Lunaison i ukryje podgląd w ostatnio używanych aplikacjach. Uwaga: ponieważ wszystko pozostaje na Twoim telefonie, nie ma zdalnego odzyskiwania.", uk = "Цей код вимагатиметься під час відкриття Lunaison і приховуватиме попередній перегляд у нещодавніх застосунках. Увага: оскільки все залишається на вашому телефоні, віддаленого відновлення не існує.", es = "Este código se pedirá al abrir Lunaison y ocultará la vista previa en las aplicaciones recientes. Atención: como todo se queda en tu teléfono, no existe recuperación a distancia.", de = "Dieser Code wird beim Öffnen von Lunaison abgefragt und blendet die Vorschau in den zuletzt verwendeten Apps aus. Achtung: Da alles auf deinem Telefon bleibt, gibt es keine Wiederherstellung aus der Ferne."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> pin = value.filter { it.isDigit() }.take(4) },
                    label = { Text(tr("Code (4 chiffres)", "PIN (4 digits)", pl = "Kod (4 cyfry)", uk = "Код (4 цифри)", es = "Código (4 dígitos)", de = "Code (4 Ziffern)")) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (pin.length == 4) onSavePin(pin) },
                enabled = pin.length == 4,
            ) {
                Text(tr("Activer", "Enable", pl = "Włącz", uk = "Увімкнути", es = "Activar", de = "Aktivieren"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen"))
            }
        },
    )
}

/**
 * Choix du code leurre. Il doit différer du vrai code : la vérification passe par l'empreinte enregistrée,
 * le vrai code n'est jamais demandé ni gardé en clair.
 */
@Composable
fun DecoySetupDialog(
    realPinHash: String,
    onSaveDecoy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var sameAsReal by rememberSaveable { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr6("Choisir le code leurre", "Choose the decoy code", "Wybierz kod-przynętę", "Оберіть код-приманку", "Elige el código señuelo", "Ablenkungscode wählen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tr6(
                        "Saisi à la place de votre code, il ouvre une Lunaison vide. Ce que vous y notez reste séparé de vos vraies données. Choisissez un code différent du vôtre.",
                        "Entered instead of your code, it opens an empty Lunaison. What you log there stays separate from your real data. Choose a code different from yours.",
                        "Wpisany zamiast twojego kodu otwiera pustą Lunaison. To, co w niej zapiszesz, pozostaje oddzielone od twoich prawdziwych danych. Wybierz kod inny niż twój.",
                        "Якщо ввести його замість вашого коду, відкриється порожня Lunaison. Те, що ви там записуєте, зберігається окремо від ваших справжніх даних. Оберіть код, відмінний від вашого.",
                        "Si lo introduces en lugar de tu código, abre una Lunaison vacía. Lo que anotes allí queda separado de tus datos reales. Elige un código distinto del tuyo.",
                        "Statt deines Codes eingegeben, öffnet er eine leere Lunaison. Was du dort einträgst, bleibt von deinen echten Daten getrennt. Wähle einen anderen Code als deinen.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it.isDigit() }.take(4)
                        sameAsReal = false
                    },
                    label = { Text(tr("Code (4 chiffres)", "PIN (4 digits)", pl = "Kod (4 cyfry)", uk = "Код (4 цифри)", es = "Código (4 dígitos)", de = "Code (4 Ziffern)")) },
                    singleLine = true,
                    isError = sameAsReal,
                    supportingText = if (sameAsReal) {
                        {
                            Text(
                                tr6(
                                    "C'est déjà le code de vos vraies données. Choisissez-en un autre.",
                                    "This is already the code for your real data. Choose another one.",
                                    "To już jest kod do twoich prawdziwych danych. Wybierz inny.",
                                    "Це вже код до ваших справжніх даних. Оберіть інший.",
                                    "Ya es el código de tus datos reales. Elige otro.",
                                    "Das ist bereits der Code für deine echten Daten. Wähle einen anderen.",
                                ),
                            )
                        }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.length != 4 || checking) return@TextButton
                    checking = true
                    scope.launch {
                        val same = withContext(Dispatchers.Default) { AppSettings.verifyPin(pin, realPinHash) }
                        checking = false
                        if (same) sameAsReal = true else onSaveDecoy(pin)
                    }
                },
                enabled = pin.length == 4 && !checking,
            ) {
                Text(tr("Activer", "Enable", pl = "Włącz", uk = "Увімкнути", es = "Activar", de = "Aktivieren"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Annuler", "Cancel", pl = "Anuluj", uk = "Скасувати", es = "Cancelar", de = "Abbrechen"))
            }
        },
    )
}
