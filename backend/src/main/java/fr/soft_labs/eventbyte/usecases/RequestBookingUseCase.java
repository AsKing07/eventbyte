package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;
import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import fr.soft_labs.eventbyte.ports.in.RequestBooking;
import fr.soft_labs.eventbyte.ports.in.StartPayment;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Traitement SYNCHRONE dans la requête (décision EB-35) :
 * <ol>
 *   <li>prix et noms lus en base, jamais envoyés par le client ;</li>
 *   <li>réservation enregistrée en requested ;</li>
 *   <li>stock pris de façon atomique (tout ou rien) -> reserved, sinon rejected ;</li>
 *   <li>paiement Stripe préparé -> awaiting-payment avec clientSecret (US-16, {@link StartPayment}).</li>
 * </ol>
 * Pour passer en asynchrone plus tard : publier après l'étape 2 et faire l'étape 3 dans un listener.
 * Aucun état dans la classe : une seule instance sert toutes les requêtes en parallèle.
 */
public class RequestBookingUseCase implements RequestBooking {

    private final TicketCatalog catalog;
    private final TicketStock stock;
    private final BookingRepository bookings;
    private final Supplier<String> idGenerator;
    private final Clock clock;
    private final StartPayment startPayment;

    public RequestBookingUseCase(TicketCatalog catalog, TicketStock stock, BookingRepository bookings,
                                 Supplier<String> idGenerator, Clock clock, StartPayment startPayment) {
        this.catalog = Objects.requireNonNull(catalog);
        this.stock = Objects.requireNonNull(stock);
        this.bookings = Objects.requireNonNull(bookings);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.startPayment = Objects.requireNonNull(startPayment);
    }

    @Override
    public Booking book(UserId userId, List<RequestedItem> items) {
        Map<String, TicketOffer> offers = offersFor(items);
        List<BookingLine> lines = items.stream()
                .map(item -> {
                    TicketOffer offer = offers.get(item.ticketId());
                    return new BookingLine(offer.ticketId(), offer.name(), item.quantity(), offer.priceInCents());
                })
                .toList();

        Booking requested = Booking.request(idGenerator.get(), userId, singleEvent(offers), lines, clock.instant());
        bookings.save(requested);

        if (!stock.tryTake(lines)) {
            Booking rejected = requested.reject();
            bookings.save(rejected);
            return rejected;
        }
        Booking reserved = requested.reserve();
        try {
            bookings.save(reserved);
        } catch (RuntimeException e) {
            stock.giveBack(lines);   // ne pas perdre de places si l'enregistrement échoue
            throw e;
        }
        return startPayment.startPayment(reserved.id());
    }

    /** Offres des tickets demandés ; refuse une demande vide, en double ou avec un ticket inconnu. */
    private Map<String, TicketOffer> offersFor(List<RequestedItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La réservation doit contenir au moins un ticket");
        }
        Set<String> ids = new HashSet<>();
        for (RequestedItem item : items) {
            if (!ids.add(item.ticketId())) {
                throw new IllegalArgumentException("Ticket demandé deux fois : " + item.ticketId());
            }
        }
        Map<String, TicketOffer> offers = catalog.findOffers(ids).stream()
                .collect(Collectors.toMap(TicketOffer::ticketId, Function.identity()));
        for (String id : ids) {
            if (!offers.containsKey(id)) {
                throw new IllegalArgumentException("Ticket inconnu ou plus en vente : " + id);
            }
        }
        return offers;
    }

    private static String singleEvent(Map<String, TicketOffer> offers) {
        Set<String> events = offers.values().stream().map(TicketOffer::eventId).collect(Collectors.toSet());
        if (events.size() != 1) {
            throw new IllegalArgumentException("Une réservation ne concerne qu'un seul événement");
        }
        return events.iterator().next();
    }
}
