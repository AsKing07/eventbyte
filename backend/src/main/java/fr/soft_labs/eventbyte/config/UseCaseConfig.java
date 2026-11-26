package fr.soft_labs.eventbyte.config;

import fr.soft_labs.eventbyte.ports.in.ConfirmPayment;
import fr.soft_labs.eventbyte.ports.in.ExpireUnpaidBookings;
import fr.soft_labs.eventbyte.ports.in.GetEventById;
import fr.soft_labs.eventbyte.ports.in.DiscoverEvents;
import fr.soft_labs.eventbyte.ports.in.GetEventTickets;
import fr.soft_labs.eventbyte.ports.in.GetEventsByIds;
import fr.soft_labs.eventbyte.ports.in.IngestEvent;
import fr.soft_labs.eventbyte.ports.in.LikeEvent;
import fr.soft_labs.eventbyte.ports.in.ListLikedEvents;
import fr.soft_labs.eventbyte.ports.in.RequestBooking;
import fr.soft_labs.eventbyte.ports.in.SearchEvents;
import fr.soft_labs.eventbyte.ports.in.StartPayment;
import fr.soft_labs.eventbyte.ports.in.UnlikeEvent;
import fr.soft_labs.eventbyte.ports.in.ViewBookings;
import fr.soft_labs.eventbyte.ports.out.BookingUpdatePublisher;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import fr.soft_labs.eventbyte.ports.out.ProcessedPayments;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import fr.soft_labs.eventbyte.usecases.ConfirmPaymentUseCase;
import fr.soft_labs.eventbyte.usecases.BookingUpdatesUseCase;
import fr.soft_labs.eventbyte.usecases.ExpireUnpaidBookingsUseCase;
import fr.soft_labs.eventbyte.usecases.GetEventByIdUseCase;
import fr.soft_labs.eventbyte.usecases.DiscoverEventsUseCase;
import fr.soft_labs.eventbyte.usecases.GetEventTicketsUseCase;
import fr.soft_labs.eventbyte.usecases.GetEventsByIdsUseCase;
import fr.soft_labs.eventbyte.usecases.IngestEventUseCase;
import fr.soft_labs.eventbyte.usecases.LikeEventUseCase;
import fr.soft_labs.eventbyte.usecases.ListLikedEventsUseCase;
import fr.soft_labs.eventbyte.usecases.RequestBookingUseCase;
import fr.soft_labs.eventbyte.usecases.SearchEventsUseCase;
import fr.soft_labs.eventbyte.usecases.StartPaymentUseCase;
import fr.soft_labs.eventbyte.usecases.UnlikeEventUseCase;
import fr.soft_labs.eventbyte.usecases.ViewBookingsUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Les cas d'usage n'ont pas d'annotation Spring : ils sont déclarés ici.
 * Ajoutez vos cas d'usage à la suite.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    BookingUpdatesUseCase bookingUpdatesUseCase() {
        return new BookingUpdatesUseCase();
    }

    @Bean
    IngestEvent ingestEvent(EventRepository eventRepository, EventSearchIndex eventSearchIndex,
                            TicketStock ticketStock, Clock clock) {
        return new IngestEventUseCase(eventRepository, eventSearchIndex, ticketStock, clock);
    }

    @Bean
    SearchEvents searchEvents(EventSearchIndex eventSearchIndex, EventReadModel eventReadModel) {
        return new SearchEventsUseCase(eventSearchIndex, eventReadModel);
    }

    @Bean
    DiscoverEvents discoverEvents(EventSearchIndex eventSearchIndex, EventReadModel eventReadModel, Clock clock) {
        return new DiscoverEventsUseCase(eventSearchIndex, eventReadModel, clock);
    }

    @Bean
    GetEventsByIds getEventsByIds(EventReadModel eventReadModel) {
        return new GetEventsByIdsUseCase(eventReadModel);
    }

    @Bean
    GetEventById getEventById(EventReadModel eventReadModel) {
        return new GetEventByIdUseCase(eventReadModel);
    }

    @Bean
    LikeEvent likeEvent(EventRepository eventRepository, LikeRepository likeRepository) {
        return new LikeEventUseCase(eventRepository, likeRepository);
    }

    @Bean
    ListLikedEvents listLikedEvents(LikeRepository likeRepository, EventReadModel eventReadModel) {
        return new ListLikedEventsUseCase(likeRepository, eventReadModel);
    }

    @Bean
    UnlikeEvent unlikeEvent(LikeRepository likeRepository) {
        return new UnlikeEventUseCase(likeRepository);
    }

    @Bean
    StartPayment startPayment(BookingRepository bookingRepository, PaymentGateway paymentGateway,
                              TicketStock ticketStock, BookingUpdatePublisher bookingUpdates) {
        return new StartPaymentUseCase(bookingRepository, paymentGateway, ticketStock, bookingUpdates);
    }

    @Bean
    RequestBooking requestBooking(TicketCatalog ticketCatalog, TicketStock ticketStock,
                                  BookingRepository bookingRepository, Clock clock, StartPayment startPayment,
                                  BookingUpdatePublisher bookingUpdates) {
        return new RequestBookingUseCase(ticketCatalog, ticketStock, bookingRepository,
                () -> UUID.randomUUID().toString(), clock, startPayment, bookingUpdates);
    }

    @Bean
    GetEventTickets getEventTickets(EventRepository eventRepository, TicketCatalog ticketCatalog,
                                    TicketStock ticketStock) {
        return new GetEventTicketsUseCase(eventRepository, ticketCatalog, ticketStock);
    }

    @Bean
    ConfirmPayment confirmPayment(BookingRepository bookingRepository, TicketStock ticketStock,
                                  PaymentGateway paymentGateway, ProcessedPayments processedPayments,
                                  BookingUpdatePublisher bookingUpdates) {
        return new ConfirmPaymentUseCase(bookingRepository, ticketStock, paymentGateway, processedPayments,
                bookingUpdates);
    }

    @Bean
    ExpireUnpaidBookings expireUnpaidBookings(BookingRepository bookingRepository, TicketStock ticketStock,
                                              Clock clock,
                                              @Value("${eventbyte.booking.expiration-delay:10m}") Duration expirationDelay,
                                              BookingUpdatePublisher bookingUpdates) {
        return new ExpireUnpaidBookingsUseCase(bookingRepository, ticketStock, clock, expirationDelay,
                bookingUpdates);
    }

    @Bean
    ViewBookings viewBookings(BookingRepository bookingRepository) {
        return new ViewBookingsUseCase(bookingRepository);
    }
}
