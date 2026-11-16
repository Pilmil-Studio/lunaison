# Sources des « atouts et défis » par phase

## Reformulations appliquées le 16 novembre 2026

L'auteur a accepté toutes les propositions ci-dessous. Elles sont appliquées dans `PhaseHormoneInfo.kt`, dans les six langues (fr, en, pl, uk, es, de), et la colonne « Statut » de chaque tableau indique le sort de chaque ligne. Compléments :

- Deux libellés de niveau hormonal ont été ajoutés à `HormoneLevel` : « Variable » et « Élevé puis en baisse ». Les types de données ne changent pas.
- Phase du retard : les quatre niveaux affichent « Variable » plutôt que d'être retirés, car `HormoneSnapshot` exige un niveau pour chaque hormone.
- Phase folliculaire : le retrait des deux défis vidait la liste. Une puce neutre renvoie aux notes personnelles.
- Le titre de la phase des règles a été ramené à « Règles », comme les autres titres (section « Titres et libellés »).
- Les libellés de `HomeScreen.kt` ne sont pas modifiés (hors périmètre).
- Deux affirmations sont retirées faute de source ouverte : rétention d'eau et seins tendus (FAQ de l'ACOG non ouverte), caféine et alcool.

Ce document examine chaque affirmation de `core/content/.../PhaseHormoneInfo.kt` (carte « Hormones et dynamique du corps » de l'accueil) et la compare à des sources ouvertes pendant l'audit de novembre 2026. Pour chaque affirmation non étayée, une reformulation prudente ou un retrait était proposé ; ces propositions ont ensuite été appliquées (voir ci-dessus).

`PhaseGuides.kt` (les conseils du jour) a été lu aussi. Ses conseils citent déjà une source de `Sources.kt` ou sont marqués `withoutStudy`, et son commentaire de tête dit que « adapter tout son quotidien à chaque phase ne repose pas sur des études solides ». `PhaseHormoneInfo.kt` va à l'inverse de ce principe : titres comme « regain d'élan et clarté », « rayonnement », « cocooning », rubrique « Forces et atouts de cette phase ». Les points qui concernent `PhaseGuides.kt` sont en fin de document.

## Niveaux de preuve utilisés

- **Référence** : manuel ou chapitre de physiologie, consensus établi.
- **Solide** : revue systématique ou méta-analyse dont les conclusions sont claires.
- **Faible** : revue systématique ou méta-analyse dont les auteurs jugent eux-mêmes la qualité basse ou les résultats incohérents.
- **Information grand public** : page d'un organisme de santé, sans niveau de preuve explicite.
- **Non étayé** : aucune source ouverte pendant l'audit ne le dit.
- **Contredit** : les sources ouvertes disent plutôt le contraire.

## Sources ouvertes pendant l'audit

Titres, organismes et années ; les auteurs ne sont pas nommés. Chaque source a été ouverte (page ou résumé) pendant la mission. Pour les revues, seul le résumé a été lu, pas le texte complet.

| Code | Source | Année | URL |
|---|---|---|---|
| S1 | The Normal Menstrual Cycle and the Control of Ovulation, Endotext (manuel d'endocrinologie en ligne) | 2018 | https://endotext.org/wp-content/uploads/pdfs/the-normal-menstrual-cycle-and-the-control-of-ovulation.pdf |
| S2 | Timing of sexual intercourse in relation to ovulation: effects on the probability of conception, survival of the pregnancy, and sex of the baby, New England Journal of Medicine 333(23) | 1995 | https://doi.org/10.1056/NEJM199512073332301 |
| S3 | Menstrual cycle effects on cognitive performance: a meta-analysis, PLOS ONE 20(3) | 2025 | https://doi.org/10.1371/journal.pone.0318576 |
| S4 | The effects of menstrual cycle phase on exercise performance in eumenorrheic women: a systematic review and meta-analysis, Sports Medicine | 2020 | https://doi.org/10.1007/s40279-020-01319-3 |
| S5 | Current evidence shows no influence of women's menstrual cycle phase on acute strength performance or adaptations to resistance exercise training, Frontiers in Sports and Active Living 5 (revue de revues) | 2023 | https://frontiersin.org/articles/10.3389/fspor.2023.1054542/full |
| S6 | Effect of menstrual cycle on resting metabolism: a systematic review and meta-analysis, PLoS ONE 15(7) e0236025 | 2020 | https://doi.org/10.1371/journal.pone.0236025 |
| S7 | The effect of the menstrual cycle on energy intake: a systematic review and meta-analysis, Nutrition Reviews 83(3) | 2025 | https://academic.oup.com/nutritionreviews/article/83/3/e866/7713894 |
| S8 | Women's estrus and extended sexuality: reflections on empirical patterns and fundamental theoretical issues, Frontiers in Psychology 13 (revue narrative) | 2022 | https://doi.org/10.3389/fpsyg.2022.900737 |
| S9 | Physiological changes in women's skin during the menstrual cycle: a scoping review, Cureus 16(12) | 2024 | https://doi.org/10.7759/cureus.75286 |
| S10 | Menstrual cycle effects on sleep, Sleep Medicine Clinics 17(2) | 2022 | https://www.sri.com/publication/biosciences-health-pubs/menstrual-cycle-effects-on-sleep/ |
| S11 | Is trait rumination associated with affective reactivity to the menstrual cycle? A prospective analysis, Cambridge University Press (en ligne le 29 janvier 2024) | 2024 | https://core-prod.cambridgecore.org/core/product/92CDAD8086D832A002DF911A650D4426/core-reader |
| S12 | Instituts de recherche en santé du Canada, Institut de la santé des femmes et des hommes, lettre d'information sur le syndrome prémenstruel (« mythbuster »), n° 3 | 2014 | https://cihr-irsc.gc.ca/e/documents/igh_mythbuster_pms-en.pdf |
| S13 | Manuel MSD (version professionnelle), Dysmenorrhea, revu en juin 2026 | 2026 | https://www.msdmanuals.com/professional/gynecology-and-obstetrics/menstrual-abnormalities/dysmenorrhea |
| S14 | What we know about primary dysmenorrhea today: a critical review, Human Reproduction Update 21(6) | 2015 | https://doi.org/10.1093/humupd/dmv039 |
| S15 | Cleveland Clinic, Mittelschmerz, revu en février 2023 | 2023 | https://my.clevelandclinic.org/health/diseases/9134-mittelschmerz |
| S16 | NHS, Doing a pregnancy test | revu 2022 | https://www.nhs.uk/pregnancy/trying-for-a-baby/doing-a-pregnancy-test/ |
| S17 | NHS 111 Pays de Galles, Pregnancy guide, quand faire un test | non datée | https://111.wales.nhs.uk/livewell/pregnancy/WhenTest/ |
| S18 | NHS, Missed or late periods, revu en juin 2026 | 2026 | https://www.nhs.uk/symptoms/missed-or-late-periods/ |
| S19 | The ObG Project, résumé de la ACOG Clinical Practice Guideline n° 7, Management of Premenstrual Disorders | 2023 | https://www.obgproject.com/2023/12/27/acog-guideline-management-of-premenstrual-syndrome-and-premenstrual-dysphoric-disorder/ |

Limites : S19 est un résumé de la recommandation de l'ACOG, pas la recommandation elle-même. S12 est une lettre d'information, pas une étude. La page S16 porte une date de revue dépassée (février 2022, prochaine revue prévue en 2025), à l'inverse de S17 qui dit la même chose. Les pages de l'ACOG (FAQ sur le syndrome prémenstruel et sur les règles douloureuses), de PubMed, de PMC et de l'EFSA n'ont pas pu être ouvertes pendant la mission.

## Ce que disent les sources sur les trois grandes questions

**Cognition et humeur.** La méta-analyse la plus récente (S3, 102 articles, 3 943 participantes, sept domaines dont l'attention, les fonctions exécutives, la créativité et le verbal) ne trouve aucune preuve robuste d'une variation de performance selon la phase. Deux résultats sur les aptitudes spatiales sont significatifs mais fragiles et ne tiennent pas avec les méthodes les plus rigoureuses de repérage de la phase. Pour l'humeur, la lettre S12 rapporte que 15 % seulement des études d'une revue montrent un lien exclusif entre phase prémenstruelle et humeur négative, et que dans une cohorte de plus de cent femmes la santé physique, le stress perçu et le soutien social prédisent l'humeur bien mieux que la phase. S11 note que seule une minorité de personnes a des variations d'humeur cliniquement significatives au fil du cycle, avec une grande variabilité d'une personne à l'autre.

**Performance physique.** S4 (78 études, 1 193 participantes) trouve une performance très légèrement plus basse en début de phase folliculaire, avec un effet trivial (taille d'effet autour de moins 0,06) qui disparaît quand on ne garde que les études de bonne qualité. La qualité globale est jugée basse et les auteurs concluent qu'on ne peut pas formuler de recommandation générale : ils préconisent une approche personnalisée. S5 va plus loin sur la force et l'entraînement en résistance : l'entraînement organisé par phase n'a pas de base probante.

**Désir.** S8 (revue narrative) décrit un désir sexuel déclaré en moyenne plus élevé autour de l'ovulation que dans la phase lutéale, c'est le motif le plus constant de la littérature, avec des études qui ne le retrouvent pas et une mesure surtout subjective.

## Phase par phase

### Règles (MENSTRUAL)

| Affirmation (fr) | Source | Niveau | Proposition | Statut |
|---|---|---|---|---|
| Résumé : hormones au plancher, « déclenchant le détachement de la muqueuse » | S1 : la chute de la progestérone à la fin de la phase lutéale entraîne la contraction des artérioles spiralées, l'ischémie et la libération de prostaglandines, d'où la desquamation | Référence | Garder l'idée, corriger la causalité : « La baisse de la progestérone en fin de cycle entraîne le détachement de la muqueuse utérine. » | Appliqué |
| Niveaux : œstrogènes bas, progestérone basse, LH basse, FSH en hausse | S1 : FSH élevée en début de phase folliculaire puis en baisse ; LH basse avec pulses ; œstradiol bas puis en hausse ; progestérone très basse | Référence | Garder, remplacer « En hausse » de la FSH par « Relativement élevée au début » si le libellé de `HormoneLevel` le permet, sinon garder tel quel (simplification acceptable) | Non modifié (simplification gardée) |
| « Soulagement immédiat du syndrome prémenstruel et des tensions nerveuses » | Aucune source ouverte ne dit cela. S12 et S11 : les variations d'humeur liées au cycle ne concernent qu'une minorité | Non étayé | Retirer | Retiré |
| « Tendance naturelle au calme, au recentrage et au lâcher-prise » | Aucune | Non étayé | Retirer, ou : « Certaines personnes apprécient de ralentir pendant leurs règles ; d'autres ne ressentent aucun besoin de le faire. » (formulation sans prétention factuelle) | Reformulé (variante prudente) |
| « Nouveau départ biologique : le corps commence déjà à préparer le cycle suivant » | S1 : début du cycle, FSH élevée, recrutement d'une cohorte de follicules | Référence, mais formulation imagée | « Le premier jour des règles marque le début d'un nouveau cycle : de nouveaux follicules commencent à être recrutés dans les ovaires. » | Reformulé |
| « Crampes utérines déclenchées par les prostaglandines » | S13 : douleur attribuée à des contractions de l'utérus avec baisse de l'apport sanguin, les prostaglandines (surtout la PGF2 alpha) étant considérées comme le moteur | Référence clinique, mécanisme décrit comme « supposé » | « Les crampes viennent de contractions de l'utérus, que l'on pense liées aux prostaglandines. » | Reformulé |
| « Baisse d'énergie physique » | S4 : écart trivial et incertain de la performance en début de phase folliculaire. S14 : la fatigue et le mauvais sommeil concernent les personnes ayant une dysménorrhée | Faible, contredit en moyenne | « La fatigue est fréquente chez certaines personnes, surtout en cas de règles douloureuses ou abondantes. » (le lien avec la dysménorrhée vient de S14 ; la fréquence n'est pas chiffrée) | Reformulé (même puce que le fer) |
| « Perte modérée de fer via les saignements » | Référence existante `Sources.EFSA_IRON_2015` (apport de référence en fer avant la ménopause), non rouverte pendant l'audit | À revérifier | Garder à condition de rouvrir la source, ou retirer « modérée » (elle dépend du volume des règles) | Appliqué : « modérée » retiré |
| « Sensibilité digestive ou petits maux de tête possibles les premiers jours » | Aucune source ouverte | Non étayé | Retirer | Retiré |
| Soutien : chaleur locale « pour relâcher les fibres musculaires de l'utérus » | S13 : la chaleur sur le bas du ventre peut soulager. Référence existante `Sources.HEAT_2018`, non rouverte ; `sources.md` la décrit comme à preuves limitées | Faible à moyenne pour l'effet ; mécanisme non étayé | « Une bouillotte sur le bas du ventre peut soulager la douleur. » (retirer le mécanisme) | Reformulé |
| Soutien : aliments riches en fer avec vitamine C | Aucune source ouverte pour l'association avec la vitamine C (la fiche de l'EFSA du dépôt porte sur l'apport de référence) | Non étayé tel quel | Retirer la mention de la vitamine C, ou la sourcer avant (fiche de référence sur le fer de l'OMS, de l'EFSA ou des NIH, à ouvrir) | Appliqué : mention de la vitamine C retirée |
| Soutien : « alléger ses journées sans culpabilité » | Conseil de bon sens | Sans prétention factuelle | Garder | Non modifié |

### Phase folliculaire (FOLLICULAR)

| Affirmation (fr) | Source | Niveau | Proposition | Statut |
|---|---|---|---|---|
| Titre « regain d'élan et clarté » | S3 (pas de variation robuste de la cognition), S4 | Contredit | « Phase folliculaire » seule, ou « Phase folliculaire : un follicule mûrit » | Appliqué |
| Résumé : FSH stimule les follicules, l'un mûrit et produit des œstrogènes en hausse | S1 | Référence | Garder | Non modifié |
| Niveaux : œstrogènes en hausse, progestérone basse, LH basse, FSH dominante | S1 : FSH élevée au début puis en baisse, œstradiol en hausse | Référence pour trois niveaux sur quatre ; « dominante » pour la FSH est discutable | Remplacer « Dominant » par « Élevé puis en baisse » ou « Modéré » | Appliqué : FSH « Élevé puis en baisse » (nouveau libellé) |
| « Regain net d'énergie physique, de vitalité et d'endurance musculaire » | S4 : performance très légèrement plus basse en début de phase, effet trivial, qualité basse. S5 : pas d'influence démontrée sur la force | Contredit | « Certaines personnes se sentent plus en forme dans cette phase, d'autres ne remarquent aucune différence. Les études ne montrent pas de différence nette de performance. » | Reformulé |
| « Hausse de la motivation, curiosité et facilité pour démarrer de nouveaux projets » | S3 : aucune variation robuste ; S12 : les hormones contribuent peu à l'humeur | Non étayé | Retirer | Retiré |
| « Peau souvent plus lumineuse grâce à la stimulation du collagène par les œstrogènes » | S9 : hydratation sans changement notable, élasticité plus haute à l'ovulation qu'en phase folliculaire ; rien sur la luminosité ni sur le collagène | Non étayé, partiellement contredit | Retirer | Retiré |
| « Transition parfois progressive les premiers jours après la fin des règles » | Aucune | Non étayé | Retirer | Retiré |
| « Risque d'en faire trop d'un coup par enthousiasme débordant » | Aucune | Non étayé | Retirer | Retiré (liste vidée : puce neutre ajoutée) |
| Soutien : « Idéal pour planifier des entraînements plus intenses ou progressifs » | S4 : pas de recommandation générale possible, approche personnalisée. S5 : l'entraînement par phase n'est pas fondé sur des preuves | Contredit | « Si cela vous intéresse, notez comment vous vous sentez à l'effort au fil de vos cycles : les études ne permettent pas de recommandation valable pour tout le monde. » | Reformulé |
| Soutien : « Profiter de la clarté cognitive pour les tâches d'apprentissage » | S3 | Contredit | Retirer | Retiré |

### Autour de l'ovulation (OVULATION)

| Affirmation (fr) | Source | Niveau | Proposition | Statut |
|---|---|---|---|---|
| Titre « fertilité et rayonnement » | « Rayonnement » : aucune source | Non étayé | « Autour de l'ovulation » | Appliqué |
| Résumé : pic de LH, rupture du follicule, ovocyte libéré dans la trompe | S1 : le pic de LH commence environ 34 à 36 heures avant l'ovulation | Référence | Garder | Non modifié |
| Niveaux : œstrogènes au pic, progestérone en hausse, LH et FSH au pic | S1 : l'œstradiol atteint un pic puis chute juste avant le pic de LH ; la FSH monte à mi-cycle ; la progestérone commence à monter autour du pic de LH | Référence | Garder (« pic maximal » pour les œstrogènes est une simplification : l'œstradiol redescend juste avant le pic de LH) | Non modifié |
| « Pic de sociabilité, de confiance en soi et d'aisance relationnelle » | S8 ne couvre que le désir et l'intérêt sexuels, pas la sociabilité | Non étayé | Retirer | Retiré |
| « Libido et désir généralement au plus haut du cycle » | S8 : désir déclaré plus élevé en moyenne autour de l'ovulation, motif jugé constant, avec des études discordantes et une mesure surtout subjective | Faible à moyen (revue narrative) | « En moyenne, le désir sexuel déclaré est un peu plus élevé autour de l'ovulation, mais beaucoup de personnes ne le constatent pas. » | Reformulé |
| « Force musculaire et coordination motrice au maximum » | S4, S5 | Contredit | Retirer | Retiré |
| « Tiraillements pelviens d'un côté (douleur ovarienne d'ovulation) » | S15 : douleur de mi-cycle, d'un seul côté du bas-ventre, durant quelques heures et jusqu'à 48 heures, pouvant toucher jusqu'à 40 % des personnes qui ovulent ; en général sans gravité | Information grand public | « Une gêne d'un côté du bas-ventre à mi-cycle (douleur d'ovulation) touche jusqu'à 40 % des personnes qui ovulent, en général pendant quelques heures. » | Reformulé |
| « Glaire filante et abondante, sensation d'humidité intime marquée » | Aucune source ouverte pendant l'audit | Non étayé ici (observation courante, mais à sourcer) | Garder seulement après avoir ajouté une source ; sinon : « Certaines personnes remarquent des pertes plus fluides et filantes. » (déjà la formulation de `PhaseGuides.kt`) | Reformulé (formulation de `PhaseGuides.kt`) |
| « Fenêtre fertile maximale : attention contraceptive stricte si pas de désir d'enfant » | S2 : une grossesse n'est survenue qu'après des rapports dans une période de six jours se terminant le jour de l'ovulation, la probabilité passant d'environ 0,10 cinq jours avant à environ 0,33 le jour même ; le nombre de jours fertiles réel reste incertain | Cohorte prospective ancienne, solide pour la durée de la fenêtre. Mais l'appli ne localise pas l'ovulation avec certitude (référence sur l'échec des méthodes fondées sur le calendrier, déjà dans `Sources.kt`) | « La période où une grossesse est possible dure environ six jours autour de l'ovulation, mais l'appli ne peut pas la situer avec certitude. Elle ne remplace pas une contraception. » (aligné sur la mention déjà présente à l'accueil) | Reformulé |
| Soutien : noter la glaire pour repérer son propre schéma | Conseil d'usage de l'appli | Sans prétention factuelle | Garder | Non modifié |
| Soutien : « les spermatozoïdes peuvent attendre l'ovocyte jusqu'à 5 jours » | S2 : cohérent, les rapports cinq jours avant l'ovulation restent féconds (probabilité d'environ 0,10) | Solide pour cet ordre de grandeur | Garder, en disant « jusqu'à environ 5 jours » | Appliqué : « jusqu'à environ 5 jours » |

### Phase lutéale (LUTEAL)

| Affirmation (fr) | Source | Niveau | Proposition | Statut |
|---|---|---|---|---|
| Titre « cocooning et vigilance » | Aucune | Non étayé | « Phase lutéale » | Appliqué |
| Résumé : le corps jaune sécrète la progestérone qui épaissit la muqueuse, puis elle chute | S1 | Référence | Garder | Non modifié |
| Niveaux : œstrogènes « en baisse », progestérone dominante, LH et FSH basses | S1 : l'œstradiol chute après l'ovulation puis remonte en milieu de phase lutéale, parallèlement à la progestérone | Référence pour la progestérone ; œstrogènes inexacts | Remplacer « En baisse » par « Variable » (baisse puis second pic à mi-phase, puis baisse) | Appliqué : œstrogènes « Variable » (nouveau libellé) |
| « Progestérone calmante en première partie : sommeil plus profond et détente » | S10 : la qualité du sommeil déclarée se dégrade chez certaines personnes en période prémenstruelle ; rien sur un sommeil plus profond | Non étayé, plutôt contredit | Retirer | Retiré |
| « Sens du détail aiguisé, propice au tri, à l'organisation et aux finitions » | S3 : aucune variation cognitive robuste | Non étayé | Retirer | Retiré |
| « Métabolisme basal légèrement accru » | S6 : effet petit mais significatif d'une dépense au repos plus élevée en phase lutéale (taille d'effet 0,33, intervalle de confiance de 0,17 à 0,49, 26 études), mais plus petit et non significatif dans les études publiées depuis 2000 (0,23, de moins 0,00 à 0,47), risque de biais notable | Faible | « Certaines études mesurent une dépense d'énergie au repos un peu plus élevée en phase lutéale. L'écart est petit et incertain. » | Reformulé |
| « Baisse de sérotonine en fin de phase : hypersensibilité émotionnelle ou irritabilité » | Aucune source ouverte pour la sérotonine. S11 et S12 : seule une minorité de personnes a des changements d'humeur marqués avant les règles | Non étayé pour le mécanisme ; l'effet est minoritaire | « Une minorité de personnes ressent des changements d'humeur marqués avant les règles ; beaucoup n'en ressentent pas. Si cela vous pèse, parlez-en à un professionnel de santé. » (sans mécanisme) | Reformulé |
| « Rétention d'eau, sensation de gonflement abdominal et seins tendus » | Aucune source ouverte (la FAQ de l'ACOG n'a pas pu être ouverte) | Non étayé ici | Garder avec « chez certaines personnes » seulement après ouverture de la FAQ de l'ACOG sur le syndrome prémenstruel ; sinon retirer | Retiré (FAQ de l'ACOG non ouverte) |
| « Fringales glucidiques (réflexe naturel du cerveau face au déficit de sérotonine) » | S7 : apport énergétique plus élevé en phase lutéale (écart standardisé de 0,69, environ 168 kcal par jour, 15 jeux de données, 330 participantes), hétérogénéité forte (I2 de 83 %), preuves faibles à modérées, incohérence entre les p de deux sections. Rien sur les glucides en particulier, rien sur la sérotonine | Faible ; mécanisme non étayé | « L'appétit est parfois plus marqué en phase lutéale : une méta-analyse trouve un apport énergétique un peu plus élevé, avec des preuves limitées. » | Reformulé |
| Soutien : « magnésium et calcium pour calmer les tensions neuromusculaires » | S19 : calcium, recommandation conditionnelle, preuves de faible qualité. Référence existante `Sources.CALCIUM_PMS_1998`. Magnésium : aucune source ouverte. Mécanisme « neuromusculaire » : aucune | Faible pour le calcium ; non étayé pour le magnésium | « Un apport suffisant en calcium peut atténuer les symptômes prémenstruels chez certaines personnes (preuves limitées). » Retirer le magnésium et le mécanisme | Reformulé (magnésium et mécanisme retirés) |
| Soutien : « modérer caféine et alcool qui accentuent l'anxiété prémenstruelle » | Aucune source ouverte (S19 n'en parle pas) | Non étayé | Retirer | Retiré |
| Soutien : « activités douces (marche, étirements, respiration 4-6) » | S19 : exercice régulier (aérobie, yoga, pilates), recommandation conditionnelle, preuves de faible qualité. « Respiration 4-6 » : aucune source | Faible pour l'exercice ; non étayé pour la respiration 4-6 | « Une activité physique régulière et modérée peut aider (preuves limitées). » Retirer « respiration 4-6 », ou le garder comme proposition de l'appli (l'appli a un écran de respiration) sans le présenter comme un soin | Reformulé (respiration 4-6 retirée) |

### Retard de règles (LATE)

| Affirmation (fr) | Source | Niveau | Proposition | Statut |
|---|---|---|---|---|
| Titre « écoute et bienveillance » | Aucune | Sans prétention factuelle | Garder ou « Retard de règles » | Non modifié (titre gardé) |
| Résumé fr : phase lutéale étirée ou ovulation décalée « par un facteur de vie ou de santé » (la version anglaise dit « stress, travel, or pregnancy ») | S18 : grossesse, stress, perte de poids rapide ou surpoids, sport intense, contraception hormonale, allaitement, périménopause, syndrome des ovaires polykystiques, maladies de la thyroïde, variation naturelle de la durée du cycle | Information grand public (NHS, revue en 2026) | Aligner les six langues sur « Plusieurs causes sont possibles : grossesse, stress, changement de poids, sport intense, périménopause, contraception hormonale, allaitement ou variation naturelle du cycle. » | Appliqué dans les six langues |
| Niveaux : les quatre hormones « basses » | Aucune source. Faux en cas de grossesse (progestérone et hCG élevées) | Non étayé, inexact | Retirer les niveaux pour cette phase (ou afficher « Variable ») | Appliqué : « Variable » pour les quatre hormones (structure gardée) |
| « Occasion d'écouter les signaux récents de fatigue, de stress ou de voyage » | S18 cite le stress parmi les causes ; le voyage n'y figure pas | Partiellement étayé | « Stress, changement de poids ou sport intense peuvent retarder les règles. » (retirer « voyage », absent de S18) | Reformulé |
| « Inquiétude ou incertitude liée à l'attente des règles » | Sans prétention factuelle | | Garder | Non modifié |
| « Un test urinaire est fiable dès le premier jour de retard présumé » | S16, S17 : la plupart des tests peuvent être faits dès le premier jour de retard ; un résultat positif est presque certainement exact, un résultat négatif moins fiable, à refaire après quelques jours | Information institutionnelle | « Un test urinaire peut être fait dès le premier jour de retard. Un résultat positif est presque certain ; un résultat négatif est moins fiable, refaites le test après quelques jours. » | Reformulé |
| « Si une pause est souhaitée (grossesse ou contraception), le mode pause est disponible » | Fonction de l'appli | Sans objet | Garder (la version anglaise dit « pause from forecasts », à harmoniser) | Non modifié (version anglaise harmonisée) |
| (absent) | S18 : voir un médecin si trois règles de suite sont manquées | Information institutionnelle | Ajouter : « Sans règles depuis trois mois, demandez un avis médical. » (déjà dans `PhaseGuides.kt`) | Ajouté |

## Titres et libellés de l'interface à revoir avec le fichier

- Titres de phase : « Règles : réinitialisation et repos », « Phase folliculaire : regain d'élan et clarté », « Autour de l'ovulation : fertilité et rayonnement », « Phase lutéale : cocooning et vigilance ». Proposition : retirer la partie après les deux-points, ou la remplacer par une description factuelle (« un follicule mûrit », « un ovocyte est libéré », etc.).
- Dans `HomeScreen.kt`, les libellés « Forces et atouts de cette phase », « Défis et ce qui peut gêner », « Ce qui vous soutient » et le bouton « Atouts et conseils » présupposent que chaque phase a des forces. Proposition : « Ce que certaines personnes observent » et « Ce qui peut aider ». Ce fichier est du code d'interface, il n'est pas dans le périmètre de cette mission.
- La personnalisation que recommandent S3 et S4 existe déjà dans l'appli : `PhasePatterns` montre ce que la personne a noté aux mêmes phases lors de ses cycles précédents. Une phrase renvoyant à cette carte (« Pour savoir ce qui vous concerne, regardez ce que vous avez noté ») serait plus juste qu'une liste générale d'atouts.

## Points sur `PhaseGuides.kt` (lu, non modifié)

- Conseil sur la chaleur : « soulage autant qu'un anti-inflammatoire dans les études ». `sources.md` la décrit comme à preuves limitées. Proposer « peut soulager ». Source `HEAT_2018` non rouverte pendant l'audit ; S13 appuie seulement que la chaleur peut soulager.
- Conseil sur le fer : cite `EFSA_IRON_2015` pour l'association avec la vitamine C et pour les repas riches en fer ; `sources.md` dit que la référence porte sur l'apport de 16 mg par jour. À vérifier.
- Conseil de retard : « un test urinaire est en général fiable dès maintenant ». Ajouter la nuance sur le résultat négatif (S16, S17).
- Conseil de l'ovulation sur le calendrier qui n'évite pas une grossesse : cohérent avec S2 pour la durée de la fenêtre, et avec la référence sur les méthodes de calendrier déjà dans `Sources.kt`. Rien à changer.

## Résumé

Sur 53 lignes (affirmations, niveaux ou libellés) de `PhaseHormoneInfo.kt` : les niveaux et résumés hormonaux sont en grande partie corrects (référence S1) avec trois imprécisions (FSH « dominante », œstrogènes « en baisse » en phase lutéale, hormones « basses » en cas de retard) ; une dizaine d'affirmations sur l'énergie, la cognition, l'humeur, la sociabilité, la force et le sommeil sont non étayées ou contredites par les méta-analyses S3, S4, S5 et S10 ; quelques affirmations sont étayées avec réserves (désir, dépense énergétique, douleur d'ovulation, appétit, calcium). La proposition d'ensemble est de ne garder que ce qui relève de la physiologie, de reformuler les observations vécues en « certaines personnes » et de renvoyer vers les notes personnelles de l'utilisatrice.
