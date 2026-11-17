# Registre des formules et règles de calcul

Chaque entrée indique : formule, unités, source, hypothèses, limites et **statut** :

* **DOCUMENTÉ** : méthode ou valeur tirée d'une source identifiée (valeur reste paramétrable).
* **CHOIX DE CONCEPTION** : décision de construction propre à l'atelier (pas une « norme »).
* **À VALIDER** : pratique empirique non retrouvée dans une source fiable. Elle est implémentée comme règle
  **configurable** (table `manufacturing_rule`, statut `TO_VALIDATE`) et le devis affiche un avertissement tant
  qu'elle est utilisée. Aucune de ces valeurs n'est une norme universelle.

Les recherches datent d'octobre 2026. Les liens sont donnés pour que le bureau d'études puisse relire les sources.

---

## F1. Plan de débit (panneaux nécessaires, pertes)

* **Méthode** : placement « par niveaux » (strip/level packing) à coupes guillotine, tri par hauteur décroissante,
  meilleur ajustement dans les bandes ouvertes, rotation libre si le décor n'impose pas le fil, essai des deux
  sens de bande, retenue du meilleur résultat. Implémenté dans `CutPlanService`.
* **Source** : famille d'heuristiques FFDH/BFDH du *Two-Dimensional Bin Packing Problem* avec contrainte guillotine :
  Lodi, Martello, Monaci, Vigo, « Two-dimensional bin packing problems » (https://cris.unibo.it/handle/11585/92372) ;
  Pietrobuoni, *Two-Dimensional Bin Packing Problem with Guillotine Restrictions*, thèse Univ. Bologne
  (https://amsdottorato.unibo.it/id/eprint/6810/1/PhD_Pietrobuoni.pdf) ; panorama du problème de découpe :
  Cheng, Cheng, Feiring, *Int. J. Production Economics* 36(3), 1994.
* **Unités** : mm ; surfaces en m² ; perte % = 1 − surface nette / surface des feuilles consommées.
* **Hypothèses** : coupes de bord à bord (guillotine), pièces rectangulaires, chutes non réutilisées.
* **Limites** : heuristique, pas un optimum : le nombre de feuilles est une **borne supérieure** et peut dépasser
  celui d'un optimiseur industriel (en général de quelques %). Nombre de coupes = bandes + pièces (estimation).
  À comparer avec le logiciel de débit de l'atelier sur des cas réels (cf. `VALIDATION.md`). Statut : **DOCUMENTÉ** (méthode), résultat **à valider**.
* **Borne vérifiée par test** : nombre de feuilles ≥ ⌈surface nette / surface utile⌉.

## F2. Trait de scie (kerf)

* **Formule** : une pièce de dimension *w* consomme *w* + kerf le long de la bande ; une bande de hauteur *h* consomme *h* + kerf.
  Même principe que le calcul de colonnes ⌊L / (pièce + kerf)⌋ des calculateurs de débit (https://hacecuentas.com/en/melamine-board-cutting-calculator).
* **Valeur par défaut** : 3,2 mm (lames de format pour mélaminé, ex. FS Tool 3,2 mm : https://eoasaw.com/?p=34163) ;
  scies à format : 4,4 mm (Freud LSB40012X : https://www.burnstools.com/400mm-x-4-4mm-x-80mm-72t-tcg-lsbx-series-frulsb40012x).
  La distinction « épaisseur de lame » / « saw kerf » (matière enlevée) est celle de MaxCut (https://knowledge.maxcutsoftware.com/doc/saw-kerf-settings).
* **Limite** : dépend de la lame et de la machine de l'atelier → règle `sheet.kerf_mm`. Statut : **DOCUMENTÉ** (ordre de grandeur), valeur à confirmer.

## F3. Parage des feuilles (trim)

* Les optimiseurs industriels prévoient un parage de bord avant débit (concept présent dans les paramètres Planit).
  Valeur propre à la machine et à la qualité des chants de feuille → `sheet.trim_mm` (10 mm par défaut). Statut : **À VALIDER**.

## F4. Longueurs de chants et dimensions de débit

* **Dimension de débit** : un chant d'épaisseur *e* collé sur un bord de longueur *a* réduit la dimension *b* du panneau brut
  de *e* (et inversement) : `coreB = b − e·aEdges`, `coreA = a − e·bEdges`. Principe documenté par Promob :
  « dimension de pièce − épaisseur du chant » (https://suporte.promob.com/hc/en-us/articles/360045177013). **Vérifié par test.**
* **Longueur nette** : Σ quantité × (aEdges·a + bEdges·b).
* **Surlongueur** : on ajoute une valeur par extrémité de bande (2 × `edge.trim_offset_mm` par bande) ; même principe que le
  « lateral edge offset » de Promob (même source) et que la « overlength » des nomenclatures (forum TopSolid :
  https://forum.topsolid.com/viewtopic.php?p=5473). La valeur dépend de la plaqueuse. **À VALIDER**.
* **Perte de chant** : 10 % par défaut ; les calculateurs publics citent 10 à 15 % (source secondaire :
  https://woodworkingadvisor.com/edge-banding-calculator-how-much-you-need-for-jobs/). **À VALIDER** sur la consommation réelle de rouleaux.
* **Quels bords sont plaqués** : choix de conception (`edge.<rôle>.a_edges / b_edges`).

## F5. Nombre de charnières

* **Source** : Blum (CLIP top BLUMOTION, https://ea.blum.com/en/number-of-hinges/) : 2 charnières pour 4–6 kg, 3 pour 6–12 kg,
  valeurs valables jusqu'à 600 mm de large ; le poids inclut les poignées ; portes hautes et étroites : 3 charnières recommandées.
  Hettich (Selekta, guide seulement) : nombre indicatif, essai de montage recommandé, écartement des 2 charnières ≥ largeur de porte
  (https://hta.hettich.com/fileadmin/Hettich_Technical_Assistant/PDFs/350199_Selekta_Pro_Verarbeitung_en.pdf).
* **Implémentation** : `max(min_par_porte, table_hauteur(h), table_poids(kg))`, poids = h × l × épaisseur × densité.
  La table par poids est celle de Blum. La table par hauteur fournie est **provisoire** (valeurs de charnières de portes
  d'ASSA ABLOY, https://assaabloy.com/my/en/solutions/products/mechanical-hardware/hinge/door-hinge-fire-rated/ah2bb-ball-bearing-hinge)
  et **doit être remplacée par l'abaque du fabricant de charnières à cuvette utilisé**. Au-delà des tables : dernière ligne + avertissement.
* **Limites** : les fabricants précisent que ces nombres sont indicatifs (matériau, largeur, usage). Statut : poids **DOCUMENTÉ**, hauteur **À VALIDER**.

## F6. Flèche des étagères (contrôle, pas une règle de fabrication)

* **Formule** : poutre sur appuis simples, charge uniforme : `d = 5·w·L⁴ / (384·E·I)`, avec `I = b·t³/12`,
  `w = p·b` (N/mm), `p` = charge surfacique (kg/m² × 9,80665 / 10⁶ N/mm²). (https://www.omnicalculator.com/physics/beam-deflection ;
  application aux étagères MDF avec E = 2 500 N/mm² : https://mdf-info.eu/applications/mobel/regale-und-stauelemente-aus-mdf).
* **Lois d'échelle** (vérifiées par test) : portée ×2 à charge totale constante → flèche ×8 ; épaisseur ×2 → flèche ÷8
  (cohérent avec https://woodbin.com/calcs/sagulator/).
* **Limite admissible** : `portée / shelf.max_deflection_ratio`, 400 par défaut (tables de portées d'étagères d'aggloméré/MDF
  limitées à L/400 : https://www.jlconline.com/wp-content/uploads/sites/4/1995/practical-engineering-down-with-sagging-shelves-tcm96-1152873.pdf ;
  L/180 est cité comme flèche déjà visible dans la même source).
* **Limites** : flèche initiale uniquement (le fluage peut ajouter ~50 % selon Sagulator), E dépend du produit et du fabricant
  (à saisir par panneau, aucune valeur n'est supposée) ; étagère considérée libre sur appuis. Statut : **DOCUMENTÉ**.

## F7. Système 32 mm (perçages)

* **Source** : principes du système 32 : pas de 32 mm, trous Ø5 mm, première rangée à 37 mm du chant avant, rangée arrière possible à 37 mm,
  distances entre rangées multiples de 32 mm (https://en.wikipedia.org/wiki/32_mm_cabinetmaking_system) ; embases de charnières
  à 37 mm du chant avant (Blum/Hettich, retours de praticiens : https://woodweb.com/knowledge_base/Odd_Cabinet_Heights_with_32mm.html).
* **Usage** : uniquement pour compter les trous (temps de perçage) : `trous/rangée = ⌊(H_int − 2·marge) / 32⌋ + 1`.
  La marge début/fin (`s32.margin_mm`, 64 par défaut) est une pratique d'atelier : **À VALIDER**. Pas, 37 mm : **DOCUMENTÉ**.
* **Limite** : certaines gammes (IKEA, coulisses Blum) utilisent des décalages « hors système » (retours sur
  https://festoolownersgroup.com/threads/32-mm-system-help.14642/latest). Le comptage est une estimation.

## F8. Géométrie du caisson et des façades

* Arithmétique pure (sommes/différences d'épaisseurs, répartition de jeux). **Vérifiée par tests** (cf. `EngineTest`) :
  côtés `hc × P` ; dessus/dessous `(L − 2e) × (P − retrait fond)` ; largeur de compartiment `(L − 2e − n·e)/(n+1)` ;
  portes `l = (L − 2·jeu_ext − (n−1)·jeu)/n`, avec conservation `n·l + (n−1)·jeu + 2·jeu_ext = L`.
* Les **jeux et retraits** (jeu de porte, retrait d'étagère, rainure de fond, réduction de hauteur de caisse de tiroir, jeu latéral
  de coulisse, jeu arrière) sont des **règles de l'atelier** (statut **À VALIDER** ou **CHOIX**). Aucune source sérieuse n'impose une valeur
  universelle ; pour les coulisses, la valeur vient de la fiche du fabricant.

## F9. Quincaillerie (hors charnières)

* Taquets : 4 par étagère mobile (logique mécanique, **CHOIX**). Coulisses : 1 jeu par tiroir (**CHOIX**).
* Connecteurs d'assemblage : `max(min_par_joint, ⌈longueur du joint / pas⌉)` par joint (4 joints de base + 2 par séparation).
  Aucune source normative trouvée pour min et pas : **À VALIDER** (`connector.min_per_joint`, `connector.spacing_mm`).

## F10. Main-d'œuvre

* **Aucune base documentée universelle** n'a été trouvée pour les temps de fabrication de meubles (ils dépendent de l'outillage,
  de l'organisation et de la série). Le moteur calcule `heures = Σ (quantité de la base × minutes de référence) / 60` et
  `coût = heures × taux horaire`, mais **tous les temps sont saisis par l'administrateur** (0 par défaut, statut `TO_VALIDATE`).
  Les bases disponibles : feuille, coupe, pièce, mètre de chant, bande de chant, trou, meuble, porte, tiroir, charnière, m² de finition.
* Méthode recommandée : chronométrer 5 à 10 meubles réels, calculer les minutes par base, les saisir, puis comparer (cf. `VALIDATION.md`).

## F11. Coûts et prix

* Coût panneaux : feuilles entières × prix de feuille (mode 0) ou surface nette/surface feuille × prix × (1 + perte %) (mode 1).
* Chants : mètres à prévoir × prix/m. Quincaillerie : quantité × prix unitaire. Finition : surface visible × facteur de faces × prix/m².
* `coût de production = panneaux + chants + quincaillerie + main-d'œuvre + finition`.
* `marge = coût de production × taux de marge / 100` (**markup** = (prix − coût)/coût ; ne pas confondre avec le taux de marge sur prix).
* `total HT = production + marge + transport + installation` (transport et installation sont saisis tels que facturés, sans marge ajoutée).
* `taxes = total HT × taux` pour chaque taxe active ; arrondi `HALF_UP` à 2 décimales par ligne ; tous les montants en `BigDecimal`.
* Ce sont des définitions comptables du projet, pas des formules « techniques » : à faire valider par la direction commerciale.

---

## Ce qui n'a PAS pu être documenté

| Sujet | Traitement |
|---|---|
| Jeux de portes, retraits d'étagères, rainures, réductions de tiroirs | Règles configurables, **À VALIDER** |
| Pas et nombre minimal de connecteurs | Règles configurables, **À VALIDER** |
| Abaque charnières par hauteur (cuvette) | Table provisoire à remplacer par l'abaque du fabricant |
| Surlongueur et perte de chant | Règles configurables, **À VALIDER** |
| Temps de fabrication | Saisie admin, calibrage sur cas réels |
| Facteur de surface de finition | Règle configurable, **À VALIDER** |
