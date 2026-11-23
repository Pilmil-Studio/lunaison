# Pistes et réalisations

## Importer l'historique depuis d'autres applications

- **Réalisé en v1.4.0** : import local sans serveur depuis l'application *Calendrier des règles* (Simple Design Ltd), compatible avec les archives ZIP chiffrées par mot de passe (ZipCrypto) et les fichiers JSON bruts, ainsi qu'import/export CSV Lunaison.
- **Réalisé, Health Connect (Android)** : lecture seule des règles écrites par d'autres applis (`MenstruationPeriodRecord`, `MenstruationFlowRecord`, permission `READ_MENSTRUATION` uniquement), lancée uniquement depuis l'écran d'import. Gère Health Connect absent ou à mettre à jour, permission refusée et absence de données. Sans la permission d'historique de Health Connect, seuls les 30 jours qui précèdent le premier accord de la permission sont lisibles.
- **Réalisé, export Clue (expérimental)** : archive ZIP protégée par ZipCrypto, fichier `.json` ou ancienne sauvegarde `.cluedata`. Le schéma de l'export n'étant pas documenté, la lecture est tolérante (`ClueImporter`) et toutes les hypothèses sur les noms de clés sont réunies dans `ClueKeyMapping`. Une archive chiffrée en AES est refusée avec le conseil de la décompresser puis d'importer le fichier JSON, pour ne pas ajouter de dépendance.
- **Aperçu et confirmation, pour tous les imports** : Calendrier des règles (ZIP chiffré ou non, JSON), CSV Lunaison, Health Connect et Clue montrent d'abord le nombre de journées et de cycles et la période couverte, puis fusionnent sans jamais écraser une journée déjà notée dans Lunaison. Les fonctions de lecture (`previewAuto`, `previewLunaisonCsv`, `previewPeriodCalendarZip`...) ne font que produire un `ImportPreview` ; seule `applyImport` écrit, après confirmation, en recalculant la fusion. La situation de départ du Calendrier ne complète que les champs encore vides, à la confirmation. La restauration d'une sauvegarde `.pilmil` est la seule opération qui remplace les données, après une confirmation explicite.
- **Pistes futures** : HealthKit sur iOS (l'interface commune `HealthDataSource` est prête, l'implémentation iOS renvoie « non disponible »), puis d'autres formats d'export (Flo, Drip) si des spécifications d'export exploitables sont disponibles.

**Principes respectés :**

- import facultatif, lancé par la personne, depuis un fichier qu'elle choisit (sélecteur de documents Android, sans autorisation de stockage) ou, pour Health Connect, depuis l'écran d'import après son accord ;
- traitement 100 % local sur l'appareil, sans connexion ni envoi vers aucun serveur ;
- ce qui n'a pas d'équivalent dans Lunaison est ignoré et signalé, jamais inventé.
