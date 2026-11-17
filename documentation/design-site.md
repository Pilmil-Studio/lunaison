# Diagnostic du site (novembre 2026)

Mesures faites sur les pages locales de `docs/`, en 1280 x 900 et 360 x 800, thèmes clair et sombre.

Constats avant reprise :
1. Les huit `<img>` de la galerie de l'accueil (français et anglais) n'ont ni `width` ni `height` ; les fichiers font 461 x 1000. Avant chargement, chaque `figure` ne contient que sa légende. Les images chargent bien en local (175 x 378 px à 1280, 328 x 709 px à 360).
2. À 360 px la galerie tient sur une colonne : huit captures de 709 px de haut, soit 5 700 px avant la section suivante.
3. Haut de l'accueil : cinq pastilles, puis quatre boutons de trois styles (plein, contour, plein à 60 % d'opacité). Les deux boutons « bientôt » sont des `<span>` à `opacity:.6` : contraste 3,74 (texte blanc sur prune éclairci) et 3,54 (texte prune éclairci sur fond), sous 4,5.
4. En-tête à 360 px : six liens sur quatre lignes, 193 px de haut (accueil français) ; allemand et polonais, trois liens, 103 px. Aucun débordement mesuré (largeur de page 360). Hauteur des liens : 29 px.
5. Largeur de page à 360 px : 367 px sur `sources.html` et `en/evidence.html` (liens longs hors de leur colonne), 450 px sur `galerie_anatomie.html` (grille `minmax(420px, 1fr)`). Les autres pages : 360 px.
6. Longueur de ligne des paragraphes de section de l'accueil : 948 px de large, environ 130 caractères par ligne à 1280 px (cible : 60 à 75). Les paragraphes `.lead` : 73 caractères.
7. Contrastes calculés. Clair : texte 14,59 ; texte atténué 6,87 ; étiquettes orange `.eyebrow` (13,6 px, capitales) 5,34 sur le fond uni, 4,34 au pire du dégradé de fond ; lien 11,02. Sombre : texte 15,0 ; atténué 9,05 ; étiquettes 8,06 ; au pire du dégradé 6,23. Le thème sombre n'a donc pas de défaut ; seule l'étiquette orange du thème clair passe sous 4,5 sur le dégradé.
8. Zones tactiles : liens de navigation 29 px de haut, liens du pied de page 26 px, pastilles non interactives. Les boutons font 48 à 53 px (49 px quand le flex les comprime à 360 px).
9. Focus clavier : seul `.gallery img` a un style de focus ; les liens et boutons gardent le focus par défaut. Le lien d'évitement `.skip` existe. Aucune règle `@media print`.
10. `galerie_anatomie.html` : page à part (style en ligne, fond #121015, pas de `style.css`, pas de `zoom.js`), six images en data URI sans dimensions. Hors du périmètre d'un site commun, seul le débordement à 360 px est corrigé.
