// Jeu de données de démo EventByte : crée des événements maîtrisés dans Strapi,
// qui passent ensuite par le vrai circuit (outbox -> RabbitMQ -> backend -> Postgres + Meilisearch).
//
// Usage (Node 18+, depuis la racine du dépôt) :
//   $env:STRAPI_TOKEN="<token full access>"; node scripts/demo-seed/seed-demo.mjs            (PowerShell)
//   STRAPI_TOKEN=<token> node scripts/demo-seed/seed-demo.mjs                               (bash)
// Options :
//   --clean    supprime d'abord TOUS les événements Strapi (envoie event.deleted pour chacun)
//   --dry-run  affiche ce qui serait créé, sans rien envoyer

import { readFile } from "node:fs/promises";

const STRAPI_URL = process.env.STRAPI_URL ?? "http://localhost:1337";
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:3000";
const TOKEN = process.env.STRAPI_TOKEN;
const args = new Set(process.argv.slice(2));
const clean = args.has("--clean");
const dryRun = args.has("--dry-run");

if (!TOKEN && !dryRun) {
    console.error("STRAPI_TOKEN manquant : créez un token 'Full access' dans Strapi (Settings > API Tokens).");
    process.exit(1);
}

async function strapi(method, path, body) {
    const response = await fetch(`${STRAPI_URL}${path}`, {
        method,
        headers: { Authorization: `Bearer ${TOKEN}`, "Content-Type": "application/json" },
        body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
        throw new Error(`${method} ${path} -> ${response.status} ${await response.text()}`);
    }
    return response.status === 204 ? null : response.json();
}

async function checkBackend() {
    try {
        const health = await fetch(`${BACKEND_URL}/actuator/health`);
        if (health.ok) return;
    } catch { /* backend arrêté */ }
    console.warn(`⚠️  Backend injoignable sur ${BACKEND_URL}. Les messages attendront dans la file `
        + `eventbyte.events si le backend a déjà été démarré une fois ; sinon ils seront PERDUS.`);
}

async function imageIds() {
    const files = await strapi("GET", "/api/upload/files");
    const images = files.filter((f) => f.mime?.startsWith("image/")).map((f) => f.id);
    if (images.length === 0) {
        throw new Error("Aucune image dans la médiathèque Strapi : ajoutez-en au moins une (cover obligatoire).");
    }
    return images;
}

async function deleteAllEvents() {
    let deleted = 0;
    for (;;) {
        const page = await strapi("GET", "/api/events?fields[0]=title&pagination[page]=1&pagination[pageSize]=100");
        if (page.data.length === 0) break;
        for (const event of page.data) {
            await strapi("DELETE", `/api/events/${event.documentId}`);
            deleted++;
        }
        process.stdout.write(`\r🧹 ${deleted} événements supprimés`);
    }
    console.log(deleted ? "" : "🧹 Aucun événement à supprimer");
}

const events = JSON.parse(await readFile(new URL("./demo-events.json", import.meta.url), "utf8"));

if (dryRun) {
    for (const e of events) {
        const prices = e.tickets.map((t) => `${t.name} ${t.price.toFixed(2)} € x${t.quantity}`).join(", ");
        console.log(`- ${e.title} | ${e.venue.name} (${e.venue.lat}, ${e.venue.lng}) | ${e.startAt} | ${prices}`);
    }
    process.exit(0);
}

await checkBackend();
if (clean) await deleteAllEvents();
const covers = await imageIds();

for (const [i, e] of events.entries()) {
    await strapi("POST", "/api/events", {
        data: {
            title: e.title,
            description: e.description,
            startAt: new Date(e.startAt).toISOString(),
            cover: covers[i % covers.length],
            venue: { name: e.venue.name, adress: e.venue.adress, coordinate: { lat: e.venue.lat, lng: e.venue.lng } },
            tickets: e.tickets,
        },
    });
    console.log(`✅ ${e.title}`);
}
console.log(`\n${events.length} événements créés. Ils arrivent dans le backend en 1 à 2 secondes (outbox Strapi).`);
