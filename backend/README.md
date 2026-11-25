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
Les secrets (clé Stripe à venir) passent uniquement par variables d'environnement, jamais dans le dépôt.
