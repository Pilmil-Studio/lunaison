# Chiffrement de la base locale

> État : branche `chiffrement-base`. Android : base chiffrée par SQLCipher. iOS : protection de fichier du système seulement.

## 1. Solution retenue

Sur Android, Room 2.7 utilise encore le moteur SQLite du système à travers `SupportSQLiteOpenHelper` (pas de `BundledSQLiteDriver` côté Android). La solution la plus simple réellement compatible est donc SQLCipher pour Android (`net.zetetic:sqlcipher-android`) branché par `openHelperFactory(SupportOpenHelperFactory(...))` : aucun changement de schéma Room, aucun changement de DAO, mêmes migrations.

- Version 4.13.0 : dernière version connue qui dépend encore d'`androidx.sqlite` 2.2 (les versions 4.19 et suivantes tirent `androidx.sqlite` 2.7 et Kotlin 2.2, incompatibles avec la configuration actuelle). Pages mémoire de 16 Ko prises en charge depuis la 4.6.1. À faire monter avec Room.
- Pas de changement de version du schéma Room (reste 8) : le chiffrement est transparent pour Room.
- Clé passée au format brut `x'…'` : SQLCipher l'utilise directement, sans dérivation PBKDF2 (inutile pour une clé déjà aléatoire de 256 bits).
- Règles R8 : SQLCipher fournit les siennes ; `app/proguard-rules.pro` les répète par sécurité.
- Taille : la bibliothèque native `libsqlcipher.so` est ajoutée pour chaque architecture (voir le rapport de CI pour l'écart mesuré sur l'APK de développement). Un APK universel grossit nettement plus qu'un téléchargement depuis Google Play, qui ne livre qu'une architecture.

Sur iOS, la base passe par `BundledSQLiteDriver` (Room multiplateforme). Il n'existe pas de pilote SQLCipher maintenu pour Room sur iOS ; en fabriquer un dépasse cette étape. Le dossier de la base reçoit la protection `NSFileProtectionComplete` (voir section 7).

## 2. Modèle de menace

Ce que le chiffrement protège :
- une copie du fichier `lunaison.db` sortie du téléphone par un autre chemin que l'appli : sauvegarde `adb`, extraction du système de fichiers, outil de transfert ou de réparation, téléphone d'occasion mal effacé ;
- les restes de la base sur le stockage après suppression, une fois la conversion faite.

Ce qu'il ne protège pas :
- un téléphone allumé et déverrouillé, ou une appli malveillante disposant des droits root : la clé est utilisable par l'appli à tout moment ;
- ce que l'appli affiche ou exporte (bilan PDF, CSV, partage, sauvegarde `.pilmil` protégée par son propre mot de passe) ;
- l'accès à l'appli elle-même : c'est le rôle du code PIN (`PinThrottle`), indépendant de la clé de base ;
- la base en clair restée sur la mémoire flash avant la conversion : l'ancienne base est écrasée par des zéros avant suppression, mais la mémoire flash peut garder d'anciens blocs. Le chiffrement de fichiers d'Android (obligatoire depuis Android 10) limite ce risque.

La clé n'est pas liée au déverrouillage de l'écran : les rappels et le widget doivent lire la base téléphone verrouillé.

## 3. Gestion de la clé

1. Au premier besoin, 32 octets aléatoires (`SecureRandom`) sont générés sur le téléphone.
2. Une clé AES-256-GCM non exportable est créée dans l'Android Keystore (alias `lunaison_cle_enveloppe_base`).
3. La clé de base est chiffrée (enveloppée) par cette clé, avec un vecteur aléatoire et des données associées fixes, puis enregistrée en Base64 dans le fichier de préférences `lunaison_cle_base`. Elle n'est jamais écrite en clair.
4. L'enregistrement est synchrone et relu avant toute utilisation : une clé qui ne se relit pas n'est jamais utilisée pour chiffrer.
5. Rien n'est en dur dans le code, et aucun message de journal ne contient la clé (seuls des noms d'étapes et des types d'erreur sont journalisés).

Code : `core/data/src/androidMain/.../DatabaseKey.kt` (logique testée) et `DatabaseKeyAndroid.kt` (Keystore, préférences).

## 4. Conversion des bases existantes

Au premier lancement après la mise à jour, avant que Room n'ouvre la base (`DatabaseEncryptor`, `SqlCipherEngine`) :

1. Lecture des 16 premiers octets : `SQLite format 3` signifie base en clair.
2. La clé est lue ou créée.
3. La base en clair est ouverte, sortie du mode WAL (tout le journal est reporté dans le fichier), puis décrite : version du schéma et nombre de lignes de chaque table.
4. Une nouvelle base chiffrée `lunaison.db-chiffrement` est créée ; la base en clair y est attachée et recopiée par `sqlcipher_export`. La version du schéma, que l'export ne recopie pas, est reportée.
5. La copie est rouverte avec la clé, contrôlée (`PRAGMA quick_check`) et comparée à l'originale (version et lignes de chaque table). Au moindre écart, elle est jetée.
6. Les journaux `-wal`, `-shm`, `-journal` de l'ancienne base sont supprimés (refus si le `-wal` contient encore des données).
7. Échange par deux renommages atomiques : `lunaison.db` devient `lunaison.db-ancienne`, puis la copie devient `lunaison.db`.
8. `lunaison.db-ancienne` est écrasée par des zéros puis supprimée, après avoir vérifié que la nouvelle base s'ouvre avec la clé.

Reprise après un arrêt brutal (fonction pure `planNextStep`, testée pour chaque état) :

| État observé au lancement | Action |
|---|---|
| copie `-chiffrement` présente | jetée, jamais utilisée |
| pas de base active, ancienne présente | l'ancienne est remise en place, puis conversion reprise |
| base chiffrée active et ancienne présente | vérification de la base chiffrée, puis effacement de l'ancienne ; si la base chiffrée ne s'ouvre pas, elle est mise de côté et l'ancienne revient |
| base en clair active et ancienne présente (anormal) | l'ancienne est renommée `-ancienne-conservee-<date>`, rien n'est effacé |

En cas d'échec (moteur natif absent, export ou vérification en erreur), l'appli s'ouvre sur la base d'origine en clair, intacte, et retente au lancement suivant. Si la base d'origine n'est plus trouvable à cet instant, l'appli s'arrête plutôt que de démarrer sur une base vide.

## 5. Sauvegarde, restauration et changement de téléphone

La clé d'enveloppe du Keystore ne quitte jamais le téléphone. Une base chiffrée copiée sur un autre téléphone y est donc illisible. Stratégie retenue :

- la base (domaine `database`) et le fichier `lunaison_cle_base.xml` sont exclus de toute sauvegarde et de tout transfert (`data_extraction_rules.xml`) ; avant Android 12, `fullBackupContent="false"` coupe déjà tout ;
- le transfert direct vers un nouveau téléphone ne copie donc plus le journal. Le nouveau téléphone démarre comme une installation neuve, l'ancien garde toutes ses données ;
- pour changer de téléphone, la sauvegarde chiffrée `.pilmil` (Réglages, Données) emporte tout le journal et les réglages, protégés par un mot de passe que la personne détient ; elle passe par Room et n'est pas touchée par le chiffrement de la base ;
- si une base chiffrée arrive malgré tout sans clé utilisable (règles modifiées, outil de transfert d'un fabricant), elle est renommée `lunaison.db-illisible-<date>`, jamais effacée, et l'appli démarre sur une base neuve. **Reste à faire** : afficher à ce moment un message qui propose la restauration depuis un fichier `.pilmil` (l'appli n'a aujourd'hui aucun écran pour ce cas).

Import, export CSV, sauvegarde et restauration `.pilmil`, bilan PDF et partage passent tous par le DAO Room : aucun code ne copie le fichier de base. Ils fonctionnent à l'identique.

### Ce qu'il faudra changer dans la demande de fusion « sauvegarde optionnelle » (n° 28)

Elle inclut `database`, `sharedpref` et `file` dans la copie Google et le transfert direct. Avec la base chiffrée, ces copies seraient illisibles. Avant de la fusionner après celle-ci :

1. garder l'exclusion de `database` et de `sharedpref/lunaison_cle_base.xml` dans `<cloud-backup>` et `<device-transfer>` ;
2. pour que la copie Google et le transfert direct emportent encore le journal, faire produire par `LunaisonBackupAgent` un export du contenu (le JSON de `LunaisonBackup`) chiffré par un secret que la personne détient, puis le réimporter dans `onRestoreFinished` après saisie du secret ; à défaut, retirer de l'écran de réglage et des pages du site toute promesse que l'historique suit ;
3. ajouter le message « base illisible, restaurer un fichier .pilmil » décrit plus haut.

## 6. Fichiers de la base sur Android

`lunaison.db` (et ses `-wal`, `-shm`), plus, de façon transitoire, `lunaison.db-chiffrement` et `lunaison.db-ancienne`. Les fichiers `-illisible-` et `-ancienne-conservee-` n'apparaissent que dans les cas anormaux décrits ci-dessus ; « Tout effacer » ne les supprime pas, la désinstallation si.

## 7. Limites iOS

- Pas de chiffrement propre à l'appli : la base est protégée par la protection de données d'iOS, classe `NSFileProtectionComplete`, appliquée au dossier `Application Support/Lunaison` et à ses fichiers. Les fichiers sont illisibles tant que l'iPhone est verrouillé, à condition qu'un code soit défini.
- Une sauvegarde iCloud ou locale de l'iPhone (incluse par défaut, désactivable dans les réglages de l'appli) contient la base sous la protection d'Apple, pas sous une clé de l'appli.
- Une écriture lancée juste avant le verrouillage peut échouer après une dizaine de secondes. L'appli iOS ne fait aujourd'hui aucun travail en arrière-plan ; à revoir quand les rappels locaux arriveront (classe `CompleteUnlessOpen` à envisager).
- Piste : un pilote `SQLiteDriver` basé sur SQLCipher pour iOS, quand une version maintenue existera.

## 8. Plan de test manuel sur téléphone réel (avant toute fusion)

1. **Migration d'une base existante** : installer la dernière version publiée, saisir au moins 30 jours (notes, températures, poids) et un code PIN, fermer l'appli. Installer l'APK de cette branche par-dessus (`adb install -r`), ouvrir : toutes les données et le PIN sont là. Vérifier par `adb shell run-as fr.lunaison head -c 16 databases/lunaison.db | xxd` (version de développement) que l'en-tête n'est plus `SQLite format 3`, et qu'il ne reste ni `-chiffrement` ni `-ancienne`.
2. **Arrêt brutal pendant la conversion** : refaire l'étape 1 avec une base plus grosse (import CSV de plusieurs années) en tuant l'appli (`adb shell am kill fr.lunaison`) une fraction de seconde après l'ouverture, plusieurs fois ; au lancement suivant, aucune donnée ne manque.
3. **Usage normal après conversion** : ajouter, modifier, supprimer une journée ; redémarrer le téléphone ; vérifier rappels et widget, téléphone verrouillé.
4. **Sauvegarde `.pilmil`** : exporter, tout effacer, restaurer le fichier : tout revient.
5. **Export CSV, bilan PDF, partage QR** : identiques à la version précédente.
6. **Changement de téléphone** : transfert direct vers un second téléphone Android 12 ou plus : Lunaison y démarre comme neuve, sans plantage ; la restauration `.pilmil` y fonctionne.
7. **Version publiée (R8)** : refaire les étapes 1 et 3 avec un APK de publication signé, pour vérifier les règles de réduction du code.
8. **Taille et appareils** : comparer la taille de l'APK publié et tester sur un appareil à pages mémoire de 16 Ko (Android 15 ou émulateur dédié).
