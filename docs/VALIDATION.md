# Plan de validation métier

## 1. Déjà vérifié par des tests automatisés (`EngineTest`)
Géométrie du caisson et des façades (valeurs calculées à la main), conservation des largeurs, longueurs de chants,
dimensions de débit après chant, plan de débit (aucun chevauchement, aucun dépassement, trait de scie respecté, fil imposé,
borne inférieure de feuilles, pièce trop grande refusée), flèche des étagères (formule + lois d'échelle + portée max),
comptage système 32, arithmétique de main-d'œuvre, cohérence des totaux (production + marge + transport + installation = HT ; HT + taxes = TTC).

## 2. À valider sur cas réels (estimations non mathématiques)
| Grandeur | Comment valider | Critère suggéré (à fixer par l'atelier) |
|---|---|---|
| Feuilles consommées | Comparer au logiciel de débit/optimiseur de l'atelier sur 10 meubles | écart moyen et maximal acceptables |
| Perte de chant | Peser/mesurer les rouleaux consommés sur 5 commandes | écart sur les mètres |
| Surlongueur de chant | Mesurer les chutes à la plaqueuse | valeur retenue = moyenne observée |
| Temps de fabrication | Chronométrer chaque opération sur 5 à 10 meubles | écart devis/réel sur les heures |
| Jeux, retraits, rainures | Contrôle au montage d'un prototype | pas de reprise de pièces |
| Quantité de connecteurs | Comparer à la pratique réelle | pas de manque ni de surplus récurrent |
| Charnières | Valider avec l'abaque du fabricant retenu | conformité à la fiche |

## 3. Procédure
1. Saisir prix et temps dans l'administration. 2. Calculer 10 meubles déjà fabriqués. 3. Comparer matière, heures et coût réel.
4. Ajuster uniquement les règles marquées `TO_VALIDATE`, puis passer leur statut à `DESIGN_CHOICE` ou `DOCUMENTED` une fois validées.
5. Ajouter chaque cas validé comme test de non-régression (scénario + valeurs attendues).
