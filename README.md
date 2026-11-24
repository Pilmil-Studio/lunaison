# Lunaison

**Français** · [English](README.en.md)

[![CI](https://github.com/Pilmil-Studio/lunaison/actions/workflows/ci.yml/badge.svg)](https://github.com/Pilmil-Studio/lunaison/actions/workflows/ci.yml)

Suivi du cycle menstruel, libre et sans compte. Pour noter ses règles, ses symptômes et son humeur, comprendre ce qui se passe à chaque phase, savoir quand arrivent les prochaines règles, et prévenir son ou sa partenaire si on le souhaite.

Lunaison est une appli gratuite et libre (MPL-2.0, Android et iOS), sans publicité, sans traceur et **sans accès à Internet**. Tout reste sur le téléphone.

**[Site de Lunaison](https://pilmil-studio.github.io/lunaison/)** · **Google Play** (bientôt) · **App Store** (bientôt) · **[Télécharger l'APK](https://github.com/Pilmil-Studio/lunaison/releases/latest)** · [Pour les professionnels de santé](https://pilmil-studio.github.io/lunaison/professionnels.html) · [Sources médicales](https://pilmil-studio.github.io/lunaison/sources.html) · [Confidentialité](https://pilmil-studio.github.io/lunaison/confidentialite.html)

**Avant la sortie sur Google Play :** [rejoindre le test interne](https://play.google.com/apps/internaltest/4699280816823618019) (réservé aux personnes déjà ajoutées à la liste de testeurs : [comment devenir testeur ou testeuse](https://pilmil-studio.github.io/lunaison/testeur.html)).

## Ce que fait l'appli

- **Noter ses journées en quelques secondes** : flux, douleur (décrite par son effet), symptômes (8 symptômes courants affichés par défaut, plus symptômes cliniques dédiés aux douleurs pelviennes, à l'endométriose et à la périménopause), humeurs, prise de contraception ou de traitement, signes d'ovulation facultatifs (glaire cervicale et température basale sans clavier), note personnelle. **Ruban des 7 derniers jours sur l'accueil** pour visualiser ses journées récentes et compléter un jour oublié en un tap.
- **Prévoir les règles et s'adapter à chaque étape de vie** : une date estimée et une plage plausible calculées sur les six derniers cycles, mode **cycles naturellement irréguliers** (périménopause, SOPK, post-partum) et possibilité de mettre les prévisions en pause (grossesse, allaitement, contraception continue). Une estimation de la fenêtre fertile existe pour les adultes qui l'activent dans les réglages. Elle est désactivée par défaut, n'est jamais proposée aux moins de 18 ans, est souvent fausse et ne remplace pas une contraception.
- **Comprendre la phase en cours et les quatre phases** : ce qui se passe dans le corps, ce qui aide, et ce qui revient chez soi pendant chacune des quatre phases d'après les cycles précédents.
- **Générer un bilan PDF ou sur une page pour un soignant** : accessible en un tap depuis l'onglet Calendrier ou les réglages (`HealthReportPdf`). Durée médiane et régularité des 6 derniers cycles (repères FIGO/ACOG), durée des règles, jours de douleur modérée ou forte, saignements hors règles, **repères douleurs et endométriose sur 90 jours (les 5 « D »)** et symptômes récurrents, prêt à montrer ou partager en consultation.
- **Un conseil sourcé par jour et une minute de respiration guidée** : le conseil change chaque jour selon la phase et répond en priorité à ce qui a été noté ; un exercice de respiration abdominale (4 s / 6 s) aide à relâcher les crampes ou la tension prémenstruelle.
- **Repérer ce qui mérite un avis médical** : douleurs fortes répétées, règles de plus de 8 jours, règles abondantes répétées (repères du NHS), cycles hors des repères, saignements entre les règles, absence de règles, retard prolongé. Jamais un diagnostic.
- **Lire des fiches sourcées et schémas anatomiques** : cycle, douleurs, alimentation, syndrome prémenstruel, quand consulter, contraception, **endométriose, SOPK, adénomyose, périménopause**, limites des prévisions, fiche pour l'entourage, ainsi que des schémas anatomiques (appareil reproducteur et vulve).
- **Migration et import de données sans serveur** : import direct des sauvegardes de l'application *Calendrier des règles* (archives ZIP protégées par mot de passe ou JSON) et fichiers CSV Lunaison, analysés et reconstitués en local sans envoyer le moindre octet sur Internet.
- **Rappels discrets** : avant les règles, pour noter sa journée, ou chaque jour pour sa contraception ou son traitement, toujours neutres sur l'écran verrouillé (« Lunaison »).
- **Partager avec un ou une partenaire, par message ou par QR Code** (18 ans et plus) : préparation de messages dans la messagerie choisie, conseils de soutien concrets pour le ou la partenaire, et partage direct des estimations par **QR Code** (affiché et scanné dans l'appli) ou par le menu de partage d'Android (SMS, Signal, WhatsApp…), d'un téléphone à l'autre sans passer par un serveur.
- **Personnalisation, code PIN et widget d'accueil** : 4 palettes de couleurs (clair et sombre), verrouillage facultatif par code à 4 chiffres (avec masquage dans les applications récentes) et widget d'accueil discret.

Lunaison n'est **ni un dispositif médical ni une méthode de contraception**.

## Aperçu

<table>
  <tr>
    <td align="center"><img src="docs/captures/accueil.png" width="220" alt="Accueil : phase du cycle, prochaines règles estimées, conseil du jour et bouton Noter aujourd'hui"><br>Accueil</td>
    <td align="center"><img src="docs/captures/journee.png" width="220" alt="Ma journée : pastilles pour le flux, la douleur, les symptômes et l'humeur"><br>Ma journée</td>
    <td align="center"><img src="docs/captures/calendrier.png" width="220" alt="Calendrier mensuel des règles et des journées notées"><br>Calendrier</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/comprendre.png" width="220" alt="Comprendre : bilan des cycles, quatre phases et fiches médicales sourcées"><br>Comprendre</td>
    <td align="center"><img src="docs/captures/fiche_douleurs.png" width="220" alt="Fiche sourcée sur les règles douloureuses avec niveau de preuve et références"><br>Fiche sourcée</td>
    <td align="center"><img src="docs/captures/partage.png" width="220" alt="Partage partenaire par message ou QR Code hors-ligne"><br>Partage partenaire</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/reglages.png" width="220" alt="Réglages : thème, rappels discrets, options du cycle et export CSV"><br>Réglages</td>
    <td align="center"><img src="docs/captures/accueil_sombre.png" width="220" alt="Accueil en thème sombre chaud"><br>Thème sombre</td>
    <td align="center"><img src="docs/captures/bienvenue.png" width="220" alt="Écran de bienvenue sans création de compte"><br>Bienvenue</td>
  </tr>
</table>

Les captures sont générées automatiquement à partir du code des écrans à chaque modification.

## Parti pris

- **Pas de permission Internet** : l'application ne demande aucun accès réseau à Android (seule la caméra est demandée, pour scanner un QR Code de partenaire). Vos dates de règles, symptômes et notes restent sur votre téléphone.
- **Tout est inclus d'emblée** : le mode périménopause, le bilan de consultation, les signes d'ovulation et le partage partenaire sont accessibles sans abonnement.
- **Un ton posé, sans couleur rouge** : un retard ou un symptôme inhabituel est expliqué simplement avec ses références médicales.
- **Aucun compteur de régularité** : oublier de noter quelques jours est normal et n'empêche pas l'application de fonctionner.

## Pourquoi Lunaison est gratuite

Lunaison est née d'un besoin de ma compagne : suivre son cycle plus facilement, et me permettre de l'accompagner au quotidien. Les applications existantes réservent souvent leurs fonctions utiles à un abonnement, ou se financent avec les données de leurs utilisatrices, alors que ce sont parmi les plus intimes qui soient. Lunaison fait l'inverse : elle est gratuite, libre, fonctionne sans Internet et garde tout sur le téléphone.

L'appli reste au strict nécessaire. Ce qu'elle ne dit pas elle-même, d'où viennent ses repères et pourquoi elle est faite ainsi, est expliqué dans [Sources et choix de conception](https://pilmil-studio.github.io/lunaison/sources.html).

## Vie privée

- Aucune permission Internet, aucun SDK tiers, aucun compte.
- **Sauvegarde dans le nuage désactivée par défaut** (`pilmil-core`) : les données de santé ne passent par aucun serveur. Sont disponibles le transfert direct vers un nouveau téléphone, l'export et l'import **CSV Lunaison** (en-tête et valeurs identiques dans toutes les langues), et, sur Android seulement, la **sauvegarde chiffrée complète `.pilmil`** (AES-256-GCM, mot de passe de votre choix : journal et réglages, sans le code PIN) avec restauration qui remplace les données du téléphone et l'**import direct des archives de l'application Calendrier des règles**, traités 100 % localement.
- **Verrouillage par code PIN** avec temporisation anti-bruteforce (`PinThrottle`) et masquage dans les applications récentes.
- Le sélecteur de contacts du système donne accès au seul contact choisi, sans autorisation sur le carnet d'adresses.
- Effacement complet en un geste.

## Technique

Kotlin Multiplatform, Compose Multiplatform, Material 3, Room KMP (`sqlite-bundled`), WorkManager (Android), iOS (`iosApp/`). Android `minSdk 26`, `targetSdk 36`.

L'appli est disponible en **6 langues** (`fr`, `en`, `pl`, `uk`, `es`, `de`), selon la langue du téléphone. Les textes d'interface passent par `tr()` dans `:core:model`, et les fiches ont un fichier par langue dans `:core:content`, vérifié par les tests.

### Couverture par langue

<!-- couverture-langues:debut -->
| Langue | Textes de l'appli (`tr`) | Fiches Comprendre | Chaînes Android | Couverture |
|---|---|---|---|---|
| Français | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |
| Anglais | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |
| Polonais | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |
| Ukrainien | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |
| Espagnol | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |
| Allemand | 1039 / 1039 (100 %) | 10 / 10 | 4 / 4 | **100 %** |

Aucun texte affiché en dur hors `tr` repéré.
Un texte non traduit s'affiche en anglais. Tableau produit par `scripts/couverture-langues.py`.
<!-- couverture-langues:fin -->

Ce que le tableau ne mesure pas :

| | Français | Anglais | Polonais | Ukrainien | Espagnol | Allemand |
|---|---|---|---|---|---|---|
| Fiche Google Play | oui | oui | oui | oui | oui | oui |
| Site | complet | complet | accueil seul | accueil seul | accueil seul | accueil seul |
| Politique de confidentialité | oui | oui | non | non | non | non |
| Relecture par une personne native | oui | à faire | à faire | à faire | à faire | à faire |

Les traductions polonaise, ukrainienne, espagnole et allemande ont été écrites avec l'aide d'une IA et n'ont pas encore été relues par des natifs.

```
:app                 Coque Android, navigation, rappels WorkManager, widget, intégration pilmil-core
:ui                  Interface Compose Multiplatform partagée (Android + iOS : Accueil, Ma journée, Calendrier, Comprendre, Partage, Réglages, bilan PDF)
:core:model          Journée notée et langues supportées (KMP commonMain)
:core:cycle          Moteur : repérage des règles, prévision adaptative, corrélation de symptômes, phases, signaux, rappels (KMP commonMain, testé)
:core:content        Fiches (6 langues), conseils par phase, messages de partage, sources (KMP commonMain, testé)
:core:data           Base Room KMP locale (Android + iOS), migrations non destructives, chiffrement PIN + PinThrottle
:core:designsystem   Thème, 4 palettes WCAG AA, police Atkinson Hyperlegible, composants accessibles (KMP)
:screenshots         Tests de captures d'écran
iosApp               Coque Xcode / SwiftUI iOS
```

Les règles du moteur et leurs références sont dans [`CycleConfig.kt`](core/cycle/src/commonMain/kotlin/fr/lunaison/core/cycle/CycleConfig.kt). Les sources des contenus et les choix de conception sont dans [documentation/sources.md](documentation/sources.md), publié sur le site : [Sources et choix de conception](https://pilmil-studio.github.io/lunaison/sources.html). Après une modification, régénérer la page avec `python3 scripts/site-sources.py`.

Tests : `./gradlew test`. Construction : `./gradlew :app:assembleDebug`.

## Du même atelier (Pilmil Studio)

- **[Sommelle](https://github.com/Pilmil-Studio/sommelle)** : accompagnement de l'insomnie chronique par la TCC-I en 6 à 8 semaines, gratuit, libre et sans accès à Internet.
- **[Respire](https://github.com/Pilmil-Studio/respire)** : une courte pause de respiration avant les applications que l'on ouvre par réflexe, avec filtre des Reels et Shorts.
- **[Sauve](https://github.com/Pilmil-Studio/sauve)** : premiers secours « anti-panique », gratuit, libre et sans accès à Internet, avec guidage vocal et haptique mains libres.
- **[Sillon](https://github.com/Pilmil-Studio/sillon)** : potager vivant et calendrier agronomique indexé sur la date des dernières gelées, gratuit, libre et sans accès à Internet.
- **[Fredonne](https://github.com/Pilmil-Studio/fredonne)** : saisie mélodique, écoute acoustique hors-ligne et transposition, gratuit, libre et sans accès à Internet.

## Pistes envisagées

Les fonctions étudiées mais pas encore réalisées sont décrites dans [documentation/pistes.md](documentation/pistes.md).

## Contact et licence

Contact : [pilmil.studio@proton.me](mailto:pilmil.studio@proton.me). Distribuée sous licence [Mozilla Public License 2.0](LICENSE) (MPL-2.0) : vous pouvez l'utiliser, l'étudier, la modifier et la redistribuer ; les fichiers que vous modifiez doivent rester publiés sous la même licence, avec leur code source.

## Note sur le développement

Le code de Lunaison a été développé avec l'aide d'un assistant IA (Claude, d'Anthropic) : l'application elle-même n'embarque aucune intelligence artificielle, tous les contenus de santé sont rédigés et vérifiés à partir des sources publiques (HAS, NHS, CNGOF, INSERM), le moteur du cycle est purement déterministe et couvert par des tests automatiques, et je maintiens personnellement le projet en répondant moi-même aux tickets et aux messages.
