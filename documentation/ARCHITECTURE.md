# Architecture & Choix Techniques — Lunaison

## 1. Structure du projet et modules

Lunaison repose sur une architecture **Kotlin Multiplatform (KMP)** modulaire avec build composite `pilmil-core` :
- `:core:model` (pur Kotlin `commonMain`) : entités du domaine (`DayLog`, `Flow`, `Pain`, `Symptom`, `Mood`, `PartnerSnapshot`, etc.) et traductions `tr()`.
- `:core:cycle` (pur Kotlin `commonMain`) : moteurs d'analyse (`CycleAnalyzer`, `AdaptiveCyclePredictor`, `PeriodDetector`, `SymptomCorrelationEngine`, `Alerts`, `ReminderPlanner`).
- `:core:content` (pur Kotlin `commonMain`) : contenus cliniques et éducatifs en 6 langues (`Articles`).
- `:core:data` (KMP) : Room (`LunaisonDatabase`, `LunaisonDao`, migrations 1 à 7), `LunaisonRepository`, `PeriodCalendarImporter`, `Sha256`.
- `:core:designsystem` : `LunaisonTheme`, typographie *Atkinson Hyperlegible*, palettes WCAG AA.
- `:ui` : écrans Compose Multiplatform 1.7.3.
- `:app` : point d'entrée Android (`MainActivity`), `WorkManager` (`ReminderScheduler`, `ReminderWorker`), `ReminderActionReceiver`, `LunaisonWidget`, `FileProvider`.
- `:screenshots` : tests de captures Paparazzi.

---

## 2. Manifeste, permissions et confidentialité

Dans `app/src/main/AndroidManifest.xml` :
- **Permissions déclarées** :
  - `POST_NOTIFICATIONS` : rappels locaux discrets.
  - `CAMERA` (`android:required="false"`) : scan du QR Code partenaire uniquement.
- **Aucune permission `INTERNET`** : aucune connexion réseau possible.
- **`android:allowBackup="false"`** par défaut et `data_extraction_rules.xml` bloquant toute sauvegarde cloud (le transfert local appareil à appareil n'emporte plus la base chiffrée ni sa clé, voir `chiffrement.md`).
- **`<queries>`** : `com.whatsapp`, `com.whatsapp.w4b`, `org.thoughtcrime.securesms` (Signal) et `smsto` pour le partage explicite vers messagerie.

---

## 3. Choix techniques vérifiés dans le code

### Base de données & sécurité du code PIN
- **Room 2.7.2** sur SQLite natif (`lunaison.db`), stocké dans le bac à sable privé de l'application (sans SQLCipher actuellement).
- **Code PIN** : haché en **SHA-256** (`Sha256.kt`) dans la table `settings`, avec temporisation anti-bruteforce (`PinThrottle.kt`).
- **Protection d'écran** : `FLAG_SECURE` activé dynamiquement dans `MainActivity` dès qu'un PIN est configuré ; widget et actions rapides de notification masqués tant que l'app est verrouillée.

### Import, export et partage P2P
- **Bilan soignant PDF** : généré localement via `android.graphics.pdf.PdfDocument` (`HealthReportPdf.kt`) et partagé via `FileProvider`.
- **Import *Calendrier des règles*** : déchiffreur **ZipCrypto** autonome en Kotlin pur (`PeriodCalendarImporter.kt`).
- **Synchronisation partenaire P2P** : jeton compact `LUNA1:<base64url>` avec contrôle d'intégrité **CRC32 (`v2`)**, généré en QR Code via **ZXing** et lu via **CameraX** sans serveur.
- **Coffre-fort `.pilmil`** : `AndroidBackupVault` (`pilmil-core`, AES-256-GCM + PBKDF2-HMAC-SHA256 600 000 itérations + GZIP) est branché dans `SettingsScreen` (export chiffré `lunaison.pilmil`) et `ImportDialogs` (déchiffrement et restauration locale).
