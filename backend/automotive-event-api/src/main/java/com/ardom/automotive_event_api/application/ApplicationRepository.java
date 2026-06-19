package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Page<Application> findByUserId(Long id, Pageable pageable);
}
