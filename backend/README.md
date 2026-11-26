# EventByte · backend

Spring Boot 3.5 · Java 21 · Maven · architecture hexagonale.

## Lancer en local

1. Démarrer l'infra depuis la racine du dépôt : `docker compose up -d`
2. Depuis `backend/` : `mvn spring-boot:run` (ou lancer `EventbyteApplication` dans l'IDE)
3. Vérifier : http://localhost:3000/actuator/health doit répondre `UP`, avec `db`, `rabbit` et `redis` à `UP`

Au démarrage, Flyway applique les migrations de `src/main/resources/db/migration`,
puis Hibernate valide le schéma (`ddl-auto: validate`). Une entité JPA qui ne correspond pas
au schéma fait échouer le démarrage : toute évolution du schéma passe par une nouvelle migration
`V<n>__<description>.sql`. On ne modifie jamais une migration déjà appliquée.

Le port 3000 correspond à l'URL par défaut du front (`VITE_API_URL`).

## Tests

```
mvn test
```

- `ArchitectureTest` : le domaine et les cas d'usage ne dépendent d'aucun framework. Tourne partout.
- `ApplicationContextTest` : démarre l'application avec Postgres, RabbitMQ et Redis via Testcontainers.
  Docker doit tourner. Sans Docker, ce test est ignoré.

## Organisation du code

```
fr.soft_labs.eventbyte
├── domain        Java pur : Event, Ticket, Booking, règles métier
├── usecases      cas d'usage, sans annotation Spring
├── ports
│   ├── in        interfaces des cas d'usage
│   └── out       interfaces vers l'extérieur (repository, recherche, stock, paiement)
├── adapters
│   ├── in        REST, consumer RabbitMQ, webhook Stripe, SSE
│   └── out       JPA, Meilisearch, Redis, Stripe
└── config        sécurité, clients techniques, déclaration des beans
```

Les dépendances pointent vers le centre : `adapters → ports/usecases → domain`.
Les entités JPA et les DTO restent dans `adapters`, jamais dans `domain`.

## Configuration

Toutes les valeurs par défaut de `application.yml` correspondent au `docker-compose.yml`.
Chacune peut être surchargée par une variable d'environnement
(`DB_URL`, `RABBITMQ_HOST`, `REDIS_HOST`, `KEYCLOAK_ISSUER_URI`, `MEILISEARCH_HOST`, `SERVER_PORT`…).
Les secrets passent uniquement par variables d'environnement, jamais dans le dépôt :

- `STRIPE_SECRET_KEY` : clé secrète Stripe de test (`sk_test_...`, dashboard Stripe > Développeurs > Clés API).
  Sans elle, l'application démarre mais `POST /bookings` répond 503 au moment du paiement (places rendues).
  Les tests n'appellent jamais Stripe (`FakePaymentConfiguration`).
- `STRIPE_WEBHOOK_SECRET` : secret de signature du webhook (`whsec_...`). En local, il est affiché par
  `stripe listen --forward-to localhost:3000/payments/stripe/webhook` (Stripe CLI, à laisser tourner pendant la démo).

## Écarts avec le sujet et décisions

Constats faits sur le code fourni et les données réelles, et choix du groupe.
Le détail et les preuves sont dans les tickets Jira `decision-spec`.

| Sujet | Ce que dit le README / PDF | Ce qu'on a constaté | Décision |
|---|---|---|---|
| Prix reçus de Strapi (EB-28) | Message en centimes | Strapi stocke des euros (`decimal`, ex. `30.23` générés par le CLI) | Conversion ×100 à la réception : centimes partout dans le backend (requis par Stripe) |
| Prix renvoyés par l'API (EB-28) | Centimes | Le front affiche le nombre tel quel en euros (pas de division par 100, données de démo en `13.99`) | L'API renvoie des euros, conversion à un seul endroit ; le front doit fonctionner sans modification |
| `startAt` (EB-27) | Obligatoire dans la réponse API | Absent du message Strapi alors que le front affiche la date | Ajout d'une ligne dans `frontend/strapi/.../lifecycles.ts` ; le backend le lit s'il est présent |
| Rayon de recherche (US-7) | `radius` en kilomètres | Le front envoie des mètres (choix 1 000 à 100 000, affichés « / 1000 km ») | Conversion mètres → km dans `EventSearchController` ; le domaine reste en km |
| Disponibilité tickets (US-11) | `quantity` disponible | `tickets.quantity` contient le stock total ; Redis est la source du stock restant consommable atomiquement par les réservations | `GET /events/{eventId}/tickets` lit Redis, initialisé depuis le stock total moins les réservations qui tiennent du stock ; réponse publique en euros pour `price` |
| Événements sans date | — | Le front valide les réponses avec zod : `startAt` doit être une chaîne, un seul `null` fait échouer toute la liste | Les événements sans `startAt` sont exclus des résultats |
| Frais de réservation (EB-34) | `fees` sans règle | Le faux backend du front compte 0,30 € par ticket, total = tickets + frais | Même règle : 30 centimes par ticket (`Booking.FEE_PER_TICKET_IN_CENTS`) |
| Passage requested → reserved (EB-35) | Asynchrone suggéré | Le front affiche « en cours » pour requested et reserved, le paiement seulement en awaiting-payment | Traitement synchrone dans `POST /bookings`, stock pris atomiquement dans Redis (script Lua, tout ou rien) |
| Quantité modifiée dans Strapi (EB-30) | — | Le stock Redis gardait l'ancienne valeur : survente possible si l'organisateur baisse la quantité | À chaque `event.updated`, les clés Redis des tickets sont supprimées puis recalculées depuis la base ; sous le déjà-réservé : disponible 0, rien n'est annulé |
| Paiement (US-16) | Pas de route « payer » côté backend | Le front affiche Stripe dès que la réservation est en `awaiting-payment` avec un `clientSecret` | `POST /bookings` enchaîne réservation puis PaymentIntent (montant = tickets + frais, en centimes, devise EUR) ; clé d'idempotence = id de réservation ; si Stripe échoue : places rendues, réservation `expired`, réponse 503 |
| Paiement après expiration (EB-33) | — | Stripe peut confirmer un paiement après l'expiration de la réservation | On reprend les places si elles sont encore libres (réservation confirmée), sinon remboursement automatique (réservation reste `expired`). Un même paiement n'est traité qu'une fois (table `processed_payments`) |
| Coordonnées du lieu | Utilisées pour la recherche géographique | Facultatives dans Strapi : le hook plante (`reading 'lat'`) et aucun message n'est envoyé | Toujours renseigner la position ; correction possible : `required: true` dans `venue.json` |
| Ordre des messages | — | L'outbox Strapi n'a pas d'`ORDER BY` et republie les échecs plus tard | Ingestion idempotente ; un événement supprimé ne revient pas |
| Messages publiés avant le 1er démarrage du backend | — | Exchange topic sans file : messages perdus | Démarrer le backend avant le seed ; rejeu possible via `strapi.outbox_events` |

Rejouer tous les messages Strapi (environnement de dev) :

```
docker compose exec postgres psql -U root -d eventbyte -c "truncate public.tickets, public.events; update strapi.outbox_events set processed_at = null;"
```
