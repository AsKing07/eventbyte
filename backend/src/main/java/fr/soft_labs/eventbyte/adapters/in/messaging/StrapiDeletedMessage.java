package fr.soft_labs.eventbyte.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Message event.deleted : Strapi n'envoie que l'id. */
@JsonIgnoreProperties(ignoreUnknown = true)
record StrapiDeletedMessage(String id) {
}
