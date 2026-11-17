# Plateforme de devis pour meubles sur mesure

Spring Boot 3 (Java 21) · PostgreSQL 16 · React + TypeScript (Vite, Tailwind) · API REST · PDF (OpenPDF).

## Démarrage
```bash
cp .env.example .env        # puis modifier les mots de passe (Windows : copy .env.example .env)
docker compose up --build   # front http://localhost:8081, API http://localhost:8080, Swagger /swagger-ui.html
```
Connexion avec `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` (créés au premier démarrage ; aucun identifiant n'existe dans le code).
En développement : `cd backend && mvn spring-boot:run` et `cd frontend && npm install && npm run dev`.

## Devis par composants (modèle principal)
Meuble -> composants (Caisson, Étagère, Porte, Poignées...) -> lignes (article du catalogue + surface/quantité saisie).
Montant d'une ligne = quantité x prix unitaire x coefficient d'arrivée de l'article. Administration : onglets « Articles » et
« Gabarits de composants ». Deux PDF : devis client (sans coûts) et fiche de revient interne. L'ancien calcul automatique
(onglets « Calcul auto ») reste disponible en option.

### Surfaces et chants automatiques
Les surfaces de panneaux et les mètres de chant sont calculés à partir des mesures du composant (LARG, PROF, HAUT, QTE), avec les formules
du second tableau de MEUBLE_HAUT.xlsx : pièces (joue, dessous/dessus, fond, étagère, porte), chute de panneau, chant avec surlongueur,
colle = chant / 50. La quincaillerie reste en quantité par défaut (modifiable). Administration : « Gabarits », « Pièces des gabarits »,
« Lignes des gabarits ». Formules : variables L, P, H, Q (et CH = mètres de chant pour les lignes).

## Premiers pas
1. **Administration > Tarifs & marges** : taux horaire, finition, transport, installation, marge, devise.
2. **Taxes** : créer les taxes applicables. **Panneaux / Chants / Accessoires** : remplacer les références d'exemple (prix à 0) par les vôtres.
3. **Temps de fabrication** : saisir les minutes par opération (0 par défaut).
4. **Règles de fabrication** : relire les règles `TO_VALIDATE` (cf. `docs/FORMULES.md`), les valider ou les ajuster, par type de meuble.
5. Créer un devis : le moteur affiche des avertissements tant que des prix, temps ou règles ne sont pas validés.

## Architecture
```
backend/.../engine/        moteur de calcul en Java pur (aucune dépendance Spring) : testable seul
  model/                   FurnitureSpec, Part, PanelMaterial, Rules, EngineConfig, Catalog ...
  service/                 CarcassGeometryService, CutPlanService, EdgeBandingService, HardwareService,
                           System32Service, StructuralCheckService, LaborService, QuoteEngine
config/DbEngineSupport     branche le moteur sur la base (règles, temps, seuils, prix)
web/                       API REST : calcul, devis, catalogue, CRUD d'administration (/api/admin/**, rôle ADMIN)
pdf/QuotePdfService        devis PDF généré depuis le résultat figé
db/migration/              V1 schéma, V2 règles & paramètres (sans tarifs), V3 références d'exemple à prix 0
frontend/                  formulaire de devis, détail des coûts, plan de débit SVG, administration
docs/FORMULES.md           registre : source, unités, hypothèses, limites, statut de chaque formule
docs/VALIDATION.md         plan de validation sur cas réels
```
Les devis enregistrés conservent leur entrée et leur résultat : modifier un prix n'altère pas un devis émis.

## API principale
`POST /api/quotes/calculate` (aperçu) · `POST /api/quotes` (enregistre) · `GET /api/quotes[/{id}[/pdf]]` ·
`GET /api/catalog` · `/api/admin/{panels,edge-bands,hardware,settings,taxes,labor,rules,thresholds,furniture-types}` (CRUD).

## État de vérification (honnête)
* **Moteur Java : compilé et testé ici** (JDK 21) : 28 tests (dont 11 sur le calcul par composants, qui reproduit les valeurs du fichier Excel ; géométrie, chants, débit, flèche, système 32, totaux, scénarios) passent.
* **Couche Spring, SQL, PDF et frontend : écrits mais non compilés/exécutés** dans l'environnement de génération (pas d'accès réseau pour Maven/npm).
  Les sources Spring ont seulement été contrôlées syntaxiquement. Attendez-vous à de petites corrections au premier `mvn test` / `npm run build`.
* Les formules géométriques sont vérifiées mathématiquement ; **pertes, temps et jeux ne sont pas validés** : voir `docs/VALIDATION.md`.
* Authentification HTTP Basic sans état (simple). Pour une mise en production exposée : HTTPS obligatoire, et JWT/OIDC conseillé.


