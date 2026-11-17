# Feuille de route : Lunaison

> Dernière mise à jour : novembre 2026. Détails iOS : [documentation/ios.md](documentation/ios.md).

## 1. Fonctionnalités à venir
- **v1.x** :
  - Prise en charge de formats d'import complémentaires (Clue, Flo, Drip) lorsque des exports exploitables existent.
  - Intégration de `pilmil-core` (`BackupVault` `.pilmil` AES-256-GCM) pour mutualiser l'export chiffré.

## 2. Aspect visuel & UX
- Audit visuel des écrans secondaires en mode nuit et avec grandes polices système (TalkBack / contraste AA-AAA).
- Maintien des captures d'écran Paparazzi à jour pour les fiches stores (6 langues).

## 3. Qualité de code & sécurité
- **En cours** (branche `chiffrement-base`) : base Room chiffrée au repos par SQLCipher sur Android, conversion des bases existantes, protection de fichier sur iOS, sauvegarde automatique du fichier `.pilmil` vers un dossier choisi, restauration au premier lancement, copie facultative de ce seul fichier dans le compte Google ([documentation/chiffrement.md](documentation/chiffrement.md)). Reste : sauvegarde automatique et pilote chiffré pour iOS.
- Sauvegarde système Android : `allowBackup` actif, l'agent de sauvegarde applique le choix de la personne. Rien ne part dans le compte Google sans son accord, jamais sans chiffrement de bout en bout, et seul le dernier fichier `.pilmil` chiffré par sa phrase secrète est concerné (jamais la base ni les clés) ; le transfert direct vers un nouveau téléphone emporte ce même fichier.
- Renforcement des tests unitaires sur les parcours critiques (verrou PIN, temporisation `PinThrottle`, import/export, calculs de cycle) et suivi `lintDebug` en CI.
- À faire : ajouter `ktlint` (detekt tourne déjà en CI) et déplacer les tests sans `java.util.Locale` vers `commonTest`.

## 4. Feuille de route iOS
- **État actuel** : tous les modules KMP (`core:model`, `core:cycle`, `core:content`, `core:data` avec Room 2.7, `core:designsystem`, `ui`) compilent pour iOS en CI.
- **Ce qu'il reste** :
  1. Implémenter le pont natif encore absent dans `iosMain` : le déverrouillage biométrique.
  2. Vérifier VoiceOver, texte dynamique et réglage d'exclusion iCloud sur appareil réel, puis publier via TestFlight / App Store.
