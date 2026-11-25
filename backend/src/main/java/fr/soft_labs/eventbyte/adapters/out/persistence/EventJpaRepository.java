package fr.soft_labs.eventbyte.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventJpaRepository extends JpaRepository<EventEntity, String> {
}
