package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface BookingJpaRepository extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByStatusInAndBookedAtLessThanEqualOrderByBookedAtAsc(
            Collection<BookingStatus> statuses, Instant cutoff);

    List<BookingEntity> findByStatusAndStockReturnedFalseOrderByUpdatedAtAsc(BookingStatus status);

    @Modifying
    @Query("""
            update BookingEntity b
            set b.status = :expired, b.stockReturned = false, b.updatedAt = :updatedAt
            where b.id = :id and b.status in :payableStatuses and b.bookedAt <= :cutoff
            """)
    int expireIfUnpaidAndBookedBefore(@Param("id") String id,
                                      @Param("payableStatuses") Collection<BookingStatus> payableStatuses,
                                      @Param("expired") BookingStatus expired,
                                      @Param("cutoff") Instant cutoff,
                                      @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            update BookingEntity b set b.stockReturned = true
            where b.id = :id and b.status = :expired and b.stockReturned = false
            """)
    int markStockReturned(@Param("id") String id, @Param("expired") BookingStatus expired);

    @Modifying
    @Query("""
            update BookingEntity b
            set b.status = :status, b.clientSecret = :clientSecret, b.updatedAt = :updatedAt
            where b.id = :id and b.status = :expectedStatus
            """)
    int updateIfStatus(@Param("id") String id,
                       @Param("expectedStatus") BookingStatus expectedStatus,
                       @Param("status") BookingStatus status,
                       @Param("clientSecret") String clientSecret,
                       @Param("updatedAt") Instant updatedAt);
}
