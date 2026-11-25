package fr.soft_labs.eventbyte.adapters.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.soft_labs.eventbyte.ports.in.IngestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consomme les messages de Strapi. Strapi publie le JSON brut, sans content-type :
 * on lit donc le corps nous-mêmes et on choisit l'action d'après la routing key.
 * 
 * Un message illisible ou invalide part directement en file d'erreur (DLQ) au lieu de boucler.
 * Une erreur technique (base indisponible) est retentée, puis envoyée en DLQ (voir application.yml).
 */
@Component
class StrapiEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(StrapiEventListener.class);

    private final IngestEvent ingestEvent;
    private final ObjectMapper objectMapper;

    StrapiEventListener(IngestEvent ingestEvent, ObjectMapper objectMapper) {
        this.ingestEvent = ingestEvent;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${eventbyte.rabbitmq.events-queue}")
    void onMessage(Message message) {
        String type = message.getMessageProperties().getReceivedRoutingKey();
        byte[] body = message.getBody();
        try {
            switch (type) {
                case "event.created", "event.updated" ->
                        ingestEvent.upsert(objectMapper.readValue(body, StrapiEventMessage.class).toDomain());
                case "event.deleted" ->
                        ingestEvent.delete(objectMapper.readValue(body, StrapiDeletedMessage.class).id());
                default -> LOG.warn("Message Strapi ignoré, type inconnu : {}", type);
            }
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException | ArithmeticException e) {
            LOG.error("Message Strapi {} invalide, envoyé en DLQ : {}", type, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Message Strapi invalide", e);
        } catch (IOException e) {
            throw new AmqpRejectAndDontRequeueException("Message Strapi illisible", e);
        }
    }
}
