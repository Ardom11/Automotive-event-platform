package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationPaymentRepository extends JpaRepository<ApplicationPayment, Long> {
    boolean existsByApplicationIdAndStatusIn(Long id, List<PaymentStatus> statuses);

    ApplicationPayment findByApplicationId(Long id);

    List<ApplicationPayment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAtBefore);
}
