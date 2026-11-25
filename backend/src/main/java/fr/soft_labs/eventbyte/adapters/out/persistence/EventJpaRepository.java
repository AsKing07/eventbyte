package fr.soft_labs.eventbyte.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface EventJpaRepository extends JpaRepository<EventEntity, String> {

    /** Événements non supprimés avec leurs tickets, en une seule requête*/
    @Query("select distinct e from EventEntity e left join fetch e.tickets where e.id in :ids and e.deletedAt is null")
    List<EventEntity> findVisibleWithTickets(@Param("ids") Collection<String> ids);
}
