package fr.lunaison.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.lunaison.core.designsystem.LunaCard
import fr.lunaison.core.model.tr
import fr.lunaison.ui.autoBackupAvailable
import fr.lunaison.ui.hideBackupCardUntil
import fr.lunaison.ui.readAutoBackupStatus
import fr.lunaison.ui.rememberPlatformContext
import kotlinx.datetime.LocalDate

/** Phrase qui dit pourquoi la sauvegarde automatique n'a pas pu se faire. Elle sert aux réglages et à l'accueil. */
fun backupProblemMessage(problem: BackupProblem): String = when (problem) {
    BackupProblem.FOLDER_UNAVAILABLE -> tr(
        "Le dossier n'est plus accessible (permission retirée, stockage absent ou dossier supprimé). Choisissez-le de nouveau.",
        "The folder is no longer accessible (permission removed, storage missing or folder deleted). Choose it again.",
        pl = "Folder nie jest już dostępny (cofnięte uprawnienie, brak nośnika albo usunięty folder). Wybierz go ponownie.",
        uk = "Тека більше недоступна (дозвіл скасовано, сховища немає або теку видалено). Виберіть її знову.",
        es = "La carpeta ya no está accesible (permiso retirado, almacenamiento ausente o carpeta eliminada). Elígela de nuevo.",
        de = "Der Ordner ist nicht mehr erreichbar (Berechtigung entzogen, Speicher fehlt oder Ordner gelöscht). Wähle ihn erneut.",
    )
    BackupProblem.PHRASE_UNREADABLE -> tr(
        "La phrase secrète n'est plus lisible sur ce téléphone. Définissez-la de nouveau avec « Changer de phrase ».",
        "The secret phrase can no longer be read on this phone. Set it again with \"Change phrase\".",
        pl = "Tajnej frazy nie można już odczytać na tym telefonie. Ustaw ją ponownie przez „Zmień frazę”.",
        uk = "Таємну фразу більше не можна прочитати на цьому телефоні. Задайте її знову через «Змінити фразу».",
        es = "La frase secreta ya no se puede leer en este teléfono. Defínela de nuevo con «Cambiar la frase».",
        de = "Die geheime Phrase ist auf diesem Handy nicht mehr lesbar. Lege sie mit „Phrase ändern“ neu fest.",
    )
    BackupProblem.WRITE_FAILED -> tr(
        "Écriture impossible dans le dossier (stockage plein ?). Les sauvegardes précédentes sont intactes.",
        "Could not write to the folder (storage full?). Earlier backups are untouched.",
        pl = "Nie można zapisać w folderze (pełna pamięć?). Poprzednie kopie są nienaruszone.",
        uk = "Не вдалося записати в теку (сховище заповнене?). Попередні копії не зачеплено.",
        es = "No se pudo escribir en la carpeta (¿almacenamiento lleno?). Las copias anteriores siguen intactas.",
        de = "Schreiben in den Ordner nicht möglich (Speicher voll?). Frühere Sicherungen sind unverändert.",
    )
    BackupProblem.VERIFY_FAILED -> tr(
        "Le fichier écrit ne se relit pas : il a été retiré et les sauvegardes précédentes sont intactes.",
        "The written file could not be read back: it was removed and earlier backups are untouched.",
        pl = "Zapisanego pliku nie udało się odczytać: usunięto go, a poprzednie kopie są nienaruszone.",
        uk = "Записаний файл не вдалося прочитати: його видалено, а попередні копії не зачеплено.",
        es = "El archivo escrito no se puede releer: se ha retirado y las copias anteriores siguen intactas.",
        de = "Die geschriebene Datei ließ sich nicht zurücklesen: Sie wurde entfernt, frühere Sicherungen sind unverändert.",
    )
    BackupProblem.PRIVATE_COPY_FAILED -> tr(
        "La sauvegarde est faite, mais la copie interne à l'appli n'a pas pu être écrite.",
        "The backup is done, but the app's internal copy could not be written.",
        pl = "Kopia jest zapisana, ale wewnętrznej kopii aplikacji nie udało się zapisać.",
        uk = "Копію збережено, але внутрішню копію застосунку записати не вдалося.",
        es = "La copia está hecha, pero no se pudo escribir la copia interna de la app.",
        de = "Die Sicherung ist gemacht, aber die interne Kopie der App konnte nicht geschrieben werden.",
    )
}

/**
 * Accueil : signale un problème de sauvegarde automatique, et rappelle sans insister que les données ne sont que
 * sur ce téléphone tant qu'aucune sauvegarde n'est configurée. Pas de notification.
 */
@Composable
fun BackupHomeCards(loggedDays: Int, today: LocalDate, onOpenSettings: () -> Unit) {
    if (!autoBackupAvailable) return
    val context = rememberPlatformContext()
    val status by produceState<AutoBackupStatus>(AutoBackupStatus.Unavailable, context) { value = readAutoBackupStatus(context) }
    var snoozed by rememberSaveable { mutableStateOf(false) }

    BackupReminder.problemToShow(status)?.let { problem ->
        LunaCard(container = MaterialTheme.colorScheme.errorContainer) {
            Text(
                tr(
                    "La sauvegarde automatique a un problème",
                    "Automatic backup has a problem",
                    pl = "Automatyczna kopia ma problem",
                    uk = "Автоматична копія має проблему",
                    es = "La copia automática tiene un problema",
                    de = "Die automatische Sicherung hat ein Problem",
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(backupProblemMessage(problem), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings) {
                    Text(
                        tr("Ouvrir les réglages", "Open settings", pl = "Otwórz ustawienia", uk = "Відкрити налаштування", es = "Abrir los ajustes", de = "Einstellungen öffnen"),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }

    if (!snoozed && BackupReminder.shouldShowReminder(status, loggedDays, today)) {
        LunaCard {
            Text(
                tr(
                    "Vos données n'existent que sur ce téléphone",
                    "Your data only exists on this phone",
                    pl = "Twoje dane istnieją tylko na tym telefonie",
                    uk = "Ваші дані існують лише на цьому телефоні",
                    es = "Tus datos solo existen en este teléfono",
                    de = "Deine Daten gibt es nur auf diesem Handy",
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                tr(
                    "Si vous perdez ou changez de téléphone, votre historique est perdu, sauf si vous avez exporté un fichier. Une sauvegarde automatique chiffrée, dans un dossier de votre choix, règle cela sans compte ni Internet.",
                    "If you lose or change your phone, your history is lost unless you exported a file. An encrypted automatic backup in a folder of your choice fixes this, with no account and no Internet.",
                    pl = "Jeśli zgubisz telefon albo go zmienisz, historia przepadnie, chyba że wyeksportujesz plik. Zaszyfrowana automatyczna kopia w wybranym folderze rozwiązuje to bez konta i internetu.",
                    uk = "Якщо ви загубите телефон або заміните його, історію буде втрачено, якщо ви не експортували файл. Зашифрована автоматична копія у вибраній теці розв'язує це без облікового запису й інтернету.",
                    es = "Si pierdes o cambias de teléfono, tu historial se pierde, salvo que hayas exportado un archivo. Una copia automática cifrada en una carpeta que elijas lo resuelve, sin cuenta ni Internet.",
                    de = "Wenn du dein Handy verlierst oder wechselst, ist dein Verlauf weg, außer du hast eine Datei exportiert. Eine verschlüsselte automatische Sicherung in einem Ordner deiner Wahl löst das, ohne Konto und ohne Internet.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    hideBackupCardUntil(context, BackupReminder.hiddenUntilAfterLater(today))
                    snoozed = true
                }) {
                    Text(tr("Plus tard", "Later", pl = "Później", uk = "Пізніше", es = "Más tarde", de = "Später"))
                }
                TextButton(onClick = onOpenSettings) {
                    Text(
                        tr(
                            "Configurer une sauvegarde",
                            "Set up a backup",
                            pl = "Skonfiguruj kopię zapasową",
                            uk = "Налаштувати резервну копію",
                            es = "Configurar una copia",
                            de = "Sicherung einrichten",
                        ),
                    )
                }
            }
        }
    }
}
