package fr.soft_labs.eventbyte.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strapi publie sur l'exchange topic "events" (routing keys event.created / updated / deleted).
 * Le backend y branche sa propre file durable, avec une file d'erreur pour les messages rejetés.
 */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange strapiEventsExchange(@Value("${eventbyte.rabbitmq.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange eventsDeadLetterExchange(@Value("${eventbyte.rabbitmq.events-queue}") String queue) {
        return new DirectExchange(queue + ".dlx", true, false);
    }

    @Bean
    Queue eventsDeadLetterQueue(@Value("${eventbyte.rabbitmq.events-queue}") String queue) {
        return QueueBuilder.durable(queue + ".dlq").build();
    }

    @Bean
    Binding eventsDeadLetterBinding(Queue eventsDeadLetterQueue, DirectExchange eventsDeadLetterExchange,
                                    @Value("${eventbyte.rabbitmq.events-queue}") String queue) {
        return BindingBuilder.bind(eventsDeadLetterQueue).to(eventsDeadLetterExchange).with(queue);
    }

    @Bean
    Queue eventsQueue(@Value("${eventbyte.rabbitmq.events-queue}") String queue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(queue + ".dlx")
                .deadLetterRoutingKey(queue)
                .build();
    }

    @Bean
    Binding eventsBinding(Queue eventsQueue, TopicExchange strapiEventsExchange) {
        return BindingBuilder.bind(eventsQueue).to(strapiEventsExchange).with("event.*");
    }
}
