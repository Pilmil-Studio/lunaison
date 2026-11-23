# Feuille de route : Lunaison

> Dernière mise à jour : novembre 2026. Détails iOS : [documentation/ios.md](documentation/ios.md).

## 1. Fonctionnalités à venir
- **v1.x** :
  - Prise en charge de formats d'import complémentaires (Flo, Drip) lorsque des exports exploitables existent.
  - Import depuis HealthKit sur iOS : l'interface commune (`HealthDataSource`) est prête, l'implémentation iOS reste à écrire.
  - Valider l'import Clue (aujourd'hui expérimental) sur un vrai export, puis ajuster `ClueKeyMapping`.
  - Intégration de `pilmil-core` (`BackupVault` `.pilmil` AES-256-GCM) pour mutualiser l'export chiffré.

## 2. Aspect visuel & UX
- Audit visuel des écrans secondaires en mode nuit et avec grandes polices système (TalkBack / contraste AA-AAA).
- Maintien des captures d'écran Paparazzi à jour pour les fiches stores (6 langues).

## 3. Qualité de code & sécurité
- **Prioritaire** : chiffrement complet de la base locale Room au repos (SQLCipher) en complément du verrou PIN (`PinThrottle`) et de la sauvegarde `.pilmil`.
- Sauvegarde système Android configurable par application (`-Ppilmil.allowAutoBackup=false` par défaut, règles `data_extraction_rules.xml` excluant le cloud).
- Renforcement des tests unitaires sur les parcours critiques (verrou PIN, temporisation `PinThrottle`, import/export, calculs de cycle).
- À faire : ajouter `ktlint` (detekt tourne déjà en CI) et déplacer les tests sans `java.util.Locale` vers `commonTest`.

## 4. Feuille de route iOS
- **État actuel** : tous les modules KMP (`core:model`, `core:cycle`, `core:content`, `core:data` avec Room 2.7, `core:designsystem`, `ui`) compilent pour iOS en CI.
- **Ce qu'il reste** :
  1. Implémenter le pont natif encore absent dans `iosMain` : le déverrouillage biométrique.
  2. Vérifier VoiceOver, texte dynamique et réglage d'exclusion iCloud sur appareil réel, puis publier via TestFlight / App Store.
