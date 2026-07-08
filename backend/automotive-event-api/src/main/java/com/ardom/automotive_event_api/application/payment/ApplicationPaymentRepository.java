package com.ardom.automotive_event_api.application.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationPaymentRepository extends JpaRepository<ApplicationPayment, Long> {
    boolean existsByApplicationId(Long id);
}
