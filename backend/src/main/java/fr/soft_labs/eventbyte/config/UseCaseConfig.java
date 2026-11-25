package fr.soft_labs.eventbyte.config;

import fr.soft_labs.eventbyte.ports.in.IngestEvent;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.usecases.IngestEventUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IngestEvent ingestEvent(EventRepository eventRepository, Clock clock) {
        return new IngestEventUseCase(eventRepository, clock);
    }
}
