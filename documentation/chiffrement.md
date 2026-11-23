# Chiffrement de la base locale

> État : branche `chiffrement-base`. Android : base chiffrée par SQLCipher, sauvegarde automatique du fichier `.pilmil` vers un dossier choisi, restauration au premier lancement. iOS : protection de fichier du système seulement.

## 1. Solution retenue

Sur Android, Room 2.7 utilise encore le moteur SQLite du système à travers `SupportSQLiteOpenHelper` (pas de `BundledSQLiteDriver` côté Android). La solution la plus simple réellement compatible est donc SQLCipher pour Android (`net.zetetic:sqlcipher-android`) branché par `openHelperFactory(SupportOpenHelperFactory(...))` : aucun changement de schéma Room, aucun changement de DAO, mêmes migrations.

- Version 4.13.0 : dernière version connue qui dépend encore d'`androidx.sqlite` 2.2 (les versions 4.19 et suivantes tirent `androidx.sqlite` 2.7 et Kotlin 2.2, incompatibles avec la configuration actuelle). Pages mémoire de 16 Ko prises en charge depuis la 4.6.1. À faire monter avec Room.
- Pas de changement de version du schéma Room (la version 9 vient de `main`, migration de données seule) : le chiffrement est transparent pour Room.
- Clé passée au format brut `x'…'` : SQLCipher l'utilise directement, sans dérivation PBKDF2 (inutile pour une clé déjà aléatoire de 256 bits).
- Règles R8 : SQLCipher fournit les siennes ; `app/proguard-rules.pro` les répète par sécurité.
- Taille : la bibliothèque native `libsqlcipher.so` est ajoutée pour chaque architecture (voir le rapport de CI pour l'écart mesuré sur l'APK de développement). Un APK universel grossit nettement plus qu'un téléchargement depuis Google Play, qui ne livre qu'une architecture.

Sur iOS, la base passe par `BundledSQLiteDriver` (Room multiplateforme). Il n'existe pas de pilote SQLCipher maintenu pour Room sur iOS ; en fabriquer un dépasse cette étape. Le dossier de la base reçoit la protection `NSFileProtectionComplete` (voir section 8).

## 2. Modèle de menace

Ce que le chiffrement protège :
- une copie du fichier `lunaison.db` sortie du téléphone par un autre chemin que l'appli : sauvegarde `adb`, extraction du système de fichiers, outil de transfert ou de réparation, téléphone d'occasion mal effacé ;
- les restes de la base sur le stockage après suppression, une fois la conversion faite.

Ce qu'il ne protège pas :
- un téléphone allumé et déverrouillé, ou une appli malveillante disposant des droits root : la clé est utilisable par l'appli à tout moment ;
- ce que l'appli affiche ou exporte (bilan PDF, CSV, partage, sauvegarde `.pilmil` protégée par son propre mot de passe) ;
- les fichiers `.pilmil` sortis du téléphone (dossier choisi, copie Google, exports) : ils ne valent que ce que vaut leur phrase, un fichier copié peut être attaqué hors ligne (PBKDF2, 600 000 itérations) ; d'où la longueur minimale et l'indicateur de la phrase ;
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

Ordre avec les migrations Room : la conversion copie la base telle quelle, version du schéma comprise (par exemple 8). Room ouvre ensuite la base chiffrée et rejoue seul les migrations manquantes (8 vers 9 : réglage de fertilité remis à zéro). La conversion ne migre rien et ne dépend pas de la version courante ; un test (`la conversion copie la base telle quelle et Room migre ensuite de 8 a 9`) vérifie que la version est conservée et qu'il ne reste que la migration 8 vers 9 à jouer.

Reprise après un arrêt brutal (fonction pure `planNextStep`, testée pour chaque état) :

| État observé au lancement | Action |
|---|---|
| copie `-chiffrement` présente | jetée, jamais utilisée |
| pas de base active, ancienne présente | l'ancienne est remise en place, puis conversion reprise |
| base chiffrée active et ancienne présente | vérification de la base chiffrée, puis effacement de l'ancienne ; si la base chiffrée ne s'ouvre pas, elle est mise de côté et l'ancienne revient |
| base en clair active et ancienne présente (anormal) | l'ancienne est renommée `-ancienne-conservee-<date>`, rien n'est effacé |

En cas d'échec (moteur natif absent, export ou vérification en erreur), l'appli s'ouvre sur la base d'origine en clair, intacte, et retente au lancement suivant. Si la base d'origine n'est plus trouvable à cet instant, l'appli s'arrête plutôt que de démarrer sur une base vide.

## 5. Sauvegarde système et clé

La clé d'enveloppe du Keystore ne quitte jamais le téléphone. Une base chiffrée copiée sur un autre téléphone y est donc illisible. Stratégie retenue :

- la base, `lunaison_cle_base.xml` (clé enveloppée) et `lunaison_phrase_sauvegarde.xml` (phrase enveloppée) ne partent dans AUCUNE sauvegarde ni AUCUN transfert : `data_extraction_rules.xml` n'inclut que le dossier privé `sauvegarde-privee` du domaine `file`, pour la copie dans le compte Google comme pour le transfert direct. Dès qu'un `<include>` existe, tout le reste est exclu. Avant Android 12, `fullBackupContent="false"` coupe déjà tout ;
- `allowBackup` est actif (`true` par défaut, propriété `pilmil.allowAutoBackup`), sinon ni le transfert direct ni la copie Google ne fonctionnent. `LunaisonBackupAgent` filtre : le transfert direct est toujours permis (Android 12 ou plus), la copie dans le compte Google n'est permise que si la personne l'a activée, qu'une phrase secrète existe et qu'Android la chiffre de bout en bout (`BackupPolicy`, testée). Avant Android 12, rien ne part et le réglage est masqué ;
- si une base chiffrée arrive malgré tout sans clé utilisable (règles modifiées, outil de transfert d'un fabricant), elle est renommée `lunaison.db-illisible-<date>`, jamais effacée, un repère `lunaison.db-cle-perdue` est posé, et l'appli démarre sur une base neuve en proposant l'écran de restauration (section 6).

Import, export CSV, sauvegarde et restauration `.pilmil`, bilan PDF et partage passent tous par le DAO Room : aucun code ne copie le fichier de base. Ils fonctionnent à l'identique.

## 6. Si le téléphone est perdu

Sans serveur, sans compte, sans permission Internet. L'historique ne se retrouve que par un fichier `.pilmil` (AES-256-GCM, clé dérivée par PBKDF2 d'une phrase secrète ou d'un mot de passe, `BackupVault` de `pilmil-core`, format inchangé) et la phrase qui l'ouvre.

```
                 téléphone                                        nouveau téléphone
  +------------------------------------------+
  | base chiffrée (SQLCipher)  ---- jamais ----+--X
  | clés Keystore              ---- jamais ----+--X
  |                                            |
  | tâche WorkManager (1 fois par jour,        |
  | si les données ont changé)                 |
  |   JSON de la base -> .pilmil (phrase)      |
  |     |            |                         |
  |     v            v                         |
  | A. dossier   B. dossier privé de l'appli   |   C. export à la main
  |    choisi       sauvegarde-privee/         |      (Réglages, Données)
  |    (3 fichiers)  derniere.pilmil           |         |
  +-----|------------|-------------------------+         |
        |            | copie Google (si activée,         |
        |            | Android 12+, chiffrée de bout     |
        |            | en bout) ou transfert direct      |
        v            v                                   v
   A. le fichier   B. restauré par Android        C. le fichier est copié
      est copié       dans sauvegarde-privee/        à la main
      à la main                |                         |
        |                      v                         |
        |        l'appli trouve le fichier,              |
        |        base vide : écran de restauration       |
        |        avec le fichier déjà choisi             |
        v                      v                         v
  « Restaurer une sauvegarde » : fichier .pilmil + phrase ou mot de passe
                               -> journal et réglages (sans le PIN)
```

Les trois chemins :

1. **A. Dossier choisi** (Réglages, Sauvegarde automatique). Dossier désigné par `ACTION_OPEN_DOCUMENT_TREE`, permission d'URI persistante sur ce seul dossier : aucun accès au stockage global, aucune permission au manifeste. Fichiers `sauvegarde-AAAA-MM-JJ.pilmil` (un numéro est ajouté si plusieurs dans la journée, jamais d'écrasement), les 3 derniers sont conservés, les autres fichiers du dossier ne sont jamais touchés. Sur un nouveau téléphone : « Restaurer une sauvegarde » à l'ouverture de l'appli, fichier, phrase.
2. **B. Copie Android** (désactivée par défaut). Réglage « Copier dans mon compte Google », activable seulement si une phrase est définie, sur Android 12 ou plus, avec un verrouillage d'écran. Seul le dernier `.pilmil`, que l'appli garde dans `filesDir/sauvegarde-privee/derniere.pilmil`, est concerné (aussi pour le transfert direct, toujours permis). Si aucun dossier n'est choisi, la sauvegarde périodique alimente quand même ce dossier privé. Sur le nouveau téléphone, Android restaure le fichier avant le premier lancement : la base est vide, l'accueil propose « Restaurer la sauvegarde retrouvée », le fichier est déjà choisi, il ne reste qu'à saisir la phrase.
3. **C. Sauvegarde manuelle** (Réglages, Données). Inchangée : mot de passe de la personne, fichier rangé où elle veut.

Cas de la base illisible (section 5) : le même écran de restauration s'affiche, avec une phrase d'explication ; si un `.pilmil` privé est présent, il est déjà sélectionné. Le repère est effacé quand la personne a restauré ou commencé à zéro.

### Où est la phrase secrète

- Elle est saisie deux fois (12 caractères au minimum, indicateur de longueur, `PassphraseRules`), jamais envoyée nulle part.
- Elle n'est conservée sur l'appareil que sous forme enveloppée : AES-256-GCM par une clé non exportable du Keystore (alias `lunaison_cle_enveloppe_phrase`, distincte de celle de la base `lunaison_cle_enveloppe_base`), dans le fichier de préférences `lunaison_phrase_sauvegarde`, relue après écriture (`BackupPassphraseStore`, testée avec un Keystore simulé). Jamais en clair, jamais dans un journal (seuls des noms d'états sont journalisés), jamais dans une sauvegarde système.
- La tâche d'arrière-plan la déchiffre en mémoire, s'en sert, puis écrase le tableau de caractères. La saisie à l'écran reste un `String` du composant de texte jusqu'à la fermeture de la fenêtre : limite connue.

### Sauvegarde périodique

`AutoBackupWorker` (WorkManager, une fois par jour, aucune contrainte réseau) :

1. rien si aucune phrase n'est définie, ni si la base est vide (une base vide ne doit jamais remplacer de vraies sauvegardes) ;
2. rien si la dernière sauvegarde réussie date d'aujourd'hui, ou si l'empreinte (SHA-256) des données n'a pas changé depuis la dernière réussite ; « Sauvegarder maintenant » et la fin de la configuration passent outre ; une exécution précédente en échec est retentée ;
3. chiffrement, vérification de relecture en mémoire, écriture de la copie privée (renommage atomique, relecture), écriture dans le dossier, relecture et déchiffrement du fichier écrit ;
4. seulement alors, suppression des fichiers au-delà des 3 derniers. Si la vérification échoue, le fichier raté est retiré et les anciens ne sont pas touchés.

Aucun échec n'est silencieux : le résultat (réussite, dossier inaccessible, phrase illisible, écriture impossible, relecture en échec, copie privée en échec) est mémorisé, affiché dans les réglages (avec la date de la dernière réussite et le dossier) et signalé sur l'accueil. Le dossier est aussi testé à l'affichage : permission révoquée ou stockage retiré apparaissent sans attendre la prochaine exécution.

Sur l'accueil, tant qu'aucune sauvegarde automatique n'est configurée et qu'au moins 14 jours sont saisis, une carte discrète rappelle que les données n'existent que sur ce téléphone (« Plus tard » la masque 30 jours ; ni notification ni insistance).

### Si la phrase est oubliée

Personne ne peut rouvrir les fichiers : ni la personne, ni l'éditeur, ni un tiers. Il n'existe ni phrase de secours, ni mécanisme de récupération (ce serait un point faible) ni serveur qui en détienne une copie. L'écran de configuration le dit avant validation et propose de la noter sur papier. Conséquences :

- téléphone encore là : « Changer de phrase » (les nouveaux fichiers utilisent la nouvelle phrase ; les anciens restent protégés par l'ancienne) ;
- téléphone perdu et phrase oubliée : les fichiers de sauvegarde automatique et la copie Google sont inutilisables. Seule une sauvegarde manuelle (chemin C, autre mot de passe) ou un export CSV peut encore servir ;
- si le Keystore perd la clé d'enveloppe de la phrase (restauration d'un téléphone, réinitialisation de sécurité), l'état « phrase illisible » est signalé et la personne en définit une nouvelle.

### Limites

- Pas de sauvegarde automatique sur iOS dans cette étape : la section est masquée (`autoBackupAvailable = false`).
- « Désactiver » oublie la phrase, annule la tâche, supprime la copie privée et remet la copie Google à « désactivé » ; les fichiers déjà écrits dans le dossier de la personne ou envoyés au compte Google ne sont pas supprimés par l'appli.
- « Tout effacer » supprime la copie privée, pas les fichiers du dossier choisi.
- Une copie restaurée par Android reste dans `sauvegarde-privee` tant qu'une nouvelle sauvegarde ne la remplace pas.
- Le dossier choisi peut appartenir à un service en ligne installé sur le téléphone : ce service synchronise alors le fichier, déjà chiffré, sous son propre contrôle.

## 7. Fichiers de la base sur Android

`lunaison.db` (et ses `-wal`, `-shm`), plus, de façon transitoire, `lunaison.db-chiffrement` et `lunaison.db-ancienne`. Les fichiers `-illisible-`, `-cle-perdue` et `-ancienne-conservee-` n'apparaissent que dans les cas anormaux décrits ci-dessus ; « Tout effacer » ne les supprime pas, la désinstallation si.

## 8. Limites iOS

- Pas de chiffrement propre à l'appli : la base est protégée par la protection de données d'iOS, classe `NSFileProtectionComplete`, appliquée au dossier `Application Support/Lunaison` et à ses fichiers. Les fichiers sont illisibles tant que l'iPhone est verrouillé, à condition qu'un code soit défini.
- Une sauvegarde iCloud ou locale de l'iPhone (incluse par défaut, désactivable dans les réglages de l'appli) contient la base sous la protection d'Apple, pas sous une clé de l'appli.
- Une écriture lancée juste avant le verrouillage peut échouer après une dizaine de secondes. L'appli iOS ne fait aujourd'hui aucun travail en arrière-plan ; à revoir quand les rappels locaux arriveront (classe `CompleteUnlessOpen` à envisager).
- Piste : un pilote `SQLiteDriver` basé sur SQLCipher pour iOS, quand une version maintenue existera.

## 9. Plan de test manuel sur téléphone réel (avant toute fusion)

1. **Migration d'une base existante** : installer la dernière version publiée, saisir au moins 30 jours (notes, températures, poids) et un code PIN, fermer l'appli. Installer l'APK de cette branche par-dessus (`adb install -r`), ouvrir : toutes les données et le PIN sont là. Vérifier par `adb shell run-as fr.lunaison head -c 16 databases/lunaison.db | xxd` (version de développement) que l'en-tête n'est plus `SQLite format 3`, et qu'il ne reste ni `-chiffrement` ni `-ancienne`.
2. **Arrêt brutal pendant la conversion** : refaire l'étape 1 avec une base plus grosse (import CSV de plusieurs années) en tuant l'appli (`adb shell am kill fr.lunaison`) une fraction de seconde après l'ouverture, plusieurs fois ; au lancement suivant, aucune donnée ne manque.
3. **Usage normal après conversion** : ajouter, modifier, supprimer une journée ; redémarrer le téléphone ; vérifier rappels et widget, téléphone verrouillé.
4. **Sauvegarde `.pilmil`** : exporter, tout effacer, restaurer le fichier : tout revient.
5. **Export CSV, bilan PDF, partage QR** : identiques à la version précédente.
6. **Changement de téléphone par transfert direct** : transfert direct vers un second téléphone Android 12 ou plus, sans sauvegarde automatique configurée : Lunaison y démarre comme neuve, sans plantage ; la restauration `.pilmil` y fonctionne. Avec la sauvegarde automatique configurée : voir l'étape 11.
7. **Version publiée (R8)** : refaire les étapes 1 et 3 avec un APK de publication signé, pour vérifier les règles de réduction du code.
8. **Taille et appareils** : comparer la taille de l'APK publié et tester sur un appareil à pages mémoire de 16 Ko (Android 15 ou émulateur dédié).
9. **Sauvegarde automatique** (téléphone avec au moins 14 jours saisis) : la carte « Vos données n'existent que sur ce téléphone » s'affiche sur l'accueil ; « Plus tard » la masque, elle revient 30 jours après. Réglages, Sauvegarde automatique : choisir un dossier (sélecteur du système), saisir deux fois une phrase (refus sous 12 caractères, avertissement lu avant validation), activer : une première sauvegarde est écrite tout de suite, le résultat s'affiche, `sauvegarde-AAAA-MM-JJ.pilmil` est dans le dossier, la date de dernière réussite et le dossier sont affichés. Vérifier au manifeste (`aapt dump permissions`) l'absence d'INTERNET et de permission de stockage.
10. **Règles et rotation** : « Sauvegarder maintenant » plusieurs fois le même jour (fichiers `-2`, `-3`), modifier une journée, avancer la date du téléphone de 3 jours avec une modification chaque jour : seuls les 3 derniers fichiers restent, un fichier étranger placé dans le dossier n'est pas touché. Sans changement de données, aucune nouvelle sauvegarde le lendemain. Un `.pilmil` du dossier s'ouvre avec la phrase par « Restaurer une sauvegarde » (Réglages) et pas avec une autre.
11. **Perte simulée du téléphone** : sur le téléphone A (Android 12 ou plus, verrouillage d'écran), activer la sauvegarde automatique et « Copier dans mon compte Google », attendre la sauvegarde Android (`adb shell bmgr backupnow fr.lunaison`). Sur un téléphone B neuf (ou réinitialisé), même compte Google : restaurer l'appli depuis la sauvegarde Android ; à l'ouverture, Lunaison est vide et affiche « Restaurer la sauvegarde retrouvée » ; la mauvaise phrase donne une erreur claire et ne change rien, la bonne phrase rend tout le journal. Refaire avec le fichier du dossier copié à la main sur B (« Restaurer une sauvegarde », choix du fichier), puis avec le transfert direct A vers B.
12. **Dossier inaccessible** : révoquer l'accès (Paramètres, Applis, Lunaison, ou supprimer le dossier, ou retirer la carte SD) : les réglages l'affichent clairement, l'accueil signale le problème, « Changer de dossier » le règle. « Changer de phrase » puis « Désactiver » (phrase oubliée, copie privée supprimée, copie Google remise à désactivé).
13. **Base sans clé** : copier le fichier `lunaison.db` d'un autre téléphone (`adb`) sur un téléphone de test puis lancer l'appli : base mise de côté, accueil avec la phrase d'explication et « Restaurer une sauvegarde ».
