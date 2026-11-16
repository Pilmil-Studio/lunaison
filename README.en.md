# Lunaison

[Français](README.md) · **English**

[![CI](https://github.com/Pilmil-Studio/lunaison/actions/workflows/ci.yml/badge.svg)](https://github.com/Pilmil-Studio/lunaison/actions/workflows/ci.yml)

A free and open source menstrual cycle tracker, with no account. To log your periods, symptoms and mood, understand what is happening in each phase, know when your next period is coming, and let your partner know if you wish.

Lunaison is a free, open source (MPL-2.0) app for Android and iOS, with no ads, no trackers and **no Internet access**. Everything stays on the phone.

**[Lunaison website](https://pilmil-studio.github.io/lunaison/en/)** · **Google Play** (soon) · **App Store** (soon) · **[Download APK](https://github.com/Pilmil-Studio/lunaison/releases/latest)** · [For health professionals](https://pilmil-studio.github.io/lunaison/en/professionals.html) · [Medical evidence](https://pilmil-studio.github.io/lunaison/en/evidence.html) · [Privacy](https://pilmil-studio.github.io/lunaison/en/privacy.html)

**Before the Google Play release:** [join the internal test](https://play.google.com/apps/internaltest/4699280816823618019) (only for people already on the tester list: [how to become a tester](https://pilmil-studio.github.io/lunaison/en/tester.html)).

## What the app does

- **Log your days in a few seconds**: flow, pain (described by its effect), symptoms (8 common symptoms shown by default, plus clinical symptoms for pelvic pain, endometriosis and perimenopause), moods, contraception or medication taken, optional ovulation signs (cervical mucus and keypad-free basal temperature), and a personal note. **Recent 7-day strip on home screen** to quickly view recent logs and fill in a forgotten day in one tap.
- **Forecast periods and adapt to every life stage**: an estimated date and a plausible range calculated from your last six cycles, with an option to display the estimated fertile window, a **naturally irregular cycles mode** (perimenopause, PCOS, postpartum), and a toggle to pause predictions (pregnancy, breastfeeding, continuous contraception).
- **Understand the current phase and all four phases**: what is happening in the body, what helps, and what tends to come back for you across each of the four phases based on your previous cycles.
- **Generate a PDF or one-page clinician health report**: accessible in 1 tap from Calendar or Settings (`HealthReportPdf`). Median length and regularity of the last 6 cycles (FIGO/ACOG reference ranges), bleeding duration, moderate/severe pain days, spotting, **90-day pain and endometriosis indicators (the 5 "Ds")** and recurring symptoms, ready to show or share during a medical appointment.
- **One referenced tip a day and 1-minute guided breathing**: the daily tip changes according to the phase and responds first to what you logged; a guided abdominal breathing exercise (4 s / 6 s) helps ease cramps or premenstrual tension.
- **Spot what deserves medical advice**: repeated severe pain, periods longer than 8 days, repeated heavy periods (NHS indicators), cycles outside the usual range, bleeding between periods, missing periods, extended delay. Never a diagnosis.
- **Read referenced guides and anatomical diagrams**: the cycle, pain, food, premenstrual syndrome, when to see a doctor, contraception, **endometriosis, PCOS, adenomyosis, perimenopause**, limits of forecasts, a guide for partners and family, plus anatomical diagrams (reproductive system and vulva).
- **Serverless data migration & import**: direct import of backups from the popular *Period Calendar* app (password-protected ZIP or JSON) and Lunaison CSV files, parsed and decrypted locally on device without sending a single byte to any server.
- **Discreet reminders**: before your period, to log your day, or daily for contraception or medication, always neutral on the lock screen ("Lunaison").
- **Share with a partner, by message or offline QR Code** (18 and over): ready-to-send messages, concrete support tips for your partner, and serverless estimate sharing via **QR Code** (displayed and scanned right inside the app) or Android's native share menu (SMS, Signal, WhatsApp…).
- **Customization, PIN lock and home screen widget**: 4 color palettes (light and dark), optional 4-digit PIN lock (hiding the preview in recent apps), and a discreet home screen widget.

Lunaison is **neither a medical device nor a method of contraception**.

## Preview

<table>
  <tr>
    <td align="center"><img src="docs/captures/en/accueil.png" width="220" alt="Home: cycle phase, estimated next period, daily tip and Log today button"><br>Home</td>
    <td align="center"><img src="docs/captures/en/journee.png" width="220" alt="My day: chips for flow, pain, symptoms and mood"><br>My day</td>
    <td align="center"><img src="docs/captures/en/calendrier.png" width="220" alt="Monthly calendar of periods and logged days"><br>Calendar</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/en/comprendre.png" width="220" alt="Understand: cycle report, four phases and referenced medical guides"><br>Understand</td>
    <td align="center"><img src="docs/captures/en/fiche_douleurs.png" width="220" alt="Referenced guide on painful periods with evidence levels and citations"><br>Referenced guide</td>
    <td align="center"><img src="docs/captures/en/partage.png" width="220" alt="Partner sharing by message or offline QR Code"><br>Partner sharing</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/en/reglages.png" width="220" alt="Settings: theme, discreet reminders, cycle options and CSV export"><br>Settings</td>
    <td align="center"><img src="docs/captures/en/accueil_sombre.png" width="220" alt="Home in warm dark theme"><br>Dark theme</td>
    <td align="center"><img src="docs/captures/en/bienvenue.png" width="220" alt="Welcome screen with no account creation"><br>Welcome</td>
  </tr>
</table>

Screenshots are generated automatically from the screen code on every change.

## Design commitments

- **No Internet permission**: the app requests no network access from Android (the only permission is the camera, to scan a partner's QR Code). Your period dates, symptoms and notes stay on your phone.
- **Everything included upfront**: perimenopause mode, consultation summaries, ovulation signs and partner sharing are all available with no subscription.
- **A calm tone, with no red warnings**: an extended delay or unusual symptom is explained plainly with its medical references.
- **No daily streak counter**: skipping a few days is normal and does not stop the app from working.

## Why Lunaison is free

Lunaison grew out of my partner's need: to track her cycle more easily, and to let me support her day to day. Existing apps often keep their useful features behind a subscription, or fund themselves with their users' data, even though it is some of the most intimate data there is. Lunaison does the opposite: it is free, open source, works without the Internet and keeps everything on the phone.

The app sticks to what is necessary. What it does not say itself, where its reference ranges come from and why it is built this way, is explained in [What the studies say](https://pilmil-studio.github.io/lunaison/en/evidence.html).

## Privacy

- No Internet permission, no third-party SDK, no account.
- Cloud backup turned off by default (`pilmil-core`): health data never passes through any server. On Android the database is encrypted at rest (SQLCipher, key protected by the Keystore, see [documentation/chiffrement.md](documentation/chiffrement.md)) and is left out of device-to-device transfer. Available: CSV export and import (same header and values in every language), and, on Android only, a **full encrypted `.pilmil` backup** (AES-256-GCM, a password you choose: log and settings, without the PIN code) with a restore that replaces the phone's data, and **direct import of password-protected Period Calendar archives**, processed 100% locally.
- Optional 4-digit PIN lock with anti-bruteforce throttling (`PinThrottle`) and hidden preview in recent apps.
- The system contact picker gives access only to the chosen contact, with no permission on the address book.
- Complete erasure in one tap.

## Technical details

Kotlin Multiplatform, Compose Multiplatform, Material 3, Room KMP (`sqlite-bundled`), WorkManager (Android), iOS (`iosApp/`). Android `minSdk 26`, `targetSdk 36`.

The app is available in **6 languages** (`fr`, `en`, `pl`, `uk`, `es`, `de`), following the phone's language. UI strings go through `tr()` in `:core:model`, and the guides have one file per language in `:core:content`, checked by tests.

```
:app                 Android shell, navigation, WorkManager reminders, widget, pilmil-core integration
:ui                  Shared Compose Multiplatform UI (Android + iOS: Home, My day, Calendar, Understand, Share, Settings, PDF report)
:core:model          Logged day and supported languages (KMP commonMain)
:core:cycle          Engine: period detection, adaptive forecasting, symptom correlation, phases, alerts, reminders (KMP commonMain, tested)
:core:content        Guides (6 languages), phase advice, share messages, sources (KMP commonMain, tested)
:core:data           Local Room KMP database (Android + iOS), non-destructive migrations, PIN encryption + PinThrottle
:core:designsystem   Theme, 4 WCAG AA palettes, Atkinson Hyperlegible font, accessible components (KMP)
:screenshots         Screenshot tests
iosApp               iOS Xcode / SwiftUI shell
```

The engine's rules and their references are in [`CycleConfig.kt`](core/cycle/src/commonMain/kotlin/fr/lunaison/core/cycle/CycleConfig.kt). The sources for the content and the design choices are in [documentation/sources.md](documentation/sources.md), published on the website as [What the studies say](https://pilmil-studio.github.io/lunaison/en/evidence.html).

Tests: `./gradlew test`. Build: `./gradlew :app:assembleDebug`.

## From the same studio (Pilmil Studio)

- **[Sommelle](https://github.com/Pilmil-Studio/sommelle)**: 6-to-8-week CBT-I insomnia support app, free, open source and offline.
- **[Respire](https://github.com/Pilmil-Studio/respire)**: a short breathing pause before the apps you open on autopilot, with a Reels & Shorts filter.
- **[Sauve](https://github.com/Pilmil-Studio/sauve)**: anti-panic first aid guide, free, open source and offline, with hands-free voice and haptic guidance.
- **[Sillon](https://github.com/Pilmil-Studio/sillon)**: living vegetable garden planner and agronomic calendar based on the last frost date, free, open source and offline.
- **[Fredonne](https://github.com/Pilmil-Studio/fredonne)**: single-voice melody entry, offline acoustic playback and transposition, free, open source and offline.

## Ideas being considered

Features that have been studied but not yet built are described in [documentation/pistes.md](documentation/pistes.md) (in French).

## Contact and license

Contact: [pilmil.studio@proton.me](mailto:pilmil.studio@proton.me). Distributed under the [Mozilla Public License 2.0](LICENSE) (MPL-2.0): you can use, study, modify and redistribute it; files you modify must stay under the same license, with their source code.

## A note on development

Lunaison's code was developed with the help of an AI assistant (Claude, by Anthropic): the app itself embeds no artificial intelligence, all health guides are written and verified from public clinical sources (HAS, NHS, CNGOF, INSERM), the cycle engine is purely deterministic and covered by automated tests, and I personally maintain the project and answer issues and messages myself.
