# Spécification technique — Prédiction adaptative de cycle 100 % locale (`lunaison`)

> Statut : Spécification de référence (15 novembre 2026).
> Objectif : Offrir des prédictions de cycle personnalisées et réalistes (y compris sur cycles irréguliers) **entièrement sur le téléphone**, sans jamais envoyer de données de santé intimes sur un serveur.

---

## 1. Enjeu de souveraineté et de confiance

La quasi-totalité des applications commerciales de suivi menstruel justifient la création d'un compte et l'envoi des données vers des serveurs distants par la promesse d'une « IA de prédiction ». Dans **Lunaison**, démontrer qu'une modélisation locale rigoureuse fonctionne **sans quitter le coffre chiffré par code PIN** répond directement à ce besoin :
- **Zéro permission réseau** (`INTERNET` absent du manifeste) ;
- **Zéro télémétrie, zéro compte** ;
- Données et paramètres appris conservés exclusivement sur le téléphone, dans la base locale (non chiffrée à ce jour, l'accès à l'appli pouvant être protégé par un code PIN), et inclus dans l'export `.pilmil` chiffré si l'utilisatrice sauvegarde son coffre).

---

## 2. Limites des moyennes naïves et approche retenue

Une simple moyenne arithmétique sur les 3 derniers cycles échoue dès qu'un cycle atypique survient (stress, voyage, post-partum, périménopause) et donne une date unique trompeuse.

### 2.1 Modèle bayésien hiérarchique léger (en Kotlin pur)
- **A priori populational neutre** : distribution initiale large (ex. moyenne 28–29 jours, dispersion réaliste), utilisée uniquement lorsqu'il y a 0 ou 1 cycle enregistré.
- **Mise à jour séquentielle personnelle** : à chaque nouveau cycle clos, le modèle met à jour localement :
  - la **durée typique personnelle** et sa **variabilité propre** (écart-type individuel) ;
  - le **poids décroissant des cycles anciens** (lissage exponentiel) pour s'adapter aux évolutions progressives du rythme ;
  - la **détection automatique des valeurs aberrantes** (un cycle exceptionnellement long ou court > 2,5 écarts-types est signalé et pondéré à la baisse afin de ne pas fausser les 3 mois suivants).
- **Prise en compte de la phase lutéale** (si la température basale ou le pic d'ovulation est saisi) : la phase lutéale étant nettement plus stable que la phase folliculaire chez une même personne, la prédiction des prochaines règles devient beaucoup plus précise dès qu'un indice ovulatoire est noté.

### 2.2 Restitution honnête par intervalle de confiance
Au lieu d'afficher une date unique faussement exacte :
- Affichage d'une **fenêtre probable** (ex. *« Règles attendues entre le 18 et le 22 mai »*, correspondant à l'intervalle à 80 % / 90 % issu de la variance personnelle) ;
- Indication sobre du niveau de confiance (*« Estimation encore large : 2 cycles enregistrés »* vs *« Rythme régulier sur 8 cycles »*).

---

## 3. Architecture et sécurité

- **Implémentation** : moteur déterministe en Kotlin pur dans `:shared` (zéro dépendance externe lourde, exécution < 1 ms, testable par tests unitaires).
- **Persistance** : les paramètres personnalisés peuvent être recalculés à la volée depuis l'historique local, sans même nécessiter de fichier séparé non chiffré.
- **Portabilité** : fonctionne à l'identique sur Android et iOS (via Kotlin Multiplatform) et reste compatible avec le transfert direct pair-à-pair et la sauvegarde `.pilmil`.
