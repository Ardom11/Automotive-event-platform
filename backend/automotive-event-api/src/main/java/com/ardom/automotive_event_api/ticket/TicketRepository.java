package com.ardom.automotive_event_api.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Integer countByEventId(Long eventId);

    Page<Ticket> findAllByUserId(Long userId, Pageable attr0);

    Ticket findByIdAndUserId(Long id, Long userId);
}
