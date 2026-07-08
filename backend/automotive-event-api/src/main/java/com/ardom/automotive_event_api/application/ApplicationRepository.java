package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Page<Application> findByUserId(Long id, Pageable pageable);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<Application> findAllByStatusAndEvent_DateStartBefore(
            ApplicationStatus status,
            LocalDateTime threshold
    );

    Optional<Application> findByIdAndStatusAndUser(Long id, ApplicationStatus status, User user);
}
