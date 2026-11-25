# Jeu de données de démo

12 événements maîtrisés pour la démo finale (US-20), créés dans Strapi : ils suivent le vrai circuit
outbox → RabbitMQ → backend → Postgres + Meilisearch, comme un événement saisi à la main.

## Lancer

Prérequis : `docker compose up -d`, le backend démarré au moins une fois (sa file RabbitMQ doit exister),
au moins une image dans la médiathèque Strapi, Node 18+.

1. Strapi admin (http://localhost:1337/admin) > Settings > API Tokens > Create new API Token,
   type **Full access**, puis copier le token.
2. Depuis la racine du dépôt (PowerShell) :

```powershell
$env:STRAPI_TOKEN="<token>"
node scripts/demo-seed/seed-demo.mjs --dry-run   # aperçu, n'envoie rien
node scripts/demo-seed/seed-demo.mjs --clean     # vide Strapi puis crée les 12 événements
```

`--clean` supprime tous les événements Strapi (y compris ceux du CLI) : chacun envoie un `event.deleted`,
le backend les passe en suppression logique et les retire de Meilisearch.

## Ce que chaque événement permet de montrer

Position par défaut du front : Paris (48.8585, 2.3475), rayon 10 km.

| Événement | Pour montrer |
|---|---|
| Nuit Jazz au Sunset | Recherche « jazz » à Paris ; prix le plus bas 22,00 € |
| Masterclass Jazz : dernière place | 1 seule place : deux réservations simultanées, une seule réussit (US-13) ; expiration (US-14) |
| Concert Électro : Warehouse Session | Un tarif épuisé (Early Bird à 0) mais l'événement n'est pas complet |
| Concert Acoustique Intimiste (complet) | `isSoldOut = true` |
| Concert Rock : Les Rails Électriques, Théâtre, Classique, Expo | Volume et variété pour la page discover ; prix avec centimes (32,90 €) |
| Stand-up : Rires de Montreuil | Événement près de l'école, dans le rayon de 10 km |
| Concert Rock en banlieue ouest | À ~19 km : absent avec 10 km, présent avec 20 km (filtre de rayon) |
| Concert Jazz à Lyon | « jazz » à Paris ne le trouve pas ; visible en choisissant Lyon |
| Concert Électro sur le Vieux-Port | Sélecteur de ville : Marseille |

Dates : du 4 au 20 décembre 2026, donc toutes à venir le jour de la démo.
Les noms de lieux sont réels, les artistes et groupes sont fictifs.

## Parcours de démo suggéré

1. Créer un événement à la main dans Strapi → il apparaît dans le front (discover) en quelques secondes.
2. Rechercher « jazz » à Paris (10 km) → Sunset et Masterclass, pas Lyon.
3. Passer le rayon à 20 km → le concert de Saint-Germain-en-Laye apparaît.
4. Liker un événement → compteur mis à jour, visible dans « mes favoris ».
5. Réserver la Masterclass → paiement Stripe (carte test 4242 4242 4242 4242) → confirmation en direct (SSE).
6. Montrer l'événement complet et, en parallèle, deux réservations de la dernière place.
