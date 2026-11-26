package fr.soft_labs.eventbyte.adapters.in.scheduling;

import fr.soft_labs.eventbyte.ports.in.ExpireUnpaidBookings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class BookingExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationScheduler.class);
    private final ExpireUnpaidBookings expireUnpaidBookings;

    BookingExpirationScheduler(ExpireUnpaidBookings expireUnpaidBookings) {
        this.expireUnpaidBookings = expireUnpaidBookings;
    }

    @Scheduled(fixedDelayString = "${eventbyte.booking.expiration-scan-interval:30s}")
    void expireOverdueBookings() {
        int expired = expireUnpaidBookings.expireOverdueBookings();
        if (expired > 0) {
            log.info("{} réservation(s) impayée(s) expirée(s)", expired);
        }
    }
}
