# Guide utilisateur — Lunaison

Lunaison est une application de suivi du cycle menstruel et de santé gynécologique 100 % hors-ligne, sans compte, sans publicité et sans accès réseau.

---

## 1. Premier lancement (`OnboardingScreen`)

L'accueil se déroule en **5 étapes** :
1. **Bienvenue** (`WELCOME`) : présentation du fonctionnement local (zéro serveur, zéro traceur) et rappel que Lunaison n'est ni un dispositif médical ni un moyen de contraception. Trois entrées possibles :
   - *Commencer (suivre mon cycle)* ;
   - *J'ai déjà des données : importer* (CSV Lunaison ou sauvegarde ZIP chiffrée / JSON de *Calendrier des règles*) ;
   - *Je suis partenaire* (scan d'un QR Code ou lecture d'un jeton `LUNA1:...` par SMS).
2. **Âge** (`AGE`) : *Moins de 18 ans* ou *18 ans ou plus* (adapte les repères de durée normale de cycle — 21 à 45 j à l'adolescence vs 24 à 38 j à l'âge adulte — et réserve le partage partenaire aux personnes majeures).
3. **Cycle habituel** (`CYCLE`) : date des dernières règles (jusqu'à 120 jours en arrière), durée habituelle du cycle (18–60 j, défaut 28 j) et des règles (2–10 j, défaut 5 j), avec option *« Je ne sais pas »*.
4. **Verrouillage par code PIN** (`LOCK`) : création facultative d'un code à 4 chiffres (`PinSetupDialog`). Lorsqu'il est actif, les captures d'écran et l'aperçu dans les applications récentes sont bloqués (`FLAG_SECURE`).
5. **Rappels** (`REMINDERS`) : activation des notifications locales discrètes (intitulé neutre « Lunaison » sur écran verrouillé).

---

## 2. Navigation principale (4 onglets dans `LunaisonApp`)

### 1. Aujourd'hui (`HomeScreen`)
- **Ruban des 7 derniers jours** (`RecentDaysStrip`) : accès direct pour compléter ou relire une journée récente.
- **Carte centrale du cycle** (`CycleCard`) : jour du cycle, phase en cours, estimation des prochaines règles (ou jours de retard), fenêtre fertile et niveau de probabilité estimée si l'option est activée.
- **Mode partenaire** : si l'appareil suit le cycle d'une partenaire, affiche la phase actuelle et des conseils d'accompagnement adaptés.
- **Actions rapides** : *« Mes règles ont commencé »*, *« Noter ma journée »* (`LogScreen`), *« Respirer »* (`BreathingScreen`), *« Partager »* (`ShareScreen`).
- **Carrousel à 3 volets** : conseil sourcé du jour (`DailyTip`), prévisions sur 7 jours (`UpcomingForecastCard`), et courbes hormonales pédagogiques (`PhaseHormonesCard`).
- **Alertes et récapitulatifs** (`AlertCard`, `RecapCard`) : signale les anomalies éventuelles et récapitule le cycle terminé.

### 2. Calendrier (`CalendarScreen`)
- Vue mensuelle navigable par flèches ou balayage tactile (règles, spotting, prévisions en pointillés, indicateurs de saisie).
- Raccourcis vers l'**Analyse des cycles** (`InsightsScreen`) et le **Bilan soignant** (`HealthReportScreen`).

### 3. Comprendre (`LearnScreens`)
- **Schémas anatomiques interactifs** : `CycleVisualGuide`, `HormoneCurvesDiagram`, `InteractiveUterusDiagram`, `VulvaDiagram`.
- **Guides cliniques spécialisés** : Contraception, Endométriose (repères des 5 « D »), SOPK (critères de Rotterdam), Adénomyose, Périménopause (STRAW+10).
- **Fiches documentaires** : douleurs, alimentation, SPM, quand consulter, limites des prévisions.

### 4. Réglages (`SettingsScreen`)
- **Rappels** : alerte avant les règles (0 à 7 j), rappel de notation, rappel pilule/traitement.
- **Mon cycle** : tranche d'âge, affichage fenêtre fertile, suivi symptothermique (glaire + température basale), **mode cycles naturellement irréguliers** (SOPK, périménopause, arrêt de pilule), mise en pause des prévisions (grossesse/allaitement/contraception continue).
- **Apparence et sécurité** : 4 thèmes (*Prune*, *Sauge*, *Océan*, *Terracotta*) et gestion du code PIN.
- **Données** : accès au bilan soignant, export CSV (`lunaison.csv`), **sauvegarde chiffrée (`.pilmil`)**, import de sauvegardes (`.pilmil`, CSV, ZIP chiffré), et suppression totale irréversible.

---

## 3. Écrans spécialisés

- **Saisie « Ma journée » (`LogScreen`)** : flux (*Traces* à *Abondant*), douleur évaluée par son impact fonctionnel, symptômes courants et spécifiques endométriose/périménopause, humeurs, statut pilule, vie intime/libido, médicaments, poids, glaire cervicale, température basale et note libre (500 car.).
- **Bilan de santé pour un soignant (`HealthReportScreen`)** : synthèse 1 page exportable en **PDF** avec cases à cocher pour choisir exactement les rubriques partagées en consultation.
- **Respirer et relâcher (`BreathingScreen`)** : respiration guidée 4 s / 6 s pour la détente pelvienne.
- **Partager (`ShareScreen`)** : messages types ou synchronisation P2P hors-ligne par **QR Code / SMS** (`LUNA1:...`, jusqu'à 6 cycles sans aucune note intime).
