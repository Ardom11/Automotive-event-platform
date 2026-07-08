package com.ardom.automotive_event_api.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Integer countByEventId(Long eventId);
}
