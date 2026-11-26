package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpireUnpaidBookingsUseCaseTest {

    private static final Instant BOOKED_AT = Instant.parse("2026-11-26T10:00:00Z");
    private static final Duration EXPIRATION_DELAY = Duration.ofMinutes(10);

    @Test
    void a_t_plus_dix_minutes_et_une_seconde_expire_et_restitue_le_stock() {
        RequestBookingUseCaseTest.RecordingRepository repository = new RequestBookingUseCaseTest.RecordingRepository();
        RequestBookingUseCaseTest.FakeStock stock = new RequestBookingUseCaseTest.FakeStock();
        Booking booking = unpaidBooking();
        repository.bookings.put(booking.id(), booking);
        stock.available.put("ticket", 0);
        ExpireUnpaidBookingsUseCase useCase = useCase(repository, stock, BOOKED_AT.plusSeconds(601));

        assertThat(useCase.expireOverdueBookings()).isEqualTo(1);
        assertThat(repository.bookings.get(booking.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(stock.available).containsEntry("ticket", 2);
    }

    @Test
    void avant_le_delai_la_reservation_reste_en_attente_et_le_stock_n_est_pas_rendu() {
        RequestBookingUseCaseTest.RecordingRepository repository = new RequestBookingUseCaseTest.RecordingRepository();
        RequestBookingUseCaseTest.FakeStock stock = new RequestBookingUseCaseTest.FakeStock();
        Booking booking = unpaidBooking();
        repository.bookings.put(booking.id(), booking);
        stock.available.put("ticket", 0);
        ExpireUnpaidBookingsUseCase useCase = useCase(repository, stock, BOOKED_AT.plusSeconds(599));

        assertThat(useCase.expireOverdueBookings()).isZero();
        assertThat(repository.bookings.get(booking.id()).status()).isEqualTo(BookingStatus.AWAITING_PAYMENT);
        assertThat(stock.available).containsEntry("ticket", 0);
    }

    @Test
    void une_reprise_du_job_ne_restitue_pas_deux_fois_le_stock() {
        RequestBookingUseCaseTest.RecordingRepository repository = new RequestBookingUseCaseTest.RecordingRepository();
        RequestBookingUseCaseTest.FakeStock stock = new RequestBookingUseCaseTest.FakeStock();
        Booking booking = unpaidBooking();
        repository.bookings.put(booking.id(), booking);
        stock.available.put("ticket", 0);
        ExpireUnpaidBookingsUseCase useCase = useCase(repository, stock, BOOKED_AT.plusSeconds(601));

        useCase.expireOverdueBookings();
        useCase.expireOverdueBookings();

        assertThat(stock.available).containsEntry("ticket", 2);
        assertThat(repository.stockReturnPending).isEmpty();
    }

    @Test
    void une_restitution_en_echec_est_reprise_au_prochain_passage() {
        RequestBookingUseCaseTest.RecordingRepository repository = new RequestBookingUseCaseTest.RecordingRepository();
        RequestBookingUseCaseTest.FakeStock stock = new RequestBookingUseCaseTest.FakeStock() {
            private boolean failOnce = true;

            @Override
            public void giveBackOnce(String bookingId, List<BookingLine> lines) {
                if (failOnce) {
                    failOnce = false;
                    throw new IllegalStateException("Redis indisponible");
                }
                super.giveBackOnce(bookingId, lines);
            }
        };
        Booking booking = unpaidBooking();
        repository.bookings.put(booking.id(), booking);
        stock.available.put("ticket", 0);
        ExpireUnpaidBookingsUseCase useCase = useCase(repository, stock, BOOKED_AT.plusSeconds(601));

        assertThatThrownBy(useCase::expireOverdueBookings)
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.bookings.get(booking.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(repository.stockReturnPending).contains(booking.id());

        useCase.expireOverdueBookings();

        assertThat(stock.available).containsEntry("ticket", 2);
        assertThat(repository.stockReturnPending).isEmpty();
    }

    private static ExpireUnpaidBookingsUseCase useCase(
            RequestBookingUseCaseTest.RecordingRepository repository,
            RequestBookingUseCaseTest.FakeStock stock,
            Instant now) {
        return new ExpireUnpaidBookingsUseCase(repository, stock, Clock.fixed(now, ZoneOffset.UTC), EXPIRATION_DELAY);
    }

    private static Booking unpaidBooking() {
        return Booking.request("bk_expiration", new UserId("alice"), "event",
                        List.of(new BookingLine("ticket", "Standard", 2, 1000)), BOOKED_AT)
                .reserve().awaitPayment("secret");
    }
}
