package fr.soft_labs.eventbyte.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingJpaRepository extends JpaRepository<BookingEntity, String> {
}
