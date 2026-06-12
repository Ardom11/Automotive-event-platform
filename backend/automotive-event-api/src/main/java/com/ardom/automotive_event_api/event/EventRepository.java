package com.ardom.automotive_event_api.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAll(Pageable pageable);

    Optional<Event> findByIdAndStatus(Long id, EventStatus status);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);
}
